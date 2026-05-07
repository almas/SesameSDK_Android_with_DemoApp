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
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CHAPIClientBizOnline(
    private val appContext: Context,
    private val cHApiClient: CHAPIClient
) : ICHAPIClientBiz {

    private val httpScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val TAG = "CHAPIClientBizOnline"

    private fun identifyId(): String {
        return AppIdentifyIdUtil.get(appContext)
    }

    private fun <T> makeApiCall(onResponse: CHResult<T>, block: () -> T) {
        httpScope.launch {
            runCatching { block() }
                .onSuccess { onResponse(Result.success(CHResultState.CHResultStateNetworks(it))) }
                .onFailure { onResponse(Result.failure(it)) }
        }
    }

    override fun upLoadKeys(keys: List<CHUserKey>, onResponse: CHResult<Array<CHUserKey>>) =
        makeApiCall(onResponse) { cHApiClient.updateKeys(identifyId(), keys) }

    override fun putKey(key: CHUserKey, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) { cHApiClient.putKey(identifyId(), key) }

    override fun getDevicesList(onResponse: CHResult<Array<CHUserKey>>) =
        makeApiCall(onResponse) { cHApiClient.getDevicesList(identifyId()) }

    override fun removeKey(keyId: String, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) { cHApiClient.removeKey(identifyId(), keyId) }

    override fun addFriend(friendID: String, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) { cHApiClient.addFriend(identifyId(), friendID) }

    override fun uploadUserDeviceToken(deviceToken: String, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) { cHApiClient.uploadDeviceToken(identifyId(), deviceToken) }

    override fun getWebUrlByScene(scene: String, extInfo: Map<String, String>?, onResponse: CHResult<String>) {
        httpScope.launch {
            TokenManager.getValidToken { result ->
                result.fold(
                    onSuccess = { token ->
                        runCatching {
                            val req = ScenePayload(scene = scene, token = token, extInfo = extInfo)
                            val resp = cHApiClient.getWebUrlByScene(identifyId(), req)
                            val url = Gson().toJsonTree(resp).asJsonObject["url"].asString
                            onResponse(Result.success(CHResultState.CHResultStateNetworks(url)))
                        }.onFailure { onResponse(Result.failure(it)) }
                    },
                    onFailure = { onResponse(Result.failure(it)) }
                )
            }
        }
    }

    override fun cancelNotification(device: CHSesameLock, fcmToken: String, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) {
            cHApiClient.fcmTokenSignDelete(
                CHFcmTokenUpload((device as CHDevices).deviceId.toString().uppercase(), fcmToken)
            )
        }

    override fun getHub3StatusFromIot(deviceUUID: String, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) { cHApiClient.getHub3StatusFromIot(deviceUUID) }

    override fun updateDeviceFirmwareVersion(deviceUUID: String, versionTag: String, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) {
            cHApiClient.postFirmwareVersion(deviceUUID, mapOf("versionTag" to versionTag))
        }

    override fun postSS2History(deviceID: String, hisHex: String, onResponse: CHResult<Any>) {
        CHDB.CHHistoryModel.insert(deviceID, hisHex)
        makeApiCall(onResponse) { cHApiClient.feedHistory(CHSSMHisUploadRequest(deviceID, hisHex)) }
    }

    override fun postOS3History(deviceID: String, hisHex: String, onResponse: CHResult<Any>) {
        CHDB.CHHistoryModel.insert(deviceID, hisHex)
        makeApiCall(onResponse) { cHApiClient.feedHistory(CHSS5HisUploadRequest(deviceID, hisHex, "5")) }
    }

    override fun postCredentialListToServer(credentialListRequest: AuthenticationDataWrapper, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) { cHApiClient.biometricsOperation(credentialListRequest) }

    override fun updateAuthenticationName(authData: Any, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) { cHApiClient.biometricsOperation(authData) }

    override fun deleteCredentialInfo(request: AuthenticationDataWrapper, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) { cHApiClient.biometricsOperation(request) }

    override fun subscribeToTopic(body: SubscriptionRequest, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) { cHApiClient.subscribeToTopic(body) }

    override fun postBatteryData(deviceID: String, payloadString: String, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) { cHApiClient.postBatteryData(deviceID, CHBatteryDataReq(payloadString)) }

    override fun myDevicesRegisterSesame5Post(deviceId: String?, body: Any?, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) { cHApiClient.myDevicesRegisterSesame5Post(deviceId, body) }
    }

    override fun postCHDeviceInfo(body: CHDeviceInfo, onResponse: CHResult<Any>) {
        makeApiCall(onResponse) { cHApiClient.postCHDeviceInfo(body) }
    }

    override fun updateBotScript(body: BotScriptRequest, onResponse: CHResult<Any>) =
        makeApiCall(onResponse) { cHApiClient.updateBotScript(body) }

    override fun updateHub3Switch(historytag: ByteArray?, hub3: CHDevices, onResponse: CHResult<CHEmpty>) =
        makeApiCall(onResponse) {
            val sendMap: MutableMap<String, String> = mutableMapOf()
            val timestamp = (System.currentTimeMillis() / 1000).toInt()
            val buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
            buffer.putInt(timestamp)
            val msg = buffer.array().sliceArray(1..3) // 取第1-3字节

            val sign = AesCmac((hub3 as CHDeviceUtil).sesame2KeyData!!.secretKey.hexStringToByteArray(), 16)
                .computeMac(msg)!!.sliceArray(0..3)

            val cmd: Int = SesameItemCode.HUB3_OS3_RELAY_SWITCH.value.toInt()
            val hub3DeviceId = hub3.deviceId?.toString()?.uppercase() ?: ""
            val deviceIdBytes = hub3DeviceId.toByteArray(Charsets.UTF_8)
            val open = 0x01 // 保留字节，目前固定为0x01，代表开关操作
            val op = open.toByte() // 保留字节，目前固定为0x01，代表开关操作

            val payloadBytes = ByteArray(sign.size + 1 + deviceIdBytes.size + 2)
            var offset = 0
            System.arraycopy(sign, 0, payloadBytes, offset, sign.size)
            offset += sign.size
            payloadBytes[offset++] = cmd.toByte()
            System.arraycopy(deviceIdBytes, 0, payloadBytes, offset, deviceIdBytes.size)
            offset += deviceIdBytes.size
            payloadBytes[offset] = op
            val payload = Base64.encodeToString(payloadBytes, Base64.NO_WRAP)
            val hub3DeviceIdLastSegment = hub3DeviceId.substringAfterLast('-')

            sendMap["action"] = "biz3OperateIoT"
            sendMap["op"] = "cmd"
            sendMap["payload"] = payload
            sendMap["topic"] = "wm2${hub3DeviceIdLastSegment.uppercase()}cmd"

            cHApiClient.updateHub3Switch(hub3DeviceId, sendMap)
            CHEmpty()
        }

    override fun signGuestKey(key: CHRemoveSignKeyRequest, onResponse: CHResult<String>) =
        makeApiCall(onResponse) { cHApiClient.guestKeysSignPost(key) }

    override fun cmdSesame(cmd: SesameItemCode, ss2: CHDevices, historytag: ByteArray, onResponse: CHResult<CHEmpty>) =
        makeApiCall(onResponse) {
            val msg = System.currentTimeMillis().toUInt24ByteArray()
            val keyCheck = AesCmac((ss2 as CHDeviceUtil).sesame2KeyData!!.secretKey.hexStringToByteArray(), 16)
                .computeMac(msg)!!
                .sliceArray(0..3)

            cHApiClient.ss2CommandToWM2Post(
                ss2.deviceId.toString().uppercase(),
                CHSS2WebCMDReq(cmd.value, historytag.base64Encode(), keyCheck.toHexString())
            )
            CHEmpty()
        }

    override fun myDevicesRegisterSesame2Post(deviceId: String?, req: CHSS2RegisterReq?, onResponse: CHResult<CHSS2RegisterRes>) {
        makeApiCall(onResponse) { cHApiClient.myDevicesRegisterSesame2Post(deviceId, req) }
    }
}
