package co.receiver

import co.candyhouse.sesame.server.LocalServerConfig
import co.candyhouse.sesame.utils.L
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Firebase Cloud Messaging Service - DISABLED for local server mode
 *
 * This service is kept for backward compatibility but does nothing.
 * In local server mode, push notifications are not used.
 *
 * To completely remove this service:
 * 1. Delete this file
 * 2. Remove service declaration from AndroidManifest.xml
 * 3. Remove Firebase dependency from build.gradle
 */
class SesameFirebaseMessagingService : FirebaseMessagingService() {

    private val tag = "SesameFirebaseMessagingService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (LocalServerConfig.isEnabled() || LocalServerConfig.isOfflineMode()) {
            L.d(tag, "Firebase messaging is disabled in ${if (LocalServerConfig.isOfflineMode()) "offline" else "local server"} mode")
            return
        }
        // Original Firebase handling would go here
    }

    override fun onNewToken(token: String) {
        if (LocalServerConfig.isEnabled() || LocalServerConfig.isOfflineMode()) {
            L.d(tag, "FCM token registration disabled in ${if (LocalServerConfig.isOfflineMode()) "offline" else "local server"} mode")
            return
        }
        // Original Firebase handling would go here
    }
}