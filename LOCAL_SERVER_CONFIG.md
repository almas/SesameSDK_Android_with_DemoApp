# Configuration Guide for Local Server Mode

## BuildConfig Configuration

Add these build configuration options to your `build.gradle`:

```gradle
buildTypes {
    debug {
        // Enable local server mode in debug builds
        buildConfigField "boolean", "USE_LOCAL_SERVER", "true"
        buildConfigField "String", "LOCAL_SERVER_ENDPOINT", "\"http://192.168.1.100:3000\""
    }
    
    release {
        // Can use AWS for release, or set to local
        buildConfigField "boolean", "USE_LOCAL_SERVER", "false"
        buildConfigField "String", "LOCAL_SERVER_ENDPOINT", "\"http://api.example.com\""
    }
}
```

## Usage in Code

### Option 1: Automatic Initialization (Recommended)

Modify `BaseApp.kt` to use BuildConfig:

```kotlin
private fun initializeAWS() {
    if (BuildConfig.USE_LOCAL_SERVER) {
        CHAPIClientBiz.initializeLocalServer(this, BuildConfig.LOCAL_SERVER_ENDPOINT)
    } else {
        AWSStatus.initAWSMobileClient(this)
        setCHAPIClient()
    }
}
```

### Option 2: Manual Control at Runtime

Allow users to switch modes:

```kotlin
class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
        
        // Listen for local server preference changes
        findPreference<SwitchPreferenceCompat>("enable_local_server")?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean) {
                val endpoint = findPreference<EditTextPreference>("server_endpoint")?.value
                    ?: "http://192.168.1.100:3000"
                CHAPIClientBiz.initializeLocalServer(requireContext(), endpoint)
            } else {
                // Switch back to AWS
                AWSStatus.initAWSMobileClient(requireContext())
                CHAPIClientBiz.initialize(requireContext(), ...)
            }
            true
        }
    }
}
```

### Option 3: Environment-based Configuration

Create a `LocalConfig` system:

```kotlin
object LocalConfig {
    fun isLocalMode(context: Context): Boolean {
        return when {
            BuildConfig.DEBUG -> {
                // Check SharedPreferences override
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                prefs.getBoolean("force_aws_mode", false).not()
            }
            else -> BuildConfig.USE_LOCAL_SERVER
        }
    }
    
    fun getServerEndpoint(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString("custom_server_endpoint", null) 
            ?: BuildConfig.LOCAL_SERVER_ENDPOINT
    }
}
```

## Gradle Commands

### Build Debug with Local Server

```bash
./gradlew :app:assembleDebug
```

### Build Release with AWS

```bash
./gradlew :app:assembleRelease
```

### Build with Custom Configuration

```bash
./gradlew :app:assembleDebug \
  -PuseLocalServer=true \
  -PlocalServerEndpoint="http://10.0.0.5:3000"
```

## Android.xml Configuration

Create settings file at `app/src/main/res/xml/settings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<PreferenceScreen xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">

    <PreferenceCategory android:title="Server Settings">
        
        <SwitchPreferenceCompat
            android:key="enable_local_server"
            android:title="Local Server Mode"
            android:summary="Use local HTTP server instead of AWS"
            android:defaultValue="true"
            app:iconSpaceReserved="false" />

        <EditTextPreference
            android:key="server_endpoint"
            android:title="Server Endpoint"
            android:summary="e.g., http://192.168.1.100:3000"
            android:defaultValue="http://192.168.1.100:3000"
            android:dependency="enable_local_server"
            app:iconSpaceReserved="false" />

        <EditTextPreference
            android:key="server_port"
            android:title="Server Port"
            android:defaultValue="3000"
            android:inputType="number"
            android:dependency="enable_local_server"
            app:iconSpaceReserved="false" />

    </PreferenceCategory>

    <PreferenceCategory android:title="Advanced">
        
        <SwitchPreferenceCompat
            android:key="disable_firebase"
            android:title="Disable Firebase"
            android:summary="Disable FCM and Crashlytics"
            android:defaultValue="true"
            app:iconSpaceReserved="false" />
            
        <SwitchPreferenceCompat
            android:key="enable_iot_connection"
            android:title="Enable IoT Connection"
            android:summary="Connect to AWS IoT Core (requires credentials)"
            android:defaultValue="false"
            app:iconSpaceReserved="false" />

    </PreferenceCategory>

</PreferenceScreen>
```

## Flavor-based Configuration

Use product flavors for different environments:

```gradle
flavorDimensions "environment"

productFlavors {
    dev {
        dimension "environment"
        buildConfigField "boolean", "USE_LOCAL_SERVER", "true"
        buildConfigField "String", "LOCAL_SERVER_ENDPOINT", "\"http://192.168.1.100:3000\""
    }
    
    staging {
        dimension "environment"
        buildConfigField "boolean", "USE_LOCAL_SERVER", "false"
        buildConfigField "String", "LOCAL_SERVER_ENDPOINT", "\"https://staging-api.example.com\""
    }
    
    production {
        dimension "environment"
        buildConfigField "boolean", "USE_LOCAL_SERVER", "false"
        buildConfigField "String", "LOCAL_SERVER_ENDPOINT", "\"https://api.example.com\""
    }
}
```

Build commands:

```bash
./gradlew :app:assembleDevDebug        # Local server
./gradlew :app:assembleStagingRelease  # Staging API
./gradlew :app:assembleProductionRelease # Production API
```

## ProGuard/R8 Configuration

If using ProGuard, add keep rules for local server classes:

```proguard
# Keep local server classes
-keep class co.candyhouse.sesame.server.LocalServerConfig { *; }
-keep class co.candyhouse.sesame.server.LocalHttpClient { *; }
-keep class co.candyhouse.sesame.server.LocalServerConfig** { *; }

# Keep if Firebase is optional
-dontwarn com.google.firebase.**
```

## Testing Configuration

For unit tests, mock the local server:

```kotlin
@Before
fun setup() {
    // Mock local server for tests
    MockWebServer webServer = new MockWebServer()
    webServer.start(3000)
    
    // Configure app to use mock server
    CHAPIClientBiz.initializeLocalServer(context, "http://localhost:3000")
}

@Test
fun testDeviceRegistration() {
    webServer.enqueue(new MockResponse().setBody("{\"success\": true}"))
    
    // Test device registration...
}
```

## Debugging Configuration

Add logging configuration:

```kotlin
// In BaseApp.kt onCreate()
if (BuildConfig.DEBUG && LocalServerConfig.isEnabled()) {
    Log.d("LocalServer", "Server Mode: ${LocalServerConfig.getServerEndpoint()}")
    Log.d("LocalServer", "Available endpoints: /device, /device/list, /health")
}
```

Enable verbose logging:

```bash
# Show all SDK logs
adb logcat | grep -E "(SESAME|LOCAL|CHAPIClient)"

# Show just local server errors
adb logcat | grep -E "ERROR.*LOCAL"
```

## Network Configuration

### For Android Emulator

If running server on host machine:

```kotlin
// Use 10.0.2.2 instead of localhost
val endpoint = if (BuildConfig.DEBUG) {
    "http://10.0.2.2:3000"  // Emulator host access
} else {
    "http://192.168.1.100:3000"  // Real device on LAN
}
```

### For Real Device

Ensure both app and server are on same network:

```bash
# Check server is accessible from device
adb shell curl http://192.168.1.100:3000/health

# Example response
# {"status":"ok"}
```

## Network Security Configuration

Create `res/xml/network_security_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">192.168.1.100</domain>
        <domain includeSubdomains="true">localhost</domain>
        <domain includeSubdomains="true">10.0.0.0/8</domain>
    </domain-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">example.com</domain>
    </domain-config>
</network-security-config>
```

Reference in `AndroidManifest.xml`:

```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
</application>
```

## Summary

Choose the configuration approach that fits your needs:

| Approach | Use Case | Flexibility |
|----------|----------|------------|
| BuildConfig | Development/Testing | Low |
| SharedPreferences | User controllable | High  |
| Product Flavors | Multiple environments | Medium |
| Runtime Detection | Complex logic | High |

For most development: **Use BuildConfig + override via SharedPreferences** for maximum simplicity with flexibility.

