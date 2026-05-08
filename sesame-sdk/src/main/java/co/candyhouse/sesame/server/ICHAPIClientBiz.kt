package co.candyhouse.sesame.server

import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.open.devices.base.CHDevices
import co.candyhouse.sesame.open.devices.base.CHSesameLock
import co.candyhouse.sesame.server.dto.AuthenticationDataWrapper
import co.candyhouse.sesame.server.dto.BotScriptRequest
import co.candyhouse.sesame.server.dto.CHDeviceInfo
import co.candyhouse.sesame.server.dto.CHRemoveSignKeyRequest
import co.candyhouse.sesame.server.dto.CHSS2RegisterReq
import co.candyhouse.sesame.server.dto.CHSS2RegisterRes
import co.candyhouse.sesame.server.dto.CHUserKey
import co.candyhouse.sesame.server.dto.SubscriptionRequest
import co.candyhouse.sesame.utils.CHEmpty
import co.candyhouse.sesame.utils.CHResult

/**
 * Interface for CHAPIClientBiz to support both Online and Offline modes
 */
interface ICHAPIClientBiz {
    fun upLoadKeys(keys: List<CHUserKey>, onResponse: CHResult<Array<CHUserKey>>)
    fun putKey(key: CHUserKey, onResponse: CHResult<Any>)
    fun getDevicesList(onResponse: CHResult<Array<CHUserKey>>)
    fun removeKey(keyId: String, onResponse: CHResult<Any>)
    fun addFriend(friendID: String, onResponse: CHResult<Any>)
    fun uploadUserDeviceToken(deviceToken: String, onResponse: CHResult<Any>)
    fun getWebUrlByScene(scene: String, extInfo: Map<String, String>? = null, onResponse: CHResult<String>)
    fun cancelNotification(device: CHSesameLock, fcmToken: String, onResponse: CHResult<Any>)
    fun getHub3StatusFromIot(deviceUUID: String, onResponse: CHResult<Any>)
    fun updateDeviceFirmwareVersion(deviceUUID: String, versionTag: String, onResponse: CHResult<Any>)
    fun postSS2History(deviceID: String, hisHex: String, onResponse: CHResult<Any>)
    fun postOS3History(deviceID: String, hisHex: String, onResponse: CHResult<Any>)
    fun postCredentialListToServer(credentialListRequest: AuthenticationDataWrapper, onResponse: CHResult<Any>)
    fun updateAuthenticationName(authData: Any, onResponse: CHResult<Any>)
    fun deleteCredentialInfo(request: AuthenticationDataWrapper, onResponse: CHResult<Any>)
    fun subscribeToTopic(body: SubscriptionRequest, onResponse: CHResult<Any>)
    fun postBatteryData(deviceID: String, payloadString: String, onResponse: CHResult<Any>)
    fun myDevicesRegisterSesame5Post(deviceId: String?, body: Any?, onResponse: CHResult<Any>)
    fun postCHDeviceInfo(body: CHDeviceInfo, onResponse: CHResult<Any>)
    fun updateBotScript(body: BotScriptRequest, onResponse: CHResult<Any>)
    fun updateHub3Switch(historytag: ByteArray?, hub3: CHDevices, onResponse: CHResult<CHEmpty>)

    // Internal methods
    fun signGuestKey(key: CHRemoveSignKeyRequest, onResponse: CHResult<String>)
    fun cmdSesame(cmd: SesameItemCode, ss2: CHDevices, historytag: ByteArray, onResponse: CHResult<CHEmpty>)
    fun myDevicesRegisterSesame2Post(deviceId: String?, req: CHSS2RegisterReq?, onResponse: CHResult<CHSS2RegisterRes>)
}