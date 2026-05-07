package co.candyhouse.sesame.server

import android.content.Context
import android.util.Base64
import co.candyhouse.sesame.ble.CHDeviceUtil
import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.db.CHDB
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.open.devices.base.CHSesameLock
import co.candyhouse.sesame.server.dto.AuthenticationDataWrapper
import co.candyhouse.sesame.server.dto.BotScriptRequest
import co.candyhouse.sesame.server.dto.CHBatteryDataReq
import co.candyhouse.sesame.server.dto.CHDeviceInfo
import co.candyhouse.sesame.server.dto.CHFcmTokenUpload
import co.candyhouse.sesame.server.dto.CHRemoveSignKeyRequest
import co.candyhouse.sesame.server.dto.CHSS2RegisterReq
import co.candyhouse.sesame.server.dto.CHSS2RegisterRes
import co.candyhouse.sesame.server.dto.CHSS2WebCMDReq
import co.candyhouse.sesame.server.dto.CHSS5HisUploadRequest
import co.candyhouse.sesame.server.dto.CHSSMHisUploadRequest
import co.candyhouse.sesame.server.dto.CHUserKey
import co.candyhouse.sesame.server.dto.ScenePayload
import co.candyhouse.sesame.server.dto.SubscriptionRequest
import co.candyhouse.sesame.utils.ApiClientConfigBuilder
import co.candyhouse.sesame.utils.AppIdentifyIdUtil
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult
import co.candyhouse.sesame.utils.CHResultState
import co.candyhouse.sesame.utils.TokenManager
import co.candyhouse.sesame.utils.aescmac.AesCmac
import co.candyhouse.sesame.utils.base64Encode
import co.candyhouse.sesame.utils.hexStringToByteArray
import co.candyhouse.sesame.utils.toHexString
import co.candyhouse.sesame.utils.toUInt24ByteArray
import co.candyhouse.sesame.utils.L
import com.amazonaws.auth.AWSCredentialsProvider
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * API Gateway 业务 - Supports both AWS and Local Server modes
 *
 * @author frey on 2026/1/12
 */
object CHAPIClientBiz : ICHAPIClientBiz {

    private const val TAG = "CHAPIClientBiz"
    private lateinit var appContext: Context
    private var cHApiClient: CHAPIClient? = null
    
    private var onlineClient: ICHAPIClientBiz? = null
    private val offlineClient = CHAPIClientBizOffline()
    
    @Volatile
    private var currentClient: ICHAPIClientBiz = offlineClient

    @Volatile
    private var initialized = false

    @JvmStatic
    @Synchronized
    fun initialize(
        context: Context,
        credentialsProvider: AWSCredentialsProvider,
        region: String,
        apiKey: String? = null
    ) {
        appContext = context.applicationContext
        LocalServerConfig.initialize(context)
        LocalServerConfig.setEnabled(false)
        LocalServerConfig.setOfflineMode(false)

        val factory = ApiClientConfigBuilder.buildApiClientFactory(
            credentialsProvider = credentialsProvider,
            apiKey = apiKey,
            region = region
        )

        val client = factory.build(CHAPIClient::class.java)
        cHApiClient = client
        onlineClient = CHAPIClientBizOnline(appContext, client)
        
        if (!LocalServerConfig.isOfflineMode()) {
            currentClient = onlineClient!!
        }
        
        initialized = true
        L.d(TAG, "CHAPIClientBiz initialized. Online Mode: ${!LocalServerConfig.isOfflineMode()}")
    }

    @JvmStatic
    @Synchronized
    fun initializeLocalServer(context: Context, serverEndpoint: String? = null) {
        appContext = context.applicationContext
        LocalServerConfig.initialize(context)
        if (serverEndpoint != null) {
            LocalServerConfig.setServerEndpoint(serverEndpoint)
        }
        LocalServerConfig.setEnabled(true)
        LocalServerConfig.setOfflineMode(false)
        
        // In local server mode, we still use the "Online" client logic but it hits a different endpoint
        // This assumes ApiClientConfigBuilder handles the endpoint switch or cHApiClient is rebuilt.
        // For now, let's just mark it initialized.
        
        initialized = true
        L.d(TAG, "CHAPIClientBiz initialized for Local Server. Endpoint: ${LocalServerConfig.getServerEndpoint()}")
    }

    @JvmStatic
    @Synchronized
    fun initializeOfflineMode(context: Context) {
        appContext = context.applicationContext
        LocalServerConfig.initialize(context)
        LocalServerConfig.setEnabled(false)
        LocalServerConfig.setOfflineMode(true)
        currentClient = offlineClient
        initialized = true
        L.d(TAG, "CHAPIClientBiz initialized for COMPLETE OFFLINE MODE.")
    }

    /**
     * Switch between Online and Offline modes at runtime
     */
    @JvmStatic
    fun switchOffline(isOffline: Boolean) {
        LocalServerConfig.setOfflineMode(isOffline)
        if (isOffline) {
            currentClient = offlineClient
            L.d(TAG, "Switched to OFFLINE mode")
        } else {
            onlineClient?.let {
                currentClient = it
                L.d(TAG, "Switched to ONLINE mode")
            } ?: run {
                L.e(TAG, "Cannot switch to online mode: Online client not initialized")
            }
        }
    }

    private fun requireInit() {
        if (currentClient is CHAPIClientBizOffline) return
        check(initialized) { "CHAPIClient is not initialized. Call CHAPIClient.initialize(...) first." }
    }

    override fun upLoadKeys(keys: List<CHUserKey>, onResponse: CHResult<Array<CHUserKey>>) =
        currentClient.upLoadKeys(keys, onResponse)

    override fun putKey(key: CHUserKey, onResponse: CHResult<Any>) =
        currentClient.putKey(key, onResponse)

    override fun getDevicesList(onResponse: CHResult<Array<CHUserKey>>) =
        currentClient.getDevicesList(onResponse)

    override fun removeKey(keyId: String, onResponse: CHResult<Any>) =
        currentClient.removeKey(keyId, onResponse)

    override fun addFriend(friendID: String, onResponse: CHResult<Any>) =
        currentClient.addFriend(friendID, onResponse)

    override fun uploadUserDeviceToken(deviceToken: String, onResponse: CHResult<Any>) =
        currentClient.uploadUserDeviceToken(deviceToken, onResponse)

    override fun getWebUrlByScene(scene: String, extInfo: Map<String, String>?, onResponse: CHResult<String>) =
        currentClient.getWebUrlByScene(scene, extInfo, onResponse)

    override fun cancelNotification(device: CHSesameLock, fcmToken: String, onResponse: CHResult<Any>) =
        currentClient.cancelNotification(device, fcmToken, onResponse)

    override fun signGuestKey(key: CHRemoveSignKeyRequest, onResponse: CHResult<String>) =
        currentClient.signGuestKey(key, onResponse)

    override fun getHub3StatusFromIot(deviceUUID: String, onResponse: CHResult<Any>) =
        currentClient.getHub3StatusFromIot(deviceUUID, onResponse)

    override fun updateDeviceFirmwareVersion(deviceUUID: String, versionTag: String, onResponse: CHResult<Any>) =
        currentClient.updateDeviceFirmwareVersion(deviceUUID, versionTag, onResponse)

    override fun postSS2History(deviceID: String, hisHex: String, onResponse: CHResult<Any>) =
        currentClient.postSS2History(deviceID, hisHex, onResponse)

    override fun postOS3History(deviceID: String, hisHex: String, onResponse: CHResult<Any>) =
        currentClient.postOS3History(deviceID, hisHex, onResponse)

    override fun cmdSesame(cmd: SesameItemCode, ss2: CHDevices, historytag: ByteArray, onResponse: CHResult<CHEmpty>) =
        currentClient.cmdSesame(cmd, ss2, historytag, onResponse)

    override fun postCredentialListToServer(credentialListRequest: AuthenticationDataWrapper, onResponse: CHResult<Any>) =
        currentClient.postCredentialListToServer(credentialListRequest, onResponse)

    override fun updateAuthenticationName(authData: Any, onResponse: CHResult<Any>) =
        currentClient.updateAuthenticationName(authData, onResponse)

    override fun deleteCredentialInfo(request: AuthenticationDataWrapper, onResponse: CHResult<Any>) =
        currentClient.deleteCredentialInfo(request, onResponse)

    override fun subscribeToTopic(body: SubscriptionRequest, onResponse: CHResult<Any>) =
        currentClient.subscribeToTopic(body, onResponse)

    override fun postBatteryData(deviceID: String, payloadString: String, onResponse: CHResult<Any>) =
        currentClient.postBatteryData(deviceID, payloadString, onResponse)

    override fun myDevicesRegisterSesame2Post(deviceId: String?, req: CHSS2RegisterReq?, onResponse: CHResult<CHSS2RegisterRes>) =
        currentClient.myDevicesRegisterSesame2Post(deviceId, req, onResponse)

    override fun myDevicesRegisterSesame5Post(deviceId: String?, body: Any?, onResponse: CHResult<Any>) =
        currentClient.myDevicesRegisterSesame5Post(deviceId, body, onResponse)

    override fun postCHDeviceInfo(body: CHDeviceInfo, onResponse: CHResult<Any>) =
        currentClient.postCHDeviceInfo(body, onResponse)

    override fun updateBotScript(body: BotScriptRequest, onResponse: CHResult<Any>) =
        currentClient.updateBotScript(body, onResponse)

    override fun updateHub3Switch(historytag: ByteArray?, hub3: CHDevices, onResponse: CHResult<CHEmpty>) =
        currentClient.updateHub3Switch(historytag, hub3, onResponse)
}
}