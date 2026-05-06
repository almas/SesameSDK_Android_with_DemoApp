# 📑 Index & Quick Reference

## 🎯 Start Here

**First time?** Read in this order:
1. ⭐ **QUICK_START.md** - Get running in 5 minutes
2. 📖 **IMPLEMENTATION_SUMMARY.md** - Understand what was done
3. 🔧 **LOCAL_SERVER_CONFIG.md** - Configure for your setup

**Need specific information?**
- 🚀 How do I run the server? → **local-server/README.md**
- ⚙️ How do I configure the app? → **LOCAL_SERVER_CONFIG.md**
- 🔍 What files were changed? → **MIGRATION_GUIDE.md**
- 🐛 Something not working → **LOCAL_SERVER_SETUP.md** (troubleshooting)

## 📂 File Structure

### Documentation Files (Read These)

```
ROOT/
├── 📄 QUICK_START.md                    ⭐ Start here (5 min)
├── 📄 IMPLEMENTATION_SUMMARY.md         Complete summary
├── 📄 LOCAL_SERVER_SETUP.md             Complete guide
├── 📄 LOCAL_SERVER_CONFIG.md            Configuration howto
├── 📄 MIGRATION_GUIDE.md                Technical details
├── 📄 INDEX.md                          This file
│
└── local-server/                        👇 Backend server
    ├── 📄 package.json
    ├── 📄 minimal-server.js
    ├── 📄 README.md
    └── 🚀 server.js (optional)
```

### Code Files (Modified/Created)

```
sesame-sdk/src/main/java/co/candyhouse/sesame/server/
├── 🆕 LocalServerConfig.kt              Configuration management
├── 🆕 LocalHttpClient.kt                HTTP client
└── ✏️  CHAPIClientBiz.kt                Enhanced with local mode

app/src/main/java/
├── ✏️  co/candyhouse/app/base/BaseApp.kt
└── ✏️  co/receiver/SesameFirebaseMessagingService.kt
```

Legend: 🆕 = New file, ✏️ = Modified, 📄 = Documentation, 🚀 = Executable

## 🗺️ Topic Quick Links

### Setup & Deployment

| Topic | File | Section |
|-------|------|---------|
| Quick setup | QUICK_START.md | Steps 1-4 |
| Full setup | LOCAL_SERVER_SETUP.md | Quick Start |
| Server only | local-server/README.md | Installation |
| Docker | LOCAL_SERVER_SETUP.md | Docker section |
| Cloud | QUICK_START.md | Production path |

### Configuration

| Topic | File | Section |
|-------|------|---------|
| BuildConfig | LOCAL_SERVER_CONFIG.md | BuildConfig Configuration |
| Runtime | LOCAL_SERVER_CONFIG.md | Usage in Code |
| SharedPreferences | LOCAL_SERVER_CONFIG.md | Option 2 |
| Product Flavors | LOCAL_SERVER_CONFIG.md | Flavor-based |
| Environment | LOCAL_SERVER_CONFIG.md | Environment-based |

### Troubleshooting

| Issue | File | Solution |
|-------|------|----------|
| Server won't start | QUICK_START.md | Common Issues |
| Can't connect | LOCAL_SERVER_SETUP.md | Troubleshooting |
| Firebase errors | QUICK_START.md | Common Issues |
| Connection refused | QUICK_START.md | Common Issues |
| Wrong IP | QUICK_START.md | Common Issues |

### Technical Details

| Topic | File |
|-------|------|
| Architecture | MIGRATION_GUIDE.md |
| API Endpoints | LocalServerConfig.md / MIGRATION_GUIDE.md |
| Modified files | MIGRATION_GUIDE.md |
| Local server details | local-server/README.md |
| Security | LOCAL_SERVER_SETUP.md |

## 🔧 Code Files Reference

### LocalServerConfig.kt (NEW)

**Location:** `sesame-sdk/src/main/java/co/candyhouse/sesame/server/LocalServerConfig.kt`

**Key Functions:**
```kotlin
LocalServerConfig.initialize(context)        // Call once at startup
LocalServerConfig.isEnabled()                // Check if enabled
LocalServerConfig.getServerEndpoint()        // Get URL
LocalServerConfig.setEnabled(enabled)        // Enable/disable
LocalServerConfig.setServerEndpoint(url)     // Change endpoint
LocalServerConfig.getFullApiUrl(path)        // Build full URL
```

**Usage:**
```kotlin
// Enable and configure
LocalServerConfig.initialize(context)
LocalServerConfig.setEnabled(true)
LocalServerConfig.setServerEndpoint("http://192.168.1.100:3000")

// Check status
if (LocalServerConfig.isEnabled()) {
    Log.d("Sesame", LocalServerConfig.getServerEndpoint())
}
```

### LocalHttpClient.kt (NEW)

**Location:** `sesame-sdk/src/main/java/co/candyhouse/sesame/server/LocalHttpClient.kt`

**Key Functions:**
```kotlin
suspend fun makeRequest(method, path, body, headers)
suspend fun postJson(path, body)
suspend fun getJson(path)
suspend fun putJson(path, body)
suspend fun deleteJson(path, body)
```

**Usage:**
```kotlin
// Make HTTP request
val response = LocalHttpClient.postJson("/device/list", emptyMap())

// Parse response
val devices = Gson().fromJson(response, DeviceList::class.java)
```

### CHAPIClientBiz.kt (MODIFIED)

**New Initialization:**
```kotlin
// For local server
CHAPIClientBiz.initializeLocalServer(context, "http://192.168.1.100:3000")

// For AWS (existing)
CHAPIClientBiz.initialize(context, credentialsProvider, region, apiKey)
```

**All existing methods still work:**
- `upLoadKeys()`, `putKey()`, `getDevicesList()`, etc.
- Automatically uses local server if enabled

### BaseApp.kt (MODIFIED)

**Changes:**
- Auto-detects local server mode
- Skips Firebase Crashlytics in local mode
- Has fallback logic if AWS init fails
- Logs configuration used

**Detection Logic:**
```kotlin
if (BuildConfig.USE_LOCAL_SERVER) {
    // Initialize local server
    CHAPIClientBiz.initializeLocalServer(this)
} else {
    // Initialize AWS (or fallback to local if fails)
    AWSStatus.initAWSMobileClient(this)
}
```

### SesameFirebaseMessagingService.kt (MODIFIED)

**Changes:**
- Checks if local server enabled before processing FCM
- Gracefully skips without errors
- Non-fatal in local mode

**Key Check:**
```kotlin
override fun onMessageReceived(remoteMessage) {
    if (LocalServerConfig.isEnabled()) return  // Skip FCM
    // Process Firebase message...
}
```

## 🚀 Quick Commands

### Server Operations

```bash
# Start server
cd local-server && npm start

# Different port
PORT=8080 npm start

# Docker
docker build -t sesame-server local-server
docker run -p 3000:3000 sesame-server

# Test server
curl http://localhost:3000/health
```

### Android Operations

```bash
# Build debug
./gradlew :app:assembleDebug

# Install to device
adb install app/build/outputs/apk/debug/app-debug.apk

# View logs
adb logcat | grep "LocalServer"
adb logcat | grep "CHAPIClientBiz"

# Test connectivity
adb shell curl http://192.168.1.100:3000/health
```

### API Testing

```bash
# List devices
curl http://localhost:3000/device/list \
  -H "appidentifyid: test"

# Register device
curl -X POST http://localhost:3000/device/v1/sesame5/ABC123 \
  -H "appidentifyid: test" \
  -H "Content-Type: application/json"

# Get status
curl http://localhost:3000/admin/devices

# View history
curl http://localhost:3000/admin/history/ABC123
```

## 📊 Endpoints Summary

### Core Operations
```
POST   /device/v1/iot/sesame2/:id      ← Lock/Unlock send here
POST   /device/v1/sesame5/:id          ← Register devices here
GET    /device/list                    ← List all devices
POST   /device/v1/sesame5/:id/battery  ← Battery data
```

### Admin (Debugging)
```
GET    /admin/devices                  ← See all data
GET    /admin/history/:id              ← View device history
DELETE /admin/devices/:id              ← Delete device
```

### Optional/Stub
```
POST   /friend, /device, /biometrics, /bot/script, etc.
```

## ✅ Verification Checklist

- [ ] Server starts without errors: `npm start` ✓
- [ ] Health check succeeds: `curl http://localhost:3000/health` ✓
- [ ] App builds: `./gradlew :app:assembleDebug` ✓
- [ ] App installs: `adb install ...apk` ✓
- [ ] App launches without crash ✓
- [ ] BLE pairing works ✓
- [ ] Lock/Unlock succeeds ✓
- [ ] Device registration succeeds ✓
- [ ] Server logs show requests ✓

## 🎓 Learning Path

### Beginner (Just want to use it)
1. Read: QUICK_START.md
2. Follow: Steps 1-4
3. Test: Curl/Phone
4. Done! ✓

### Intermediate (Want to understand)
1. Read: IMPLEMENTATION_SUMMARY.md
2. Read: MIGRATION_GUIDE.md
3. Skim: Code files
4. Reference: LOCAL_SERVER_CONFIG.md as needed

### Advanced (Want to extend it)
1. Study: MIGRATION_GUIDE.md - Architecture
2. Review: LocalServerConfig.kt implementation
3. Review: LocalHttpClient.kt implementation
4. Modify: local-server/server.js as needed
5. Add: Database, auth, etc. as desired

## 🆘 Help Resources

### Most Common Questions

**Q: Server won't connect?**
A: See QUICK_START.md - Common Issues section

**Q: How do I change server endpoint?**
A: See LOCAL_SERVER_CONFIG.md - any method

**Q: What about security?**
A: See LOCAL_SERVER_SETUP.md - Security section

**Q: How do I add a database?**
A: See LOCAL_SERVER_CONFIG.md - Data Persistence section

**Q: Can I use AWS and local together?**
A: Yes! See LOCAL_SERVER_CONFIG.md - all methods support toggling

### Support Contacts

- **Code Issues:** Check MIGRATION_GUIDE.md or file-specific docs
- **Setup Issues:** Check QUICK_START.md and LOCAL_SERVER_SETUP.md
- **Configuration:** See LOCAL_SERVER_CONFIG.md
- **Server Issues:** See local-server/README.md

## 📈 File Sizes (Reference)

| File | Size | Purpose |
|------|------|---------|
| QUICK_START.md | ~4KB | Quick setup |
| IMPLEMENTATION_SUMMARY.md | ~12KB | Overview |
| LOCAL_SERVER_CONFIG.md | ~16KB | Configuration |
| LOCAL_SERVER_SETUP.md | ~20KB | Complete guide |
| MIGRATION_GUIDE.md | ~18KB | Technical |
| LocalServerConfig.kt | ~2KB | Config class |
| LocalHttpClient.kt | ~3KB | HTTP client |
| server.js (full) | ~10KB | Full server |
| minimal-server.js | <1KB | Minimal |

## 🎯 Next Actions If You're...

### ... Just Starting Out
→ Go to: **QUICK_START.md**

### ... Ready to Deploy
→ Go to: **LOCAL_SERVER_SETUP.md** (Security section)

### ... Need to Customize
→ Go to: **LOCAL_SERVER_CONFIG.md**

### ... Troubleshooting an Issue
→ Go to: **QUICK_START.md** (Common Issues) or specific doc

### ... Want to Understand the Code
→ Go to: **MIGRATION_GUIDE.md**

---

**Last Updated:** May 6, 2026  
**Total Documentation:** 6 guides + 5 code examples  
**Estimated Setup Time:** 5-15 minutes  
**Ready to Use:** ✅ Yes

