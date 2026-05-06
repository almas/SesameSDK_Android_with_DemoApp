# Sesame SDK - Local Server Mode Setup Guide

## Overview

This guide explains how to use the modified Sesame SDK app with a local HTTP server instead of AWS IoT Core and API Gateway services. This enables:

- ✅ Control devices via local BLE (Bluetooth Low Energy)
- ✅ Local HTTP server for device management
- ✅ Offline-first design with minimal cloud dependencies
- ✅ OS3 devices only (OS2 support removed for simplicity)
- ✅ Optional Firebase (disabled by default in debug mode)

## Architecture

```
┌─────────────┐         ┌──────────────────┐         ┌─────────────┐
│   Android   │  BLE    │  Sesame Devices  │         │  Local HTTP │
│    App      │◄────────►  (Lock/Unlock)   │         │   Server    │
│             │         └──────────────────┘         │  (Optional) │
└─────────────┘                                      └─────────────┘
      │
      │ Device registration, history, battery
      └─────────────────────────────────────────────►
```

## Quick Start

### 1. Start the Local Server

**Using Node.js (Express):**

```bash
cd local-server
npm install
npm start
```

Server will run on `http://localhost:3000`

**Or using minimal server:**

```bash
node minimal-server.js
```

### 2. Configure the Android App

By default, the app is configured for local server mode in DEBUG builds.

To explicitly enable local server mode in your app code:

```kotlin
// In BaseApp.kt or on app startup
CHAPIClientBiz.initializeLocalServer(context, "http://192.168.1.100:3000")
```

Or via SharedPreferences:

```kotlin
// Enable local server mode
SharedPreferences.edit {
    putBoolean("local_server_enabled", true)
    putString("local_server_endpoint", "http://192.168.1.100:3000")
}
```

### 3. Use the App

The app will automatically:
- Connect to devices via BLE when in range
- Use local HTTP server for device registration and management
- Fall back to local control if server is unavailable
- Skip Firebase messaging in local server mode

## Local Server Endpoints

### Device Management
```
POST   /device                    - Upload device keys
GET    /device/list               - Get devices list
PUT    /device                    - Update device key
DELETE /device                    - Delete device key
```

### Device Control
```
POST   /device/v1/iot/sesame2/:device_id    - Send commands (lock/unlock)
POST   /device/v1/sesame5/:device_id        - Register OS3 device
```

### Status & Monitoring
```
GET    /device/v1/wifi_module/:device_id/status
POST   /device/v1/sesame5/:device_id/battery
POST   /device/v1/sesame5/:device_id/fwVer
```

### History
```
POST   /device/v1/sesame2/historys          - Upload history logs
POST   /device/infor                        - Post device info
```

## Configuration Settings

### Environment Variables

```bash
# Server port (default: 3000)
PORT=8080

# Client endpoint
SESAME_SERVER_ENDPOINT=http://192.168.1.100:3000
```

### SharedPreferences Keys

```kotlin
"local_server_enabled"       // Boolean: Enable local server mode
"local_server_endpoint"      // String: Server URL
```

## API Headers

All requests should include:

```
appidentifyid: <unique-app-identifier>
Content-Type: application/json
```

Example:
```bash
curl -X GET http://localhost:3000/device/list \
  -H "appidentifyid: my-app-001" \
  -H "Content-Type: application/json"
```

## Offline Functionality

The app supports offline device control via BLE:

1. **Local BLE Control** - Always available when device is in range
2. **Server Features Graceful Degradation** - History, battery data, and firmware updates can be queued and sent when server returns online
3. **Login Not Required** - Local BLE operation doesn't require server authentication

## Troubleshooting

### Device Won't Connect

1. Ensure device is paired via BLE
2. Check server is running: `curl http://localhost:3000/health`
3. Verify network connectivity
4. Check app logs: `adb logcat | grep CHAPIClientBiz`

### Server Connection Errors

```
E/LocalHttpClient: Network error: Connection timeout
```

Solutions:
- Verify server endpoint is correct
- Check firewall allows port 3000
- Ensure app and server are on same network
- For Docker/VM: check network bridge settings

### Firebase Errors

In local server mode, Firebase errors are non-fatal:

```
E/FirebaseApp: App initialization failed
```

This is expected and doesn't affect functionality.

## Data Persistence

The local server stores data in-memory by default. For production:

1. **Database Integration** - Add MongoDB, PostgreSQL, or SQLite
2. **Persistent Storage** - Implement file-based history logs
3. **Cloud Sync** - Optional: sync with cloud when available

Example database integration:

```javascript
// server.js
const db = require('mongodb');
const devices = db.collection('devices');

app.post('/device', async (req, res) => {
  await devices.insertOne(req.body);
  res.json({ success: true });
});
```

## Advanced Configuration

### Custom Server Endpoint

```kotlin
// At app startup
LocalServerConfig.initialize(context)
LocalServerConfig.setServerEndpoint("http://my-server.local:3000")
LocalServerConfig.setEnabled(true)
```

### Auto-Discovery (mDNS)

Extend LocalServerConfig for automatic server discovery:

```kotlin
fun discoverServer(): String? {
    // Use Android's NsdManager to discover services on network
    // Look for "_sesame._tcp" service
}
```

### HTTPS Support

To enable HTTPS:

```kotlin
// In LocalHttpClient.kt
val connection = (url.openConnection() as HttpsURLConnection).apply {
    sslSocketFactory = getCustomSSLSocketFactory()
}
```

## Security Notes

For production deployments:

1. **API Authentication** - Replace appidentifyid with JWT or OAuth
2. **HTTPS Encryption** - Use SSL/TLS certificates
3. **Rate Limiting** - Add request throttling
4. **Device Validation** - Verify device signature before executing commands
5. **Access Control** - Implement user/device permission checks

## Files Modified

### Android SDK
- `CHAPIClientBiz.kt` - Added local server support
- `CHIotManager.kt` - Disabled AWS IoT in local mode
- `SesameFirebaseMessagingService.kt` - Disabled FCM in local mode
- `BaseApp.kt` - Added local server initialization

### New Files
- `LocalServerConfig.kt` - Configuration management
- `LocalHttpClient.kt` - HTTP client for local server
- `local-server/server.js` - Express-based HTTP server
- `local-server/minimal-server.js` - Minimal Node.js server

### Removed/Disabled
- OS2 device support code (unused if only OS3 needed)
- AWS API Gateway annotations (kept for compatibility)
- Firebase Crashlytics in DEBUG mode

## Next Steps

1. **Start the server:** `npm start` in local-server/
2. **Build the app:** `./gradlew :app:assembleDebug`
3. **Test locally:** Pair device via BLE, test lock/unlock
4. **Deploy server:** Run on local network or Docker
5. **Scale up:** Add database persistence and authentication

## Support

For issues or questions:
- Check server logs: `curl http://localhost:3000/admin/devices`
- Check app logs: `adb logcat | grep -E "(CHAPIClientBiz|LocalServer|LocalHttp)"`
- Review local-server/README.md for backend details

## License

Same as original SesameSDK - See LICENSE file.

