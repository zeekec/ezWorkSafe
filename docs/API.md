# ezWorkSafe — API Reference

Every Android API, system service, Jetpack library, Kotlin construct, and testing framework used in this project, with links to official documentation.

---

## 1. Android System Services

### 1.1 WifiManager
| | |
|---|---|
| **Package** | `android.net.wifi.WifiManager` |
| **Acquired via** | `context.getSystemService(Context.WIFI_SERVICE)` |
| **Methods used** | `isWifiEnabled` |
| **Intent action** | `WifiManager.WIFI_STATE_CHANGED_ACTION` |
| **Used in** | `SystemSensorRepository.kt:50-72` |
| **Purpose** | Query WiFi enabled/disabled state and subscribe to state change broadcasts |
| **Docs** | https://developer.android.com/reference/android/net/wifi/WifiManager |

### 1.2 BluetoothAdapter / BluetoothManager
| | |
|---|---|
| **Package** | `android.bluetooth.BluetoothAdapter`, `android.bluetooth.BluetoothManager` |
| **Acquired via** | `context.getSystemService(Context.BLUETOOTH_SERVICE)` → `BluetoothManager.adapter` |
| **Methods used** | `BluetoothManager.adapter`, `BluetoothAdapter.isEnabled` |
| **Intent action** | `BluetoothAdapter.ACTION_STATE_CHANGED` |
| **Used in** | `SystemSensorRepository.kt:74-106` |
| **Purpose** | Query Bluetooth enabled/disabled state and subscribe to state change broadcasts |
| **Docs** | https://developer.android.com/reference/android/bluetooth/BluetoothAdapter |

### 1.3 AudioManager
| | |
|---|---|
| **Package** | `android.media.AudioManager` |
| **Acquired via** | `context.getSystemService(Context.AUDIO_SERVICE)` |
| **Methods used** | `registerAudioRecordingCallback()`, `unregisterAudioRecordingCallback()` |
| **Callback class** | `AudioManager.AudioRecordingCallback` — `onRecordingConfigChanged()` |
| **Used in** | `SystemSensorRepository.kt:123-155` |
| **Purpose** | Monitor microphone recording state via audio recording configuration callbacks (API 24+) |
| **Docs** | https://developer.android.com/reference/android/media/AudioManager |

### 1.4 CameraManager
| | |
|---|---|
| **Package** | `android.hardware.camera2.CameraManager` |
| **Acquired via** | `context.getSystemService(Context.CAMERA_SERVICE)` |
| **Properties used** | `cameraIdList` |
| **Methods used** | `registerAvailabilityCallback()`, `unregisterAvailabilityCallback()` |
| **Callback class** | `CameraManager.AvailabilityCallback` — `onCameraAvailable()`, `onCameraUnavailable()` |
| **Used in** | `SystemSensorRepository.kt:157-207` |
| **Purpose** | Monitor camera usage state (available vs. in-use) via camera2 availability callbacks |
| **Docs** | https://developer.android.com/reference/android/hardware/camera2/CameraManager |

---

## 2. Android Framework — Core

### 2.1 Application & Context
| API | Class/File | Purpose | Docs |
|-----|-----------|---------|------|
| `android.app.Application` | `EzWorkSafeApp.kt` | Custom Application subclass; initializes `sensorRepository` in `onCreate()` | https://developer.android.com/reference/android/app/Application |
| `android.content.Context` | Throughout | Access system services, register receivers, check permissions | https://developer.android.com/reference/android/content/Context |
| `android.os.Bundle` | `MainActivity.kt:26` | Activity `onCreate(Bundle?)` parameter for saved instance state | https://developer.android.com/reference/android/os/Bundle |
| `android.content.Intent` | `MainActivity.kt:58`, `MonitoringService.kt:49` | Service launch intents, broadcast `onReceive()` parameter | https://developer.android.com/reference/android/content/Intent |

### 2.2 BroadcastReceiver
| | |
|---|---|
| **Package** | `android.content.BroadcastReceiver` |
| **Method** | `onReceive(context: Context, intent: Intent)` |
| **Paired with** | `android.content.IntentFilter` |
| **Registered via** | `context.registerReceiver()` / `context.unregisterReceiver()` |
| **Intents received** | `WifiManager.WIFI_STATE_CHANGED_ACTION`, `BluetoothAdapter.ACTION_STATE_CHANGED` |
| **Used in** | `SystemSensorRepository.kt:64-69`, `SystemSensorRepository.kt:97-103` |
| **Purpose** | Receive system-level broadcasts when WiFi or Bluetooth state changes |
| **Docs** | https://developer.android.com/reference/android/content/BroadcastReceiver |

### 2.3 Service
| | |
|---|---|
| **Package** | `android.app.Service` |
| **Methods** | `onCreate()`, `onStartCommand()`, `onDestroy()`, `onBind()` |
| **Return constant** | `START_STICKY` |
| **Used in** | `MonitoringService.kt` |
| **Purpose** | Base class for foreground service that runs background sensor monitoring with a persistent notification |
| **Docs** | https://developer.android.com/reference/android/app/Service |

### 2.4 Notification APIs
| API | Class | Purpose | Docs |
|-----|-------|---------|------|
| `android.app.Notification` | `MonitoringService.kt:128-145` | Foreground notification object | https://developer.android.com/reference/android/app/Notification |
| `android.app.NotificationChannel` | `MonitoringService.kt:147-157` | Notification channel (required API 26+) | https://developer.android.com/reference/android/app/NotificationChannel |
| `android.app.NotificationManager` | `MonitoringService.kt:156-157` | System service for channel creation | https://developer.android.com/reference/android/app/NotificationManager |
| `androidx.core.app.NotificationCompat` | `MonitoringService.kt:136-144` | Builder for notifications with backward compatibility | https://developer.android.com/reference/androidx/core/app/NotificationCompat |
| `NotificationCompat.BigTextStyle` | `MonitoringService.kt:137` | Expandable notification style with full summary text | https://developer.android.com/reference/androidx/core/app/NotificationCompat.BigTextStyle |

### 2.5 PackageManager / Permissions
| API | Used in | Purpose | Docs |
|-----|---------|---------|------|
| `android.content.pm.PackageManager` | `PermissionHelper.kt:17` | `PERMISSION_GRANTED` constant | https://developer.android.com/reference/android/content/pm/PackageManager |
| `android.Manifest.permission` | `PermissionHelper.kt:11-12` | Permission name constants (`CAMERA`, `RECORD_AUDIO`) | https://developer.android.com/reference/android/Manifest.permission |
| `androidx.core.content.ContextCompat.checkSelfPermission()` | `PermissionHelper.kt:17` | Runtime permission check (backward compatible) | https://developer.android.com/reference/androidx/core/content/ContextCompat |

### 2.6 Build / OS Version Check
| API | Used in | Purpose | Docs |
|-----|---------|---------|------|
| `android.os.Build.VERSION.SDK_INT` | `EzWorkSafeTheme.kt:27`, `MonitoringService.kt:77` | Check running API level | https://developer.android.com/reference/android/os/Build.VERSION#SDK_INT |
| `Build.VERSION_CODES.S` (31) | `EzWorkSafeTheme.kt:27` | Dynamic color support gate | https://developer.android.com/reference/android/os/Build.VERSION_CODES#S |
| `Build.VERSION_CODES.O` (26) | `MonitoringService.kt:77` | Notification channel gate | https://developer.android.com/reference/android/os/Build.VERSION_CODES#O |

### 2.7 Activity Lifecycle
| API | Used in | Purpose | Docs |
|-----|---------|---------|------|
| `androidx.lifecycle.Lifecycle` / `LifecycleEventObserver` | `MainActivity.kt:28-32` | Observe activity resume events to refresh sensor flows | https://developer.android.com/reference/androidx/lifecycle/Lifecycle |

---

## 3. AndroidX / Jetpack Libraries

### 3.1 Activity Compose (`androidx.activity:activity-compose:1.8.2`)
| API | Used in | Purpose |
|-----|---------|---------|
| `androidx.activity.ComponentActivity` | `MainActivity.kt:18` | Base activity with Compose support |
| `setContent {}` | `MainActivity.kt:37` | Sets the Compose UI content |
| `registerForActivityResult()` | `MainActivity.kt:20` | Register a contract-based activity result launcher |
| `ActivityResultContracts.RequestMultiplePermissions()` | `MainActivity.kt:21` | Contract for requesting multiple runtime permissions |
| **Docs** | https://developer.android.com/jetpack/androidx/releases/activity | |

### 3.2 Compose UI (`androidx.compose.ui:ui` via BOM `2024.01.00`)
| APIs used | `Modifier`, `fillMaxSize`, `background`, `padding`, `size`, `width`, `weight`, `clip`, `draw.clip`, `graphics.Color`, `foundation.background`, `foundation.shape.CircleShape`, `foundation.shape.RoundedCornerShape`, `foundation.layout.*` |
|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Used in** | `StatusDashboard.kt` |
| **Purpose** | Core Compose UI toolkit for layout, drawing, and input |
| **Docs** | https://developer.android.com/jetpack/compose |

### 3.3 Compose Material3 (`androidx.compose.material3:material3` via BOM)
| APIs used | `MaterialTheme`, `Card`, `CardDefaults`, `Surface`, `Text`, `lightColorScheme`, `darkColorScheme`, `dynamicDarkColorScheme`, `dynamicLightColorScheme` |
|-----------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Used in** | `StatusDashboard.kt`, `EzWorkSafeTheme.kt` |
| **Purpose** | Material Design 3 components: cards, surfaces, typography, dynamic color (Android 12+) |
| **Docs** | https://developer.android.com/jetpack/compose/material3 |

### 3.4 Lifecycle (`androidx.lifecycle:lifecycle-*-2.7.0`)
| Artifacts | `lifecycle-runtime-ktx:2.7.0`, `lifecycle-viewmodel-compose:2.7.0`, `lifecycle-runtime-compose:2.7.0` |
|-----------|-------------------------------------------------------------------------------------------------------|
| **APIs used** | `AndroidViewModel`, `viewModelScope`, `viewModel()` (compose function), `ViewModelProvider` |
| **Used in** | `SensorViewModel.kt`, `MainActivity.kt` |
| **Purpose** | ViewModel with lifecycle-scoped coroutines, Compose integration for ViewModel resolution |
| **Docs** | https://developer.android.com/jetpack/androidx/releases/lifecycle |

### 3.5 Core KTX (`androidx.core:core-ktx:1.12.0`)
| API | Used in | Purpose |
|-----|---------|---------|
| `ContextCompat.startForegroundService()` | `MainActivity.kt:60` | Start foreground service with backward compatibility |
| `ContextCompat.checkSelfPermission()` | `PermissionHelper.kt:17`, `SystemSensorRepository.kt:132,166` | Runtime permission check |
| **Docs** | https://developer.android.com/kotlin/ktx | |

### 3.6 Compose UI Test (`androidx.compose.ui:ui-test-junit4`)
| APIs used | `createAndroidComposeRule()`, `createComposeRule()`, `onNodeWithText()`, `onNode()`, `assertIsDisplayed()`, `assertExists()`, `waitUntil()`, `waitForIdle()` |
|-----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Used in** | `StatusDashboardE2eTest.kt`, `PermissionRefreshE2eTest.kt`, `EzWorkSafeThemeTest.kt` |
| **Purpose** | Instrumented UI testing for Compose screens |
| **Docs** | https://developer.android.com/jetpack/compose/testing |

### 3.7 AndroidX Test (`androidx.test.ext:junit:1.2.1`, `androidx.test:rules:1.6.1`)
| APIs used | `GrantPermissionRule`, `AndroidJUnit4` runner, `@SmallTest` |
|-----------|------------------------------------------------------------|
| **Used in** | `StatusDashboardE2eTest.kt`, `PermissionRefreshE2eTest.kt`, `EzWorkSafeThemeTest.kt` |
| **Purpose** | AndroidX test infrastructure: permission grants, test runners |
| **Docs** | https://developer.android.com/training/testing/junit-runner |

### 3.8 AndroidX Arch Core Testing (`androidx.arch.core:core-testing:2.2.0`)
| API | `InstantTaskExecutorRule` |
|-----|--------------------------|
| **Used in** | `SensorViewModelTest.kt`, `SensorRepositoryTest.kt`, `PermissionHelperTest.kt` |
| **Purpose** | Synchronous task execution for ViewModel/repository unit tests (avoids async timing issues) |
| **Docs** | https://developer.android.com/reference/androidx/arch/core/executor/testing/InstantTaskExecutorRule |

---

## 4. Kotlin Coroutines & Flow

### 4.1 Kotlinx Coroutines (`org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3`)

| API | Used in | Purpose |
|-----|---------|---------|
| `kotlinx.coroutines.flow.callbackFlow` | `SystemSensorRepository.kt:50,74,123,157` | Create a `Flow` from callback-based APIs (BroadcastReceiver, AudioManager callback, CameraManager callback) |
| `kotlinx.coroutines.channels.awaitClose` | `SystemSensorRepository.kt:71,105,152,204` | Suspend until flow collection is cancelled, then run cleanup (unregister receiver/callback) |
| `kotlinx.coroutines.flow.flatMapLatest` | `SystemSensorRepository.kt:40` | Restart the sensor observation flow when `refreshTrigger` emits a new value |
| `kotlinx.coroutines.flow.stateIn` | `SensorViewModel.kt:21-33` | Convert cold `Flow<SensorStatus>` into hot `StateFlow` scoped to `viewModelScope` |
| `kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000)` | `SensorViewModel.kt:21-33` | Keep upstream flow alive for 5s after last subscriber (survives rotation) |
| `kotlinx.coroutines.flow.MutableStateFlow` | `SystemSensorRepository.kt:31` | Observable state holder for the refresh trigger counter |
| `kotlinx.coroutines.flow.combine` | `MonitoringService.kt:63` | Merge four sensor flows into a single notification text string |
| `kotlinx.coroutines.CoroutineScope` | `MonitoringService.kt:40` | Custom scope for foreground service coroutines |
| `kotlinx.coroutines.SupervisorJob` | `MonitoringService.kt:40` | Job that allows child coroutines to fail independently |
| `kotlinx.coroutines.Dispatchers.Main` | `MonitoringService.kt:40` | Main-thread dispatcher for UI-bound operations |
| `kotlinx.coroutines.ExperimentalCoroutinesApi` | `SystemSensorRepository.kt:22` | Opt-in for `flatMapLatest` (experimental in 1.7.x) |

**Docs:**
- Coroutines guide: https://kotlinlang.org/docs/coroutines-overview.html
- Flow: https://kotlinlang.org/docs/flow.html
- callbackFlow: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/callback-flow.html
- stateIn: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/state-in.html

### 4.2 Kotlinx Coroutines Test (`org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3`)
| API | Used in | Purpose |
|-----|---------|---------|
| `runTest` | `SensorViewModelTest.kt`, `SensorRepositoryTest.kt` | Scope for testing suspending functions with virtual time |
| `kotlinx.coroutines.flow.first` | `SensorRepositoryTest.kt` | Collect the first emission from a Flow in tests |
| **Docs** | https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/ | |

---

## 5. Android Permissions

### Declared in AndroidManifest.xml

| Permission | Type | API Level | Purpose |
|-----------|------|-----------|---------|
| `android.permission.ACCESS_WIFI_STATE` | Normal | All | Read WiFi radio state (enabled/disabled) |
| `android.permission.BLUETOOTH` | Normal | All | Legacy Bluetooth state access (pre-API 31) |
| `android.permission.BLUETOOTH_CONNECT` | Dangerous | 31+ | Bluetooth state access on modern Android |
| `android.permission.RECORD_AUDIO` | Dangerous | All | Monitor microphone recording state (runtime prompt) |
| `android.permission.CAMERA` | Dangerous | All | Monitor camera usage state (runtime prompt) |
| `android.permission.FOREGROUND_SERVICE` | Normal | 28+ | Run a foreground service |
| `android.permission.FOREGROUND_SERVICE_DATA_SYNC` | Normal | 34+ | Declare `dataSync` foreground service type |

### Runtime Permissions (CAMERA + RECORD_AUDIO)
- Requested in `MainActivity.kt:52-55` via `ActivityResultContracts.RequestMultiplePermissions`
- Checked per-sensor in `SystemSensorRepository.kt:132,166` — emits `SensorStatus.Denied` if not granted
- Utility wrapper: `PermissionHelper.kt`

**Docs:** https://developer.android.com/training/permissions/requesting

---

## 6. Android Manifest Declarations

| Element | Value | Purpose |
|---------|-------|---------|
| `android:name=".EzWorkSafeApp"` | Application subclass | Initializes `sensorRepository` on startup |
| `android:name=".ui.view.MainActivity"` | Activity | Launcher activity with `MAIN`/`LAUNCHER` intent filter |
| `android:name=".service.MonitoringService"` | Service | Foreground service, `foregroundServiceType="dataSync"`, not exported |
| `android.hardware.camera` (required=false) | Feature | Camera hardware optional — app works without it |
| `android.hardware.microphone` (required=false) | Feature | Microphone hardware optional — app works without it |

**Docs:** https://developer.android.com/guide/topics/manifest/manifest-intro

---

## 7. Gradle Build Configuration

| Plugin | Version | Purpose |
|--------|---------|---------|
| `com.android.application` | 9.2.1 | Android app build (AGP — Android Gradle Plugin) |
| `org.jetbrains.kotlin.plugin.compose` | 2.2.10 | Kotlin Compose compiler plugin (replaces `composeOptions { kotlinCompilerExtensionVersion }`) |

| Config | Value |
|--------|-------|
| `compileSdk` | 34 |
| `minSdk` | 26 |
| `targetSdk` | 33 |
| `namespace` / `applicationId` | `com.ezworksafe` |
| Java compatibility | `VERSION_17` |

**Docs:**
- AGP: https://developer.android.com/build/releases/gradle-plugin
- Kotlin Compose compiler: https://android.googlesource.com/platform/tools/kotlin/+/refs/heads/main/plugins/compose/ReadMe.md

---

## 8. Testing Libraries

### 8.1 JUnit 4 (`junit:junit:4.13.2`)
| APIs used | `@Test`, `@Before`, `@BeforeClass`, `@Ignore`, `Assert.*`, `@Rule` |
|-----------|--------------------------------------------------------------------|
| **Used in** | All test files |
| **Purpose** | Unit test framework — test structure, assertions, rules |
| **Docs** | https://junit.org/junit4/ |

### 8.2 Mockito (`mockito-core:5.7.0`, `mockito-kotlin:5.1.0`)
| APIs used | `mock()`, `doReturn`, `on { }` (Mockito Kotlin DSL) |
|-----------|------------------------------------------------------|
| **Used in** | `SensorViewModelTest.kt`, `SensorRepositoryTest.kt` |
| **Purpose** | Create mock objects for `Context`, `Application`, and system service dependencies |
| **Docs** | https://site.mockito.org/ |

### 8.3 Espresso (`androidx.test.espresso:espresso-core:3.7.0`)
| Used in | build.gradle.kts (declared dependency for instrumented tests) |
|---------|---------------------------------------------------------------|
| **Purpose** | UI interaction assertions for instrumented tests |
| **Docs** | https://developer.android.com/training/testing/espresso |

### 8.4 Compose UI Test Manifest (`androidx.compose.ui:ui-test-manifest`)
| Purpose | Debug-only dependency that enables `createAndroidComposeRule()` for instrumented Compose tests |
|---------|----------------------------------------------------------------------------------------------|
| **Docs** | https://developer.android.com/jetpack/compose/testing |

---

## 9. Domain Model APIs

### 9.1 SensorStatus (sealed class)
| State | Meaning | Color |
|-------|---------|-------|
| `Active` | Sensor is enabled or resource is in use | Green `0xFF4CAF50` |
| `Inactive` | Sensor is disabled or resource is idle | Gray `0xFF9E9E9E` |
| `Denied` | Runtime permission not granted | Red `0xFFF44336` |
| `Unavailable` | Hardware missing, service null, or API level too low | Dark gray `0xFF616161` |

**File:** `data/model/SensorStatus.kt:9-18`

### 9.2 SensorType (enum)
| Value | Display Name |
|-------|-------------|
| `WIFI` | "WiFi" |
| `BLUETOOTH` | "Bluetooth" |
| `MICROPHONE` | "Microphone" |
| `CAMERA` | "Camera" |

**File:** `data/model/SensorStatus.kt:20-25`

### 9.3 SensorRepository (interface)
```kotlin
interface SensorRepository {
    fun observeSensor(type: SensorType): Flow<SensorStatus>
    fun refresh()
}
```
- **Implementation:** `SystemSensorRepository` — wraps system services via `callbackFlow`
- **refresh():** Increments `refreshTrigger` → `flatMapLatest` restarts all sensor flows
- **File:** `data/repository/SensorRepository.kt`

---

## 10. Intent Actions

| Action Constant | Used In | Purpose |
|----------------|---------|---------|
| `WifiManager.WIFI_STATE_CHANGED_ACTION` | `SystemSensorRepository.kt:69` | WiFi radio on/off broadcasts |
| `BluetoothAdapter.ACTION_STATE_CHANGED` | `SystemSensorRepository.kt:102` | Bluetooth radio on/off broadcasts |
| `android.intent.action.MAIN` | `AndroidManifest.xml:38` | Main activity entry point |
| `android.intent.category.LAUNCHER` | `AndroidManifest.xml:39` | Launcher category for home screen icon |

---

## 11. Theme / Style / Resource APIs

| Resource | File | Purpose |
|----------|------|---------|
| `Theme.EzWorkSafe` | `res/values/themes.xml` | Base theme extending `Theme.Material.Light.NoActionBar` |
| Status colors | `data/model/SensorStatus.kt` | Color tokens baked into sealed class (`Active`, `Inactive`, `Denied`, `Unavailable`) |
| Dynamic color | `EzWorkSafeTheme.kt:27-29` | Android 12+ dynamic color via `dynamicLightColorScheme` / `dynamicDarkColorScheme` |
| `MaterialTheme` | `EzWorkSafeTheme.kt:34` | Compose Material3 theme wrapper |
| **Compose Docs** | https://developer.android.com/jetpack/compose/theming | |
| **Material3 Theming** | https://developer.android.com/jetpack/compose/material3/customizing-themes | |
