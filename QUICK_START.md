# Quick Start Guide: Sesame SDK Local Server Mode

## ⚡ 5-Minute Setup

### Step 1: Start Local Server (2 min)

```bash
cd SesameSDK_Android_with_DemoApp/local-server
npm install
npm start
```

Server runs on: `http://localhost:3000` (or `http://192.168.1.100:3000` on LAN)

### Step 2: Configure Android App (1 min)

**Option A: Automatic (Recommended)**

Edit `app/build.gradle`:

```gradle
android {
    buildTypes {
        debug {
            buildConfigField "boolean", "USE_LOCAL_SERVER", "true"
            buildConfigField "String", "LOCAL_SERVER_ENDPOINT", "\"http://192.168.1.100:3000\""
        }
    }
}
```

**Option B: Runtime**

In your activity:

```kotlin
CHAPIClientBiz.initializeLocalServer(this, "http://192.168.1.100:3000")
```

### Step 3: Build and Deploy (2 min)

```bash
./gradlew :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Step 4: Test (Testing your setup)

#### Quick Test Commands

```bash
# Test server is running
curl http://192.168.1.100:3000/health
# Response: {"status":"ok"}

# List devices
curl http://192.168.1.100:3000/device/list \
  -H "appidentifyid: test"
# Response: []
```

#### Test via App UI

1. Open Sesame app
2. Pair device via Bluetooth
3. Press Lock/Unlock button
4. Should work via local BLE!

## 📱 Device Support

| Device | OS Version | Status |
|--------|-----------|--------|
| Sesame 5 | OS3 | ✅ Supported |
| Sesame Bot 2 | OS3 | ✅ Supported |
| Smart Lock Pro 2 | OS3 | ✅ Supported |
| Sesame 3 | OS2 | ❌ Removed |
| Bot | OS2 | ❌ Removed |

All OS3 devices work with local server!

## 🔧 Configuration Options

### Server Endpoint

```kotlin
// Local network
"http://192.168.1.100:3000"

// Emulator (from Android Studio)
"http://10.0.2.2:3000"

// Docker container
"http://docker-host.internal:3000"

// Public cloud
"https://api.example.com"
```

### Enable/Disable Local Server

```kotlin
// Enable
LocalServerConfig.setEnabled(true)

// Disable (fallback to AWS)
LocalServerConfig.setEnabled(false)

// Check status
if (LocalServerConfig.isEnabled()) {
    Log.d("Sesame", "Using: " + LocalServerConfig.getServerEndpoint())
}
```

## 🧪 Testing Checklist

- [ ] Server running: `curl http://localhost:3000/health` → OK
- [ ] Device paired via Bluetooth
- [ ] App built with local server config
- [ ] App launches without Firebase errors
- [ ] Lock/Unlock via BLE works
- [ ] Device registration succeeds
- [ ] Battery data uploads
- [ ] Server logs show requests (`/device/list`, etc.)

## 🚨 Common Issues

### "Connection refused"
```
❌ Server not running
✅ Start server: npm start
✅ Check port: lsof -i :3000
```

### "Unknown host"
```
❌ Wrong IP address
✅ Check local IP: ipconfig getifaddr en0 (macOS) or ipconfig (Windows)
✅ Ensure app and server on same network
```

### "App crashes on startup"
```
❌ Firebase/AWS config error
✅ This is normal! Local mode disables these
✅ Check: adb logcat | grep "CHAPIClientBiz"
```

### "Firebase errors in logs"
```
❌ Actually OK - expected in local mode
✅ Firebase is gracefully disabled
✅ Check adb logcat | grep "LocalServer"
```

## 📊 Server Status Endpoints

```bash
# Get all devices (admin)
curl http://localhost:3000/admin/devices

# Get device history
curl http://localhost:3000/admin/history/DEVICE_ID

# Clean data (debug)
curl -X DELETE http://localhost:3000/admin/devices/DEVICE_ID
```

## 🔌 API Headers

All requests need:

```
Header: appidentifyid
Value: any-unique-id (e.g., "test-app", "my-phone")

Header: Content-Type  
Value: application/json
```

Example:
```bash
curl -X POST http://localhost:3000/device \
  -H "appidentifyid: my-app" \
  -H "Content-Type: application/json" \
  -d '[{"deviceId":"ABC123"}]'
```

## 📈 Next Steps

### For Development
1. ✅ **Local Server**: Running and tested
2. ✅ **App**: Configured and building
3. ➡️ **Next**: Add device registration UI (see Dev Guide)
4. ➡️ **Next**: Implement history sync

### For Production
1. ➡️ **Database**: Add MongoDB/PostgreSQL
2. ➡️ **Authentication**: Implement JWT tokens
3. ➡️ **HTTPS**: Get SSL certificate
4. ➡️ **Deployment**: Docker/Cloud Run

## 📚 Full Documentation

- **Setup Details**: See `LOCAL_SERVER_SETUP.md`
- **Configuration Options**: See `LOCAL_SERVER_CONFIG.md`
- **Technical Details**: See `MIGRATION_GUIDE.md`
- **Server API**: See `local-server/README.md`

## 🆘 Need Help?

### Check Logs

```bash
# App logs
adb logcat | grep -E "(CHAPIClientBiz|LocalServer|LocalHttp)"

# Server logs
# Check terminal where npm start is running
```

### Test Connectivity

```bash
# From Android
adb shell curl http://192.168.1.100:3000/health

# From Mac/Linux
curl http://192.168.1.100:3000/health

# With verbose output
curl -v http://192.168.1.100:3000/health
```

### Debug Mode

```kotlin
// Add to BaseApp.kt
if (BuildConfig.DEBUG && LocalServerConfig.isEnabled()) {
    Log.d("LocalServer", "Endpoint: ${LocalServerConfig.getServerEndpoint()}")
    Log.d("LocalServer", "Enabled: ${LocalServerConfig.isEnabled()}")
}
```

---

## 🎉 That's It!

You now have:
- ✅ Local HTTP server running
- ✅ Android app configured
- ✅ OS3 devices support
- ✅ BLE lock/unlock working
- ✅ Firebase disabled (optional)

**Happy locking! 🔐**

---

**Version:** 1.0  
**Updated:** 2026-05-06  
**Status:** Ready for use

