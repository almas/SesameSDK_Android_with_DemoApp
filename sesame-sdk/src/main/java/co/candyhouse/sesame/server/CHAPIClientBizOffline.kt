package co.candyhouse.sesame.server

import co.candyhouse.sesame.ble.SesameItemCode
import co.candyhouse.sesame.db.CHDB
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
import co.candyhouse.sesame.utils.L

internal class CHAPIClientBizOffline : ICHAPIClientBiz {

    private val TAG = "CHAPIClientBizOffline"

    private fun <T> offlineFailure(onResponse: CHResult<T>) {
        L.d(TAG, "Offline mode: skipping API call")
        onResponse(Result.failure(Exception("Offline mode enabled")))
    }

    override fun upLoadKeys(keys: List<CHUserKey>, onResponse: CHResult<Array<CHUserKey>>) = offlineFailure(onResponse)

    override fun putKey(key: CHUserKey, onResponse: CHResult<Any>) = offlineFailure(onResponse)

    override fun getDevicesList(onResponse: CHResult<Array<CHUserKey>>) = offlineFailure(onResponse)

    override fun removeKey(keyId: String, onResponse: CHResult<Any>) = offlineFailure(onResponse)

    override fun addFriend(friendID: String, onResponse: CHResult<Any>) = offlineFailure(onResponse)

    override fun uploadUserDeviceToken(deviceToken: String, onResponse: CHResult<Any>) = offlineFailure(onResponse)

    override fun getWebUrlByScene(scene: String, extInfo: Map<String, String>?, onResponse: CHResult<String>) = offlineFailure(onResponse)

    override fun cancelNotification(device: CHSesameLock, fcmToken: String, onResponse: CHResult<Any>) = offlineFailure(onResponse)

    override fun getHub3StatusFromIot(deviceUUID: String, onResponse: CHResult<Any>) = offlineFailure(onResponse)

    override fun updateDeviceFirmwareVersion(deviceUUID: String, versionTag: String, onResponse: CHResult<Any>) = offlineFailure(onResponse)

    override fun postSS2History(deviceID: String, hisHex: String, onResponse: CHResult<Any>) {
        L.d(TAG, "Offline mode: saving history locally only")
        CHDB.CHHistoryModel.insert(deviceID, hisHex)
        onResponse(Result.success(co.candyhouse.sesame.utils.CHResultState.CHResultStateNetworks(Any())))
    }

    override fun postOS3History(deviceID: String, hisHex: String, onResponse: CHResult<Any>) {
        L.d(TAG, "Offline mode: saving history locally only")
        CHDB.CHHistoryModel.insert(deviceID, hisHex)
        onResponse(Result.success(co.candyhouse.sesame.utils.CHResultState.CHResultStateNetworks(Any())))
    }

    override fun postCredentialListToServer(credentialListRequest: AuthenticationDataWrapper, onResponse: CHResult<Any>) = offlineFailure(onResponse)

    override fun updateAuthenticationName(authData: Any, onResponse: CHResult<Any>) = offlineFailure(onResponse)

    override fun deleteCredentialInfo(request: AuthenticationDataWrapper, onResponse: CHResult<Any>) = offlineFailure(onResponse)

    override fun subscribeToTopic(body: SubscriptionRequest, onResponse: CHResult<Any>) = offlineFailure(onResponse)

    override fun postBatteryData(deviceID: String, payloadString: String, onResponse: CHResult<Any>) = offlineFailure(onResponse)

    override fun myDevicesRegisterSesame5Post(deviceId: String?, body: Any?, onResponse: CHResult<Any>) = offlineFailure(onResponse)

    override fun postCHDeviceInfo(body: CHDeviceInfo, onResponse: CHResult<Any>) = offlineFailure(onResponse)

    override fun updateBotScript(body: BotScriptRequest, onResponse: CHResult<Any>) = offlineFailure(onResponse)

    override fun updateHub3Switch(historytag: ByteArray?, hub3: CHDevices, onResponse: CHResult<CHEmpty>) = offlineFailure(onResponse)

    override fun signGuestKey(key: CHRemoveSignKeyRequest, onResponse: CHResult<String>) = offlineFailure(onResponse)

    override fun cmdSesame(cmd: SesameItemCode, ss2: CHDevices, historytag: ByteArray, onResponse: CHResult<CHEmpty>) = offlineFailure(onResponse)

    override fun myDevicesRegisterSesame2Post(deviceId: String?, req: CHSS2RegisterReq?, onResponse: CHResult<CHSS2RegisterRes>) = offlineFailure(onResponse)
}
