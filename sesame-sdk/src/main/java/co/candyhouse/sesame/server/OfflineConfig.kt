package co.candyhouse.sesame.server

import android.content.Context
import android.content.SharedPreferences

/**
 * Offline configuration class that handles both local server and offline mode settings
 */
object OfflineConfig {
    private const val PREFS_NAME = "sesame_offline_config"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_OFFLINE_MODE = "offline_mode"
    private const val KEY_SERVER_ENDPOINT = "server_endpoint"

    private var prefs: SharedPreferences? = null

    fun initialize(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isEnabled(): Boolean {
        return prefs?.getBoolean(KEY_ENABLED, false) ?: false
    }

    fun setEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_ENABLED, enabled)?.apply()
    }

    fun isOfflineMode(): Boolean {
        return prefs?.getBoolean(KEY_OFFLINE_MODE, false) ?: false
    }

    fun setOfflineMode(offline: Boolean) {
        prefs?.edit()?.putBoolean(KEY_OFFLINE_MODE, offline)?.apply()
    }

    fun getServerEndpoint(): String {
        return prefs?.getString(KEY_SERVER_ENDPOINT, "") ?: ""
    }

    fun setServerEndpoint(endpoint: String) {
        prefs?.edit()?.putString(KEY_SERVER_ENDPOINT, endpoint)?.apply()
    }
}