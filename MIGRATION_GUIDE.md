# Implementation Summary: Local Server Backend for Sesame SDK

## 📋 Project Overview

This implementation provides a local HTTP server alternative to AWS IoT Core and API Gateway, enabling:
- ✅ **Offline-first design** with local BLE device control
- ✅ **Small HTTP server** replacement for cloud services  
- ✅ **OS3 devices only** (OS2 support removed)
- ✅ **Optional Firebase** (disabled by default)
- ✅ **Minimal configuration** for ease of setup

## 📁 Files Created

### Backend Server
```
local-server/
├── package.json                    # Node.js dependencies
├── minimal-server.js               # Minimal HTTP server (minimal Node.js)
├── server.js                       # Full Express.js server (recommended)
└── README.md                       # Server setup guide
```

### Android SDK
```
sesame-sdk/src/main/java/co/candyhouse/sesame/server/
├── LocalServerConfig.kt            # Configuration management (NEW)
├── LocalHttpClient.kt              # Local HTTP client (NEW)
└── CHAPIClientBiz.kt              # Modified to support local server
```

### Android App
```
app/src/main/java/co/candyhouse/app/
├── base/BaseApp.kt                 # Modified: Init local server if needed
└── ...existing code...
```

### Firebase Service
```
app/src/main/java/co/receiver/
└── SesameFirebaseMessagingService.kt  # Modified: Disable in local mode
```

### Documentation
```
LOCAL_SERVER_SETUP.md               # Complete setup guide
LOCAL_SERVER_CONFIG.md              # Configuration options
MIGRATION_GUIDE.md                  # This file
```

## 🔧 Key Modifications

### 1. CHAPIClientBiz.kt (sesame-sdk)

**Changes:**
- Added `initializeLocalServer()` method for non-AWS initialization
- Added helper `makeLocalApiCall()` for HTTP requests
- Added L (logging) import for debug logging
- All existing AWS methods remain unchanged for backward compatibility

**Before:**
```kotlin
fun initialize(context, credentialsProvider, region, apiKey)
```

**After:**
```kotlin
fun initialize(context, credentialsProvider, region, apiKey)  // AWS mode
fun initializeLocalServer(context, endpoint)                  // Local mode
```

### 2. LocalServerConfig.kt (NEW)

**Purpose:** Centralized configuration for local server
- Toggle local server on/off
- Manage server endpoint
- SharedPreferences persistence

**Key Methods:**
```kotlin
LocalServerConfig.initialize(context)
LocalServerConfig.isEnabled()
LocalServerConfig.getServerEndpoint()
LocalServerConfig.setEnabled(enabled)
LocalServerConfig.setServerEndpoint(endpoint)
LocalServerConfig.getFullApiUrl(path)
```

### 3. LocalHttpClient.kt (NEW)

**Purpose:** Pure HTTP client replacing AWS SDK
- No AWS dependencies needed
- Standard URLConnection-based implementation
- Coroutine-aware async operations

**Key Methods:**
```kotlin
suspend fun makeRequest(method, path, body, headers)
suspend fun postJson(path, body)
suspend fun getJson(path)
suspend fun putJson(path, body)  
suspend fun deleteJson(path, body)
```

### 4. BaseApp.kt (app)

**Changes:**
- Modified `setupCrashlytics()` to disable Firebase in debug/local mode
- Modified `initializeAWS()` to detect and use local server mode
- Added fallback from AWS to local server if AWS init fails
- Added L import for logging

**Logic:**
```kotlin
if (USE_LOCAL_SERVER || AWS_INIT_FAILS) {
    CHAPIClientBiz.initializeLocalServer(this)
} else {
    AWSStatus.initAWSMobileClient(this)
    setCHAPIClient()
}
```

### 5. CHIotManager.kt (sesame-sdk)

**Changes:**
- Added check in `startConnection()` to skip AWS IoT if local server enabled
- MQTT connection gracefully skipped without errors

**Logic:**
```kotlin
if (LocalServerConfig.isEnabled()) {
    // Skip AWS IoT - using local server
    return
}
// Proceed with AWS IoT connection...
```

### 6. SesameFirebaseMessagingService.kt (app)

**Changes:**
- Added local server checks in `onMessageReceived()`
- Added local server checks in `onNewToken()`
- Firebase gracefully disabled without errors

**Logic:**
```kotlin
if (LocalServerConfig.isEnabled()) {
    // Skip Firebase messaging
    return
}
// Handle Firebase message...
```

## 🌐 Local Server Endpoints

### Device Management
```
POST   /device                      Upload device keys
GET    /device/list                 List all devices
PUT    /device                      Update device key
DELETE /device                      Delete device key
```

### Device Control (Core)
```
POST   /device/v1/iot/sesame2/:device_id     Send lock/unlock commands
POST   /device/v1/sesame5/:device_id         Register OS3 device
```

### Status & Monitoring
```
GET    /device/v1/wifi_module/:device_id/status       Get device status
POST   /device/v1/sesame5/:device_id/battery          Upload battery data
POST   /device/v1/sesame5/:device_id/fwVer            Upload firmware version
```

### History & Logs
```
POST   /device/v1/sesame2/historys            Upload device history
POST   /device/infor                          Post device info
```

### Stub Endpoints (Optional)
```
POST   /friend                    Friend management
POST   /friend/token              FCM token registration
DELETE /device/v1/token           FCM token removal
POST   /device/v1/biometrics      Biometric operations
POST   /device/v1/subscribe       Topic subscriptions
POST   /device/v1/bot/script      Bot script updates
POST   /device/v1/wifi_module/{id}/switch    Switch control
POST   /web_route                 Web URL generation
```

## 🚀 Deployment Options

### Option 1: Local Network Server (Recommended)

```bash
cd local-server
npm install
PORT=3000 npm start
```

Access from Android:
```
http://192.168.1.100:3000/health
```

### Option 2: Docker Container

```dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package.json .
RUN npm install
COPY . .
CMD ["npm", "start"]
EXPOSE 3000
```

Build and run:
```bash
docker build -t sesame-server .
docker run -p 3000:3000 sesame-server
```

### Option 3: Cloud Deployment

```bash
# Deploy to Heroku
heroku create sesame-local-server
git push heroku main

# Or use Cloud Run
gcloud run deploy sesame-server --source local-server
```

## 🔐 Security Considerations

### For Development
- ✅ HTTP allowed (no HTTPS required)
- ✅ Minimal authentication (appidentifyid header)
- ✅ In-memory data storage (no persistence)

### For Production
- ❌ HTTPS required
- ❌ JWT/OAuth authentication needed
- ❌ Database persistence required
- ❌ Rate limiting and throttling
- ❌ Device signature verification

See `LOCAL_SERVER_CONFIG.md` for HTTPS and authentication setup.

## 📱 Android Configuration

### Method 1: BuildConfig (Recommended for Development)

Add to `build.gradle`:

```gradle
buildTypes {
    debug {
        buildConfigField "boolean", "USE_LOCAL_SERVER", "true"
        buildConfigField "String", "LOCAL_SERVER_ENDPOINT", "\"http://192.168.1.100:3000\""
    }
}
```

### Method 2: Runtime Configuration

```kotlin
// At app startup
LocalServerConfig.initialize(context)
LocalServerConfig.setEnabled(true)
LocalServerConfig.setServerEndpoint("http://192.168.1.100:3000")
```

### Method 3: Settings UI

Users can toggle local server mode via app settings (see XML config file).

## ✅ Testing

### Test Local Server

```bash
# Health check
curl http://localhost:3000/health

# List devices
curl -X GET http://localhost:3000/device/list \
  -H "appidentifyid: test-app"

# Register device
curl -X POST http://localhost:3000/device/v1/sesame5/ABC123 \
  -H "appidentifyid: test-app" \
  -H "Content-Type: application/json" \
  -d '{"deviceId":"ABC123"}'
```

### Test Android App

1. **BLE Control** (works offline):
   - Ensure device is paired
   - Open app
   - Press lock/unlock buttons
   - Verify commands sent via BLE

2. **Server Integration**:
   - With server running
   - Register new device
   - Verify battery data uploaded
   - Check history logs saved

3. **Offline Mode**:
   - Stop server
   - Verify app still works for BLE
   - Resume server, verify reconnection

## 🔄 Migration Path

### From AWS to Local Server

1. **Keep existing code:** All AWS code remains, just disabled
2. **Add new classes:** LocalServerConfig, LocalHttpClient
3. **Modify initialization:** BaseApp detects and uses local server
4. **Test ABS:** Verify BLE and registration work
5. **Deploy server:** Run on local network
6. **Configure app:** Point to server endpoint

### Rollback to AWS

Simply disable local server mode:

```kotlin
LocalServerConfig.setEnabled(false)
```

All AWS code is still present and functional.

## 📊 Comparison: AWS vs Local Server

| Feature | AWS | Local |
|---------|-----|-------|
| BLE Control | ✅ | ✅ |
| Remote Lock | ✅ | ❌ |
| Cloud Sync | ✅ | ❌ |
| Offline Mode | ❌ | ✅ |
| Cost | Monthly | Free |
| Setup | AWS Account | npm install |
| Authentication | Cognito | App ID |
| Scalability | Unlimited | Limited to LAN |
| Firebase Push | ✅ | ❌ |

## 🐛 Troubleshooting

### Server won't start
```bash
# Check port is available
lsof -i :3000

# Try different port
PORT=8080 npm start
```

### App can't connect
```bash
# Test connectivity
adb shell curl http://192.168.1.100:3000/health

# Check network
adb shell ip addr show
```

### Firebase errors
```bash
# Normal in local mode
# Check logs: adb logcat | grep Firebase
```

## 📚 Documentation Files

| File | Purpose |
|------|---------|
| `LOCAL_SERVER_SETUP.md` | Complete setup and usage guide |
| `LOCAL_SERVER_CONFIG.md` | Configuration options and examples |
| `MIGRATION_GUIDE.md` | This file - implementation details |
| `local-server/README.md` | Backend server documentation |

## 🎯 Next Steps

1. **Setup server:** Follow `local-server/README.md`
2. **Configure app:** Edit `build.gradle` or use SharedPreferences
3. **Build and test:** `./gradlew :app:assembleDebug`
4. **Verify BLE:** Pair device, test lock/unlock
5. **Monitor logs:** Use `adb logcat` to debug
6. **Deploy:** Run server on stable local network or cloud

## 💡 Tips & Tricks

### Auto-Discovery
```kotlin
// Implement mDNS service discovery
val discoveredServer = discoverSesameServer() // Finds _sesame._tcp
LocalServerConfig.setServerEndpoint(discoveredServer)
```

### Data Sync
```kotlin
// Queue commands when server offline
val queue = mutableListOf<PendingCommand>()

// Retry when connection restored
ConnectionMonitor.onConnected { 
    queue.forEach { sendRetry(it) }
}
```

### Hybrid Operation
```kotlin
// Use BLE for control, server for registration/history
if (bleAvailable) {
    device.lockViaBLE()
} else {
    device.lockViaServer()
}
```

## 📄 License

Same as original SesameSDK - See LICENSE file

---

**Last Updated:** 2026-05-06  
**Status:** Implementation Complete  
**Testing Status:** Ready for integration testing

