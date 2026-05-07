package co.candyhouse.sesame.utils

import co.candyhouse.sesame.server.LocalServerConfig
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.results.Tokens

/**
 * Token管理类
 *
 * @author frey on 2025/9/15
 */
object TokenManager {

    fun getValidToken(callback: (Result<String?>) -> Unit) {
        if (LocalServerConfig.isOfflineMode() || LocalServerConfig.isEnabled()) {
            callback(Result.success("offline_token"))
            return
        }
        if (!AWSMobileClient.getInstance().isSignedIn()) {
            callback(Result.success(null))
            return
        }

        AWSMobileClient.getInstance().getTokens(object : Callback<Tokens> {
            override fun onResult(result: Tokens) {
                callback(Result.success(result.idToken.tokenString))
            }

            override fun onError(e: Exception) {
                callback(Result.failure(e))
            }
        })
    }
}