package co.candyhouse.sesame.server

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/**
 * Configuration for local server mode.
 * Manages server endpoint and enables/disables remote services.
 */
object LocalServerConfig {

    private var isLocalServerMode = false
    private var isOfflineMode = false
    private var serverEndpoint = "http://192.168.1.100:3000"
    private var preferences: SharedPreferences? = null

    private const val KEY_LOCAL_SERVER_ENABLED = "local_server_enabled"
    private const val KEY_OFFLINE_MODE_ENABLED = "offline_mode_enabled"
    private const val KEY_SERVER_ENDPOINT = "local_server_endpoint"
    private const val DEFAULT_ENDPOINT = "http://192.168.1.100:3000"

    fun initialize(context: Context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        isLocalServerMode = preferences?.getBoolean(KEY_LOCAL_SERVER_ENABLED, false) ?: false
        isOfflineMode = preferences?.getBoolean(KEY_OFFLINE_MODE_ENABLED, false) ?: false
        serverEndpoint = preferences?.getString(KEY_SERVER_ENDPOINT, DEFAULT_ENDPOINT) ?: DEFAULT_ENDPOINT
    }

    fun isEnabled(): Boolean = isLocalServerMode

    fun isOfflineMode(): Boolean = isOfflineMode

    fun getServerEndpoint(): String = serverEndpoint

    fun setEnabled(enabled: Boolean) {
        isLocalServerMode = enabled
        preferences?.edit()?.putBoolean(KEY_LOCAL_SERVER_ENABLED, enabled)?.apply()
    }

    fun setOfflineMode(enabled: Boolean) {
        isOfflineMode = enabled
        preferences?.edit()?.putBoolean(KEY_OFFLINE_MODE_ENABLED, enabled)?.apply()
    }

    fun setServerEndpoint(endpoint: String) {
        serverEndpoint = endpoint
        preferences?.edit()?.putString(KEY_SERVER_ENDPOINT, endpoint)?.apply()
    }

    fun getFullApiUrl(path: String): String {
        return if (serverEndpoint.endsWith("/")) {
            serverEndpoint + path.trimStart('/')
        } else {
            "$serverEndpoint/$path".replace("//", "/").replace("http:/", "http://")
        }
    }
}

