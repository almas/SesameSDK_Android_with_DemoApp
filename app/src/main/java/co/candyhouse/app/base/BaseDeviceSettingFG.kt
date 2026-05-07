package co.candyhouse.app.base

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.View
import androidx.core.content.FileProvider
import android.widget.RelativeLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewbinding.ViewBinding
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.R
import co.candyhouse.app.ext.CHDeviceWrapperManager
import co.candyhouse.app.ext.DfuCenter
import co.candyhouse.app.ext.userKey
import co.candyhouse.app.ext.webview.data.WebViewConfig
import co.candyhouse.app.ext.webview.util.EmbeddedWebViewContent
import co.candyhouse.app.tabs.devices.model.bindLifecycle
import co.candyhouse.app.tabs.devices.ssm2.clearNFC
import co.candyhouse.app.tabs.devices.ssm2.getFirmwareName
import co.candyhouse.app.tabs.devices.ssm2.getFirmwarePath
import co.candyhouse.app.tabs.devices.ssm2.getIsWidget
import co.candyhouse.app.tabs.devices.ssm2.getLevel
import co.candyhouse.app.tabs.devices.ssm2.getNFC
import co.candyhouse.app.tabs.devices.ssm2.getNickname
import co.candyhouse.app.tabs.devices.ssm2.modelName
import co.candyhouse.app.tabs.devices.ssm2.setIsNOHand
import co.candyhouse.app.tabs.devices.ssm2.setIsWidget
import co.candyhouse.app.tabs.devices.ssm2.setNFC
import co.candyhouse.app.tabs.devices.ssm2.setting.DfuService
import co.candyhouse.sesame.server.LocalServerConfig
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.open.CHBleStatusDelegate
import co.candyhouse.sesame.open.CHScanStatus
import co.candyhouse.sesame.open.devices.base.CHDeviceLoginStatus
import co.candyhouse.sesame.open.devices.base.CHDeviceStatus
import co.candyhouse.sesame.open.devices.base.CHDeviceStatusDelegate
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.open.devices.base.CHDevices.Companion.UNSET_BLE_TX_POWER_VALUE
import co.candyhouse.sesame.open.devices.base.CHProductModel
import co.candyhouse.sesame.utils.L
import co.utils.base64Encode
import co.utils.hexStringToByteArray
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.alertview.fragments.toastMSG
import co.utils.alertview.objects.AlertAction
import co.utils.base64Encode
import co.utils.hexStringToByteArray
import co.utils.safeNavigate
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import kotlinx.coroutines.launch

abstract class BaseDeviceSettingFG<T : ViewBinding> : BaseDeviceFG<T>(), NfcSetting,
    BleStatusUpdate, DeviceStatusChange {

    private var isToNotification = false
    private var isViewDestroyed = false
    private val refreshCounter = mutableIntStateOf(0)
    private var pageDeviceKey: String? = null

    override fun onResume() {
        super.onResume()
        onChange()

        CHBleManager.statusDelegate = object : CHBleStatusDelegate {
            override fun didScanChange(ss: CHScanStatus) {
                onChange()
            }
        }

        mDeviceModel.ssmosLockDelegates[mDeviceModel.ssmLockLiveData.value!!] =
            object : CHDeviceStatusDelegate {
                @SuppressLint("SetTextI18n")
                override fun onBleDeviceStatusChanged(
                    device: CHDevices,
                    status: CHDeviceStatus,
                    shadowStatus: CHDeviceStatus?
                ) {
                    L.d("[say]", "[BaseDeviceSettingFG.kt][onBleDeviceStatusChanged]")
                    onChange()
                    onUIDeviceStatus(status)
                    checkVersionTag(status, device)
                }

                override fun onBleTxPowerReceive(device: CHDevices, txPower: Byte) {
                    L.d("BLE tx power", "onBleTxPowerReceive...$txPower")
                    showBleTxPowerUI(device, txPower)
                }

                @SuppressLint("SetTextI18n")
                override fun onMechStatus(device: CHDevices) {
                    setBatteryResult(device)
                }
            }.bindLifecycle(viewLifecycleOwner)

        checkTvSysNotifyWidget(isOnResume = true)
    }

    private fun showBleTxPowerUI(targetDevice: CHDevices, txByte: Byte) {
        val ctx = context ?: return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        L.d("BLE tx power", "bleTxPower: $txByte")

        val bleTxPowerZone = view?.findViewById<View>(R.id.ble_tx_power_zone)
        val bleTxPowerSeekbar = view?.findViewById<IndicatorSeekBar>(R.id.ble_tx_power_seekbar)
        activity?.runOnUiThread {
            if (txByte.toInt() == UNSET_BLE_TX_POWER_VALUE) {
                bleTxPowerZone?.visibility = View.GONE
                return@runOnUiThread
            } else {
                if (!isAdded) return@runOnUiThread

                bleTxPowerSeekbar?.setIndicatorTextFormat("\${PROGRESS} " + getString(R.string.dBm))
                bleTxPowerSeekbar?.setProgress(txByte.toFloat())

                var lastProgress = txByte.toInt()
                bleTxPowerSeekbar?.onSeekChangeListener = object : OnSeekChangeListener {

                    override fun onStartTrackingTouch(seekBar: IndicatorSeekBar) {}

                    override fun onSeeking(seekParams: SeekParams) {
                        val currentProgress = seekParams.progress
                        if (currentProgress != lastProgress) {
                            lastProgress = currentProgress
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createOneShot(3, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(3)
                            }
                        }
                    }

                    override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {
                        val txPwr = seekBar.progress.toByte()
                        L.d("BLE tx power", "设置 BLE tx power 为： $txPwr ")
                        targetDevice.setBleTxPower(txPwr) {}
                    }
                }

                bleTxPowerZone?.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isViewDestroyed = true
    }

    @SuppressLint("StringFormatMatches")
    fun usePressText() {
        val model = mDeviceModel.ssmLockLiveData.value
        model?.apply {
            view?.findViewById<TextView>(R.id.drop_hint_txt)?.text =
                resources.getString(R.string.drop_hint_press, model.productModel.modelName())
        }
    }

    fun setBatteryResult(device: CHDevices) {
        val batteryText = view?.findViewById<TextView>(R.id.battery)
        val batteryLevel = device.batteryPercentage ?: device.userKey?.stateInfo?.batteryPercentage
        batteryText?.post {
            batteryText.text = batteryLevel?.let { "$it%" } ?: ""
        }
    }

    open fun checkVersionTag(status: CHDeviceStatus, device: CHDevices) {
        if (status.value == CHDeviceLoginStatus.logined) {
            device.getVersionTag {
                it.onSuccess { va ->
                    if (isAdded && !isDetached) {
                        versionSet(device, va.data)
                    }
                }
            }
        } else if (device.productModel == CHProductModel.SSMOpenSensor || device.productModel == CHProductModel.RemoteNano) {
            if (isAdded && !isDetached) {
                device.userKey?.stateInfo?.currentFwVer?.let {
                    versionSet(device, it)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun versionSet(targetDevice: CHDevices, str: String) {
        if (targetDevice.productModel != CHProductModel.Hub3 && targetDevice.productModel != CHProductModel.Hub3_LTE) {
            view?.findViewById<View>(R.id.device_version_txt)?.post {
                val ctx = context ?: return@post
                val zipName: String? = targetDevice.getFirmwareName(ctx)
                zipName?.apply {
                    val tailTag = str.split("-").last()
                    val tempFlag = zipName.contains(tailTag)
                    view?.findViewById<TextView>(R.id.device_version_txt)?.text = str + (if (tempFlag) getString(R.string.latest) else "")
                    view?.findViewById<View>(R.id.alert_logo)?.visibility = if (tempFlag) View.GONE else View.VISIBLE
                    // 如果是最新版，则更新设备列表对应item
                    if (tempFlag) {
                        CHDeviceWrapperManager.updateCurrentFwVer(targetDevice.deviceId?.toString(), str)
                        mDeviceViewModel.updateNeeRefresh(targetDevice)
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isViewDestroyed = false

        mDeviceModel.ssmLockLiveData.observe(viewLifecycleOwner) { ss2 ->
            getView()?.findViewById<TextView>(R.id.device_model)?.text =
                ss2.productModel.deviceModelName()
            getView()?.findViewById<TextView>(R.id.device_uuid_txt)?.text =
                ss2.deviceId.toString().uppercase()
            getView()?.findViewById<TextView>(R.id.histag_txt)?.text =
                ss2.getHistoryTag()?.let { String(it) }

            checkVersionTag(ss2.deviceStatus, ss2)
        }

        val targetDevice = mDeviceModel.ssmLockLiveData.value!!
        pageDeviceKey = targetDevice.deviceId.toString()
        handleUI(targetDevice)
        setBatteryResult(targetDevice)
        showBleTxPowerUI(targetDevice, targetDevice.bleTxPower)
        setupListeners(targetDevice)
    }

    private fun setupListeners(targetDevice: CHDevices) {
        view?.findViewById<View>(R.id.drop_zone)?.setOnClickListener {
            if (LocalServerConfig.isOfflineMode()) {
                AlertView("", "", AlertStyle.IOS).apply {
                    addAction(
                        AlertAction(
                            getString(
                                R.string.trash_device_key,
                                targetDevice.getNickname()
                            ), AlertActionStyle.NEGATIVE
                        ) {
                            mDeviceModel.dropDeviceOffline()
                            if (isAdded && !isViewDestroyed) {
                                viewLifecycleOwner.lifecycleScope.launch {
                                    findNavController().popBackStack(R.id.deviceListPG, false)
                                }
                            }
                        })
                    show(activity as AppCompatActivity)
                }
                return@setOnClickListener
            }
            AlertView("", "", AlertStyle.IOS).apply {
                addAction(
                    AlertAction(
                        getString(
                            R.string.trash_device_key,
                            targetDevice.getNickname()
                        ), AlertActionStyle.NEGATIVE
                    ) {
                        mDeviceModel.dropDevice {
                            it.onSuccess {
                                if (isAdded && !isViewDestroyed) {
                                    viewLifecycleOwner.lifecycleScope.launch {
                                        findNavController().popBackStack(R.id.deviceListPG, false)
                                    }
                                }
                            }
                            it.onFailure {
                                toastMSG(it.localizedMessage)
                                L.d("hcia", "無法刪除鑰匙倉庫:")
                            }
                        }
                    })
                show(activity as AppCompatActivity)
            }
        }
        view?.findViewById<View>(R.id.reset_zone)?.setOnClickListener {
            if (LocalServerConfig.isOfflineMode()) {
                AlertView("", "", AlertStyle.IOS).apply {
                    addAction(
                        AlertAction(
                            getString(R.string.ssm_delete),
                            AlertActionStyle.NEGATIVE
                        ) {
                            mDeviceModel.resetDeviceOffline()
                            if (isAdded) {
                                findNavController().popBackStack(R.id.deviceListPG, false)
                            }
                        })
                    show(activity as AppCompatActivity)
                }
                return@setOnClickListener
            }
            AlertView("", "", AlertStyle.IOS).apply {
                addAction(
                    AlertAction(
                        getString(R.string.ssm_delete),
                        AlertActionStyle.NEGATIVE
                    ) {
                        mDeviceModel.resetDevice {
                            it.onSuccess {
                                if (isAdded) {
                                    findNavController().popBackStack(R.id.deviceListPG, false)
                                }
                            }
                            it.onFailure {
                                toastMSG(it.localizedMessage)
                                L.d("hcia", "無法刪除鑰匙倉庫:")
                            }
                        }
                    })
                show(activity as AppCompatActivity)
            }
        }
        view?.findViewById<View>(R.id.nfc_zone)?.setOnClickListener {
            if (targetDevice.getNFC()?.isEmpty() == false) {
                AlertView("", "", AlertStyle.IOS).apply {
                    addAction(
                        AlertAction(
                            getString(R.string.nfc_reset),
                            AlertActionStyle.NEGATIVE
                        ) {
                            targetDevice.clearNFC()
                            getView()?.findViewById<TextView>(R.id.nfc_id_txt)?.text =
                                getString(R.string.nfc_hint)
                        })
                    show(activity as AppCompatActivity)
                }
            }
        }
        view?.findViewById<View>(R.id.dfu_zone)?.setOnClickListener {
            if (LocalServerConfig.isOfflineMode()) {
                toastMSG("Update is disabled in offline mode.")
                return@setOnClickListener
            }
            val unlogined =
                mDeviceModel.ssmLockLiveData.value?.deviceStatus?.value == CHDeviceLoginStatus.unlogined

            if (unlogined) {
                when (targetDevice.productModel) {
                    CHProductModel.SSMOpenSensor, CHProductModel.RemoteNano -> return@setOnClickListener
                    else -> {
                        toastMSG(getString(R.string.toastBleNotReadyForDFU))
                        return@setOnClickListener
                    }
                }
            }
            AlertView(getString(R.string.ssm_update), "", AlertStyle.IOS).apply {
                addAction(
                    AlertAction("OK", AlertActionStyle.NEGATIVE) {
                        targetDevice.updateFirmware { res ->
                            res.onSuccess {
                                val dfuAddress = it.data.address
                                L.d("DFU", "updateFirmware:$dfuAddress")

                                val firmwarePath = targetDevice.getFirmwarePath(requireContext()) ?: return@onSuccess
                                val pageDeviceKey = getPageDeviceKey() ?: return@onSuccess

                                when (
                                    DfuCenter.startDfu(
                                        context = requireContext(),
                                        deviceKey = pageDeviceKey,
                                        deviceAddress = dfuAddress,
                                        firmwarePath = firmwarePath,
                                        delegate = this@BaseDeviceSettingFG,
                                        serviceClass = DfuService::class.java
                                    )
                                ) {
                                    is DfuCenter.StartResult.Started -> {}

                                    is DfuCenter.StartResult.AlreadyRunningSameDevice -> {}

                                    is DfuCenter.StartResult.Busy -> {
                                        toastMSG(getString(R.string.dfu_busy))
                                    }
                                }
                            }
                        }
                    }
                )
                show(activity as AppCompatActivity)
            }
        }

        view?.findViewById<View>(R.id.battery_zone)?.setOnClickListener {
            val config = WebViewConfig(
                scene = "battery-trend",
                params = mapOf(
                    "deviceUUID" to targetDevice.deviceId.toString().uppercase(),
                    "deviceName" to targetDevice.productModel.deviceModelName()
                )
            )
            safeNavigate(R.id.action_DeviceMember_to_webViewFragment, config.toBundle())
        }
        
        view?.findViewById<View>(R.id.share_zone)?.setOnClickListener {
            showShareMenu(targetDevice)
        }
    }
    
    private fun showShareMenu(device: CHDevices) {
        // Get device info and key
        val uuid = device.deviceId?.toString()?.uppercase() ?: "N/A"
        val level = device.getLevel()
        val key = try { device.getKey() } catch (e: Exception) { null }
        
        if (key == null) {
            toastMSG("Device key not found")
            return
        }
        
        val productModel = device.productModel
        val productType = productModel.productType()
        
        // Match the logic in ScanQRcodeFG.kt for "valid" models
        val isValidModel = productModel == CHProductModel.SS5 || productModel == CHProductModel.BiKeLock2 || 
                           productModel == CHProductModel.BiKeLock3 || productModel == CHProductModel.SSMTouchPro || 
                           productModel == CHProductModel.SSMTouch2Pro || productModel == CHProductModel.SSMTouch || 
                           productModel == CHProductModel.SSMTouch2 || productModel == CHProductModel.SS5PRO || 
                           productModel == CHProductModel.BLEConnector || productModel == CHProductModel.Remote || 
                           productModel == CHProductModel.RemoteNano || productModel == CHProductModel.SS5US || 
                           productModel == CHProductModel.SesameBot2 || productModel == CHProductModel.SesameBot3 ||
                           productModel == CHProductModel.SSMFacePro || productModel == CHProductModel.SSMFace2Pro || 
                           productModel == CHProductModel.SSMFaceProAI || productModel == CHProductModel.SSMFace2ProAI || 
                           productModel == CHProductModel.SSMFaceAI || productModel == CHProductModel.SSMFace2AI || 
                           productModel == CHProductModel.SS6 || productModel == CHProductModel.SS6Pro ||
                           productModel == CHProductModel.SS6ProSLiDingDoor || productModel == CHProductModel.Hub3 || 
                           productModel == CHProductModel.SSMFace || productModel == CHProductModel.SSMFace2 || 
                           productModel == CHProductModel.SSMOpenSensor2 || productModel == CHProductModel.SSMOpenSensor ||
                           productModel == CHProductModel.SSM_MIWA || productModel == CHProductModel.Hub3_LTE

        val secretKeyBytes = key.secretKey.hexStringToByteArray()
        val publicKeyBytes = key.sesame2PublicKey.hexStringToByteArray()
        val keyIndexBytes = key.keyIndex.hexStringToByteArray()
        val uuidBytes = key.deviceUUID.replace("-", "").hexStringToByteArray()

        // Pack the data into the 'sk' parameter as expected by the scanner
        val skBytes = if (isValidModel) {
            // Format for valid models: [1 byte Type][16 bytes Secret][4 bytes Pub][2 bytes Index][16 bytes UUID] = 39 bytes
            val result = ByteArray(39)
            result[0] = productType.toByte()
            System.arraycopy(secretKeyBytes, 0, result, 1, 16.coerceAtMost(secretKeyBytes.size))
            System.arraycopy(publicKeyBytes, 0, result, 17, 4.coerceAtMost(publicKeyBytes.size))
            System.arraycopy(keyIndexBytes, 0, result, 21, 2.coerceAtMost(keyIndexBytes.size))
            System.arraycopy(uuidBytes, 0, result, 23, 16.coerceAtMost(uuidBytes.size))
            result
        } else {
            // Format for other models: [1 byte Type][16 bytes Secret][64 bytes Pub][2 bytes Index][16 bytes UUID] = 99 bytes
            val result = ByteArray(99)
            result[0] = productType.toByte()
            System.arraycopy(secretKeyBytes, 0, result, 1, 16.coerceAtMost(secretKeyBytes.size))
            System.arraycopy(publicKeyBytes, 0, result, 17, 64.coerceAtMost(publicKeyBytes.size))
            System.arraycopy(keyIndexBytes, 0, result, 81, 2.coerceAtMost(keyIndexBytes.size))
            System.arraycopy(uuidBytes, 0, result, 83, 16.coerceAtMost(uuidBytes.size))
            result
        }

        val encodedSecret = skBytes.base64Encode()
        
        // URL encode the nickname for the QR code (handle special characters)
        val nickname = device.getNickname()?.let { 
            java.net.URLEncoder.encode(it, "UTF-8")
                .replace("+", "%20") // Replace + with %20 for cleaner URLs
        } ?: ""
        
        // Create QR code content with correct format: ssm://UI?t=sk&sk=BASE64_PACKED_DATA&l=LEVEL&n=NICKNAME
        val qrContent = "ssm://UI?t=sk&sk=$encodedSecret&l=$level&n=$nickname"
        
        L.d("showShareMenu", "QR Content: $qrContent")
        L.d("showShareMenu", "Model: ${productModel.deviceModelName()} ($productType)")
        L.d("showShareMenu", "Level: $level")
        
        // For text sharing, keep it simple
        val shareContent = """
            Device UUID: $uuid
            Secret Key: ${key.secretKey}
            
            This data allows another app/device to connect to this Sesame device.
        """.trimIndent()
        
        // Show share dialog
        val items = listOf(
            Pair(getString(R.string.share_qr_code), "qr"),
            Pair(getString(R.string.share_text), "text")
        )
        
        val itemsArray = items.toTypedArray()
        
        activity?.let { act ->
            androidx.appcompat.app.AlertDialog.Builder(act)
                .setTitle(R.string.share_device)
                .setItems(itemsArray.map { it.first }.toTypedArray()) { _, which ->
                    val selectedType = items[which].second
                    when (selectedType) {
                        "qr" -> shareQRCode(qrContent)
                        "text" -> shareText(shareContent)
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }
    
    private fun shareQRCode(content: String) {
        try {
            // Use the existing QRCodeEncoder from the SDK
            val qrBitmap = cn.bingoogolapple.qrcode.zxing.QRCodeEncoder.syncEncodeQRCode(
                content,
                500
            )
            
            // Save QR code to temp file and share
            val uri = saveBitmapToUri(qrBitmap)
            if (uri == null) {
                toastMSG("Failed to save QR code")
                return
            }
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            activity?.let { act ->
                act.startActivity(Intent.createChooser(intent, act.getString(R.string.share_device)))
            }
        } catch (e: Exception) {
            L.e("ShareQR", "Error sharing QR code: ${e.message}")
            toastMSG("Failed to generate QR code")
        }
    }
    
    private fun shareText(content: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        activity?.let { act ->
            act.startActivity(Intent.createChooser(intent, act.getString(R.string.share_device)))
        }
    }
    
    private fun saveBitmapToUri(bitmap: Bitmap): Uri? {
        val context = requireContext()
        val fileName = "sesame_device_qr_${System.currentTimeMillis()}.png"
        
        try {
            // Save to external files directory to use with FileProvider
            val imagesFolder = java.io.File(context.getExternalFilesDir(null), "images")
            if (!imagesFolder.exists()) {
                imagesFolder.mkdirs()
            }
            val file = java.io.File(imagesFolder, fileName)
            java.io.FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            
            // Return Uri from FileProvider
            return FileProvider.getUriForFile(context, context.packageName, file)
        } catch (e: Exception) {
            L.e("SaveBitmap", "Error saving QR code: ${e.message}")
            return null
        }
    }

    override fun providePageDeviceKey(): String? {
        return pageDeviceKey
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun handleUI(targetDevice: CHDevices) {
        val level = targetDevice.getLevel()
        if (level == 2) {
            view?.findViewById<View>(R.id.chenge_angle_zone)?.visibility = View.GONE
            view?.findViewById<View>(R.id.auto_lock_zone)?.visibility = View.GONE
            view?.findViewById<View>(R.id.opsensor_zone)?.visibility = View.GONE
            view?.findViewById<View>(R.id.ble_tx_power_zone)?.visibility = View.GONE
        }
        targetDevice.getNFC()
            ?.apply { view?.findViewById<TextView>(R.id.nfc_id_txt)?.text = this }
        view?.findViewById<View>(R.id.reset_zone)?.visibility =
            if (BuildConfig.DEBUG) View.VISIBLE else View.GONE

        view?.findViewById<Switch>(R.id.widget_switch)?.apply {
            isChecked = targetDevice.getIsWidget()
            updataTargetDevice(isChecked, mDeviceModel.ssmLockLiveData.value)
            checkTvSysNotifyWidget(isCheck = isChecked)
            setOnCheckedChangeListener { view, isChecked ->
                targetDevice.setIsWidget(isChecked)
                updataTargetDevice(isChecked, mDeviceModel.ssmLockLiveData.value)
                checkTvSysNotifyWidget(isCheck = isChecked)
            }
        }

        view?.findViewById<RelativeLayout>(R.id.widget_rl)?.apply {
            setOnClickListener(null)
            setOnClickListener {
                openSettingNotify()
            }
        }

        view?.findViewById<TextView>(R.id.drop_hint_txt)?.text =
            getString(R.string.drop_hint, targetDevice.productModel.modelName())

        view?.findViewById<ComposeView>(R.id.device_setting_web_view)?.apply {
            disposeComposition()

            setContent {
                EmbeddedWebViewContent(
                    config = WebViewConfig(
                        scene = "device-setting",
                        params = mapOf(
                            "deviceUUID" to targetDevice.deviceId.toString().uppercase(),
                            "keyLevel" to targetDevice.getLevel().toString()
                        )
                    ),
                    height = 80.dp,
                    refreshTrigger = refreshCounter.intValue,
                    onSchemeIntercept = { uri, params ->
                        when (uri.path) {
                            "/webview/open" -> {
                                params["url"]?.let { targetUrl ->
                                    params["notifyName"]?.let { notifyName ->
                                        L.d("EmbeddedWebView", "EmbeddedWebView-notifyName=$notifyName")
                                        when (notifyName) {
                                            "DeviceMemberChanged" -> {
                                                L.d("EmbeddedWebView", "EmbeddedWebView-targetUrl=$targetUrl")
                                                safeNavigate(R.id.action_DeviceMember_to_webViewFragment, Bundle().apply {
                                                    putString("scene", "device-user")
                                                    putString("url", targetUrl)
                                                })
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }

        view?.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)?.setOnRefreshListener {
            refreshTop()
        }
    }

    private fun refreshTop() {
        view?.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)?.post {
            view?.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)?.isRefreshing = true
        }

        // 触发WebView刷新
        refreshCounter.intValue++

        view?.postDelayed({
            view?.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)?.isRefreshing = false
        }, 1500)
    }

    private fun updataTargetDevice(isChecked: Boolean, targetDevice: CHDevices?) {
        targetDevice?.apply {
            view?.findViewById<View>(R.id.no_hand_zone)?.visibility =
                if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                targetDevice.setIsNOHand(false)
                if (!isAdded) return
                view?.findViewById<TextView>(R.id.auto_open_txt)?.text = getString(R.string.Off)
            }

            mDeviceModel.updateWidgets(this.deviceId.toString())
        }
    }

    override fun onNfcId(id: String) {
        requireActivity().runOnUiThread {
            mDeviceModel.ssmLockLiveData.value?.apply {
                this.setNFC(id)

                view?.apply {
                    findViewById<TextView>(R.id.nfc_id_txt)?.apply {
                        this.text = id
                    }
                }
            }
        }
    }

    override fun onChange() {
        L.d(
            "harry",
            "[onChange][status: " + mDeviceModel.ssmLockLiveData.value?.deviceStatus.toString() + "] [CHBleManager:  " + CHBleManager.getConnectRSize()
                .toString() + "]"
        )
        if (!isAdded) return
        when {
            CHBleManager.mScanning == CHScanStatus.BleClose -> {
                view?.findViewById<View>(R.id.err_zone)?.visibility = View.VISIBLE
                view?.findViewById<TextView>(R.id.err_title)?.text = getString(R.string.noble)
            }

            mDeviceModel.ssmLockLiveData.value!!.deviceStatus == CHDeviceStatus.NoBleSignal -> {
                view?.findViewById<View>(R.id.err_zone)?.visibility = View.VISIBLE
                if (CHBleManager.getConnectRSize() >= 7) {
                    view?.findViewById<TextView>(R.id.err_title)?.text =
                        getString(R.string.BleTooManyConnections)
                } else {
                    view?.findViewById<TextView>(R.id.err_title)?.text =
                        getString(R.string.NoBleSignal)
                }
            }

            mDeviceModel.ssmLockLiveData.value?.deviceStatus?.value == CHDeviceLoginStatus.unlogined -> {
                view?.findViewById<View>(R.id.err_zone)?.visibility = View.VISIBLE
                view?.findViewById<TextView>(R.id.err_title)?.text =
                    mDeviceModel.ssmLockLiveData.value!!.deviceStatus.toString()
            }

            else -> {
                view?.findViewById<View>(R.id.err_zone)?.visibility = View.GONE
            }
        }
    }

    private fun isNotifyEnable(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val notificationManager = getSystemService(
                requireContext(),
                NotificationManager::class.java
            ) as NotificationManager
            return notificationManager.areNotificationsEnabled()
        } else {
            return true
        }
    }

    private fun openSettingNotify() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity?.apply {
                val intent = Intent()
                isToNotification = true
                intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, this.packageName)
                startActivity(intent)
            }
        }
    }

    private fun checkTvSysNotifyWidget(isCheck: Boolean = false, isOnResume: Boolean = false) {
        view?.findViewById<TextView>(R.id.tvSysNotifyWidget)?.apply {
            text =
                if (isNotifyEnable()) getString(R.string.android_notifica_permis_on) else getString(
                    R.string.android_notifica_permis_off
                )
            if (!isOnResume) {
                isEnabled = isCheck
            } else {
                if (isToNotification) {
                    isToNotification = false
                    updataTargetDevice(true, mDeviceModel.ssmLockLiveData.value)
                }
            }
            isSelected = isEnabled && !isNotifyEnable()
        }
    }
}

interface NfcSetting {
    fun onNfcId(id: String)
}

interface BleStatusUpdate {
    fun onChange()
}

interface DeviceStatusChange {
    fun onUIDeviceStatus(status: CHDeviceStatus) {}
}