# ezWorkSafe Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Native Kotlin Android app (MVVM) that displays real-time status of WiFi, Bluetooth, Microphone access, and Camera access for work safety/privacy monitoring.

**Architecture:** Single-Activity MVVM app with a `SensorRepository` wrapping Android system services via `callbackFlow` for reactive updates, a `SensorViewModel` exposing `StateFlow<SensorStatus>` per sensor, and a Jetpack Compose dashboard UI. Runtime permission requests for MIC and CAMERA. Optional foreground service for background monitoring.

**Tech Stack:** Kotlin, Coroutines + Flow, AndroidX Lifecycle (ViewModel + StateFlow), Jetpack Compose, Jetpack Glance (widget), JUnit + Mockito (testing), Gradle Kotlin DSL.

---

## Post-Plan Additions

The following were implemented beyond the original plan:

| Feature | Details |
|---------|---------|
| **Home screen widget** | 1×4 Jetpack Glance widget (`widget/`) with dual update path: Glance for initial render, RemoteViews push from service for real-time updates. |
| **Widget split sections** | Left (WiFi/BT, real-time broadcasts) and right (Mic/Cam+timestamp, foreground-refreshed) to work around Android 16 AppOps background restriction. |
| **Foreground service enhancement** | `MonitoringService` now hosts a `combine` collector that aggregates all 4 sensor flows, pushes to `WidgetState`, and updates the notification text in real time. |
| **SensorRepository refactor** | Changed from class to `interface` + `SystemSensorRepository` implementation, with `refreshTrigger` + `flatMapLatest` for permission re-checks on resume. |
| **`SensorStatus.Blocked` state** | Added for WiFI/BT hardware-off status (distinct from Inactive). Color: orange `0xFFFF9800`. |
| **App icon** | Custom adaptive icon (shield + eye) with PNG fallbacks. |
| **Dark mode** | Material You dynamic colors on API 31+, green-seeded fallback. |
| **Notification "Refresh" action** | Notification includes a button that opens MainActivity → triggers `refreshSensorFlows()`. |
| **E2E tests** | Compose UI tests for dashboard (all sensor states), widget provider metadata, notification verification via `dumpsys`, and theme. |
| **`FakeSensorRepository`** | Deterministic test double with `setStatus()` for E2E and unit tests. |
| **Configuration cache** | `org.gradle.configuration-cache=true` — builds complete in <1s on cache hit. |

### Current SDK Versions (from `app/build.gradle.kts`)

| Config | Value |
|--------|-------|
| `compileSdk` | 34 (needed for Glance 1.1.1) |
| `minSdk` | 26 |
| `targetSdk` | 33 |

### Key File Additions Not in Original Structure

```
app/src/main/java/com/ezworksafe/
└── widget/
    ├── SensorWidget.kt           # GlanceAppWidget composable layout
    ├── SensorWidgetReceiver.kt   # GlanceAppWidgetReceiver + service starter
    └── WidgetState.kt            # Singleton for cross-component state sharing
```

```
app/src/androidTest/
└── java/com/ezworksafe/
    ├── service/
    │   └── MonitoringServiceNotificationE2eTest.kt
    ├── ui/view/
    │   ├── StatusDashboardE2eTest.kt
    │   ├── PermissionRefreshE2eTest.kt   # @Ignored (API 36 shell restriction)
    │   └── EzWorkSafeThemeTest.kt
    ├── data/repository/
    │   └── FakeSensorRepository.kt
    └── widget/
        └── SensorWidgetE2eTest.kt
```

### Known Limitation
`AppOpsManager.checkOpNoThrow()` returns `MODE_IGNORED` for background processes on Android 16 regardless of actual toggle state (server-side enforcement). No client-side workaround exists. `SensorPrivacyManager` is `@SystemApi`. This is why Mic/Cam show stale state in the widget's right section until the user opens the app.

---

---

## File Structure

```
ezWorkSafe/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/ezworksafe/
│           │   ├── EzWorkSafeApp.kt
│           │   ├── data/
│           │   │   ├── model/
│           │   │   │   └── SensorStatus.kt
│           │   │   └── repository/
│           │   │       ├── SensorRepository.kt        # Interface
│           │   │       └── SystemSensorRepository.kt  # Implementation
│           │   ├── ui/
│           │   │   ├── viewmodel/
│           │   │   │   └── SensorViewModel.kt
│           │   │   └── view/
│           │   │       ├── MainActivity.kt
│           │   │       ├── StatusDashboard.kt
│           │   │       └── EzWorkSafeTheme.kt
│           │   ├── service/
│           │   │   └── MonitoringService.kt
│           │   ├── widget/
│           │   │   ├── SensorWidget.kt
│           │   │   ├── SensorWidgetReceiver.kt
│           │   │   └── WidgetState.kt
│           │   └── util/
│           │       └── PermissionHelper.kt
│           ├── res/
│           │   ├── values/
│           │   │   ├── strings.xml
│           │   │   ├── colors.xml
│           │   │   └── themes.xml
│           │   ├── layout/
│           │   │   └── widget_sensor_status.xml
│           │   └── xml/
│           │       └── widget_info_sensor.xml
│           └── drawable/ (auto-generated icons)
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── AGENTS.md
├── docs/
│   ├── PLAN.md
│   ├── API.md
│   ├── review.md
│   ├── todo.md
│   └── superpowers/
└── .github/workflows/android.yml
```

**File Responsibilities:**

| File | Responsibility |
|------|---------------|
| `settings.gradle.kts` | Root project name, module includes |
| `build.gradle.kts` (root) | Plugin declarations (AGP, Kotlin, Compose compiler) |
| `app/build.gradle.kts` | Module config: SDK versions (34/33/26), dependencies, Compose config, Glance |
| `gradle.properties` | Kotlin/JVM flags, configuration cache, Compose compiler |
| `AndroidManifest.xml` | Permissions, Activity/Service declarations, Application class, widget receiver, feature flags |
| `EzWorkSafeApp.kt` | Application subclass with lateinit `sensorRepository` |
| `SensorStatus.kt` | Sealed class: `Active`, `Inactive`, `Denied`, `Blocked`, `Unavailable` states |
| `SensorType.kt` | Enum: `WIFI`, `BLUETOOTH`, `MICROPHONE`, `CAMERA` |
| `SensorRepository.kt` | Interface: `observeSensor()`, `refresh()` |
| `SystemSensorRepository.kt` | Wraps 4 system services → `callbackFlow<SensorStatus>` per sensor, with `refreshTrigger` + `flatMapLatest` |
| `PermissionHelper.kt` | Runtime permission check + request launcher for CAMERA + RECORD_AUDIO |
| `SensorViewModel.kt` | Exposes `StateFlow<SensorStatus>` x4, lifecycle-scoped collection, `refresh()` delegation |
| `MainActivity.kt` | Single Activity host, permission orchestration, ViewModel wiring, service startup, lifecycle observer for refresh |
| `StatusDashboard.kt` | Compose UI: 4 status indicator cards with color-coded state |
| `EzWorkSafeTheme.kt` | Compose theme with Material You dynamic colors (API 31+) and green-seeded fallback |
| `MonitoringService.kt` | Foreground service with `combine` collector → `WidgetState` push → notification update |
| `SensorWidget.kt` | GlanceAppWidget composable, 1×4 layout with split sections |
| `SensorWidgetReceiver.kt` | GlanceAppWidgetReceiver, starts service on widget update |
| `WidgetState.kt` | Singleton: `statuses` map + `lastRefreshTime`, shared between service and Glance |
| `widget_sensor_status.xml` | RemoteViews layout for widget (left section + divider + right section) |
| `widget_info_sensor.xml` | Widget metadata (250×40dp min size, horizontal resize) |
| `strings.xml` | UI strings |
| `colors.xml` | Status color tokens (currently unused — colors sourced from `SensorStatus`) |
| `themes.xml` | Base Material theme |

---

## Task 1: Gradle Project Scaffolding

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `app/build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/wrapper/gradle-wrapper.properties`

- [ ] **Step 1.1: Write settings.gradle.kts**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ezWorkSafe"
include(":app")
```

- [ ] **Step 1.2: Write root build.gradle.kts**

```kotlin
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
}
```

- [ ] **Step 1.3: Write app/build.gradle.kts**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ezworksafe"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ezworksafe"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
}
```

- [ ] **Step 1.4: Write gradle.properties**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 1.5: Write gradle-wrapper.properties**

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

- [ ] **Step 1.6: Create directory structure**

Run: `mkdir -p app/src/main/java/com/ezworksafe/{data/{model,repository},ui/{viewmodel,view},service,util} app/src/main/res/values`

- [ ] **Step 1.7: Verify project syncs**

Run: `./gradlew projects`
Expected: no errors, lists `:app` project

- [ ] **Step 1.8: Commit**

```bash
git add settings.gradle.kts build.gradle.kts app/build.gradle.kts gradle.properties gradle/wrapper/gradle-wrapper.properties app/src/
git commit -m "chore: scaffold Android project with Compose, Coroutines, Lifecycle deps"
```

---

## Task 2: Android Manifest & Application Class

**Files:**
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/ezworksafe/EzWorkSafeApp.kt`
- Create: `app/src/main/res/values/strings.xml`

- [ ] **Step 2.1: Write AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Permissions (no runtime: wifi, bluetooth) -->
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

    <!-- Permissions (runtime required: mic, camera) -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.CAMERA" />

    <application
        android:name=".EzWorkSafeApp"
        android:allowBackup="true"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.EzWorkSafe"
        tools:targetApi="34">

        <activity
            android:name=".ui.view.MainActivity"
            android:exported="true"
            android:theme="@style/Theme.EzWorkSafe">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Foreground service (uncomment when Phase 8 is implemented)
        <service
            android:name=".service.MonitoringService"
            android:foregroundServiceType="dataSync"
            android:exported="false" />
        -->

    </application>
</manifest>
```

- [ ] **Step 2.2: Write Application class**

```kotlin
package com.ezworksafe

import android.app.Application

class EzWorkSafeApp : Application()
```

- [ ] **Step 2.3: Write strings.xml**

```xml
<resources>
    <string name="app_name">ezWorkSafe</string>
</resources>
```

- [ ] **Step 2.4: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2.5: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/com/ezworksafe/EzWorkSafeApp.kt app/src/main/res/values/strings.xml
git commit -m "feat: add manifest with permissions and Application class"
```

---

## Task 3: Data Model — SensorStatus

**Files:**
- Create: `app/src/main/java/com/ezworksafe/data/model/SensorStatus.kt`
- Create: `app/src/test/java/com/ezworksafe/data/model/SensorStatusTest.kt`

- [ ] **Step 3.1: Write the failing test**

```kotlin
package com.ezworksafe.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SensorStatusTest {

    @Test
    fun `active displays green label`() {
        assertEquals("Active", SensorStatus.Active.label)
        assertEquals(0xFF4CAF50L, SensorStatus.Active.color)
    }

    @Test
    fun `inactive displays gray label`() {
        assertEquals("Inactive", SensorStatus.Inactive.label)
        assertEquals(0xFF9E9E9EL, SensorStatus.Inactive.color)
    }

    @Test
    fun `denied displays red label`() {
        assertEquals("Denied", SensorStatus.Denied.label)
        assertEquals(0xFFF44336L, SensorStatus.Denied.color)
    }

    @Test
    fun `unavailable displays dark gray label`() {
        assertEquals("Unavailable", SensorStatus.Unavailable.label)
        assertEquals(0xFF616161L, SensorStatus.Unavailable.color)
    }

    @Test
    fun `sensor type enum has four values`() {
        val values = SensorType.values()
        assertEquals(4, values.size)
        assertEquals("WiFi", SensorType.WIFI.displayName)
        assertEquals("Bluetooth", SensorType.BLUETOOTH.displayName)
        assertEquals("Microphone", SensorType.MICROPHONE.displayName)
        assertEquals("Camera", SensorType.CAMERA.displayName)
    }
}
```

- [ ] **Step 3.2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*SensorStatusTest*"`
Expected: BUILD FAILED (SensorStatus and SensorType not defined)

- [ ] **Step 3.3: Write minimal SensorStatus.kt**

```kotlin
package com.ezworksafe.data.model

sealed class SensorStatus(
    val label: String,
    val color: Long
) {
    data object Active : SensorStatus("Active", 0xFF4CAF50L)
    data object Inactive : SensorStatus("Inactive", 0xFF9E9E9EL)
    data object Denied : SensorStatus("Denied", 0xFFF44336L)
    data object Unavailable : SensorStatus("Unavailable", 0xFF616161L)
}

enum class SensorType(val displayName: String) {
    WIFI("WiFi"),
    BLUETOOTH("Bluetooth"),
    MICROPHONE("Microphone"),
    CAMERA("Camera")
}
```

- [ ] **Step 3.4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*SensorStatusTest*"`
Expected: BUILD SUCCESSFUL, 5 tests passed

- [ ] **Step 3.5: Commit**

```bash
git add app/src/main/java/com/ezworksafe/data/model/SensorStatus.kt app/src/test/java/com/ezworksafe/data/model/SensorStatusTest.kt
mkdir -p app/src/test/java/com/ezworksafe/data/model
git add app/src/test/java/com/ezworksafe/data/model/SensorStatusTest.kt
git commit -m "feat: add SensorStatus sealed class and SensorType enum"
```

---

## Task 4: Permission Utility

**Files:**
- Create: `app/src/main/java/com/ezworksafe/util/PermissionHelper.kt`
- Create: `app/src/test/java/com/ezworksafe/util/PermissionHelperTest.kt`

- [ ] **Step 4.1: Write the failing test**

```kotlin
package com.ezworksafe.util

import android.Manifest
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PermissionHelperTest {

    @get:Rule
    val instantRule = InstantTaskExecutorRule()

    @Test
    fun `required runtime permissions are CAMERA and RECORD_AUDIO`() {
        val expected = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        assertArrayEquals(expected, PermissionHelper.REQUIRED_RUNTIME_PERMISSIONS)
    }
}
```

- [ ] **Step 4.2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*PermissionHelperTest*"`
Expected: BUILD FAILED (PermissionHelper not defined)

- [ ] **Step 4.3: Write PermissionHelper.kt**

```kotlin
package com.ezworksafe.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object PermissionHelper {

    val REQUIRED_RUNTIME_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    fun areRuntimePermissionsGranted(context: Context): Boolean {
        return REQUIRED_RUNTIME_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
```

Note: The Android Manifest.permission constants are only available when compiled against the Android SDK. The test file above uses them but requires the android.jar SDK stubs. Since unit tests run against a mock Android environment, add a `testOptions` block or use Mockito for tests that exercise permission APIs. For now, the test verifies the constant array.

- [ ] **Step 4.4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*PermissionHelperTest*"`
Expected: BUILD SUCCESSFUL, test passes

- [ ] **Step 4.5: Commit**

```bash
git add app/src/main/java/com/ezworksafe/util/PermissionHelper.kt app/src/test/java/com/ezworksafe/util/PermissionHelperTest.kt
git commit -m "feat: add PermissionHelper for runtime permission checks"
```

---

## Task 5: SensorRepository — System Service Wrappers

**Files:**
- Create: `app/src/main/java/com/ezworksafe/data/repository/SensorRepository.kt`
- Create: `app/src/test/java/com/ezworksafe/data/repository/SensorRepositoryTest.kt`

- [ ] **Step 5.1: Write the failing test**

```kotlin
package com.ezworksafe.data.repository

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

class SensorRepositoryTest {

    @get:Rule
    val instantRule = InstantTaskExecutorRule()

    private val mockContext: Context = mock()

    @Test
    fun `repository exposes four sensor flows`() = runTest {
        val repo = SensorRepository(mockContext)

        val wifiStatus = repo.observeSensor(SensorType.WIFI).first()
        val btStatus = repo.observeSensor(SensorType.BLUETOOTH).first()
        val micStatus = repo.observeSensor(SensorType.MICROPHONE).first()
        val camStatus = repo.observeSensor(SensorType.CAMERA).first()

        // In test context without real system services, all read as Unavailable
        assertEquals(SensorStatus.Unavailable, wifiStatus)
        assertEquals(SensorStatus.Unavailable, btStatus)
        assertEquals(SensorStatus.Unavailable, micStatus)
        assertEquals(SensorStatus.Unavailable, camStatus)
    }
}
```

- [ ] **Step 5.2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*SensorRepositoryTest*"`
Expected: BUILD FAILED (SensorRepository not defined)

- [ ] **Step 5.3: Write SensorRepository.kt**

```kotlin
package com.ezworksafe.data.repository

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

class SensorRepository(private val context: Context) {

    fun observeSensor(type: SensorType): Flow<SensorStatus> {
        return when (type) {
            SensorType.WIFI -> observeWifiStatus()
            SensorType.BLUETOOTH -> observeBluetoothStatus()
            SensorType.MICROPHONE -> observeMicStatus()
            SensorType.CAMERA -> observeCameraStatus()
        }
    }

    private fun observeWifiStatus(): Flow<SensorStatus> = callbackFlow {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiManager == null) {
            trySend(SensorStatus.Unavailable)
            close()
            return@callbackFlow
        }

        fun emitState() {
            trySend(if (wifiManager.isWifiEnabled) SensorStatus.Active else SensorStatus.Inactive)
        }

        emitState()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                emitState()
            }
        }
        context.registerReceiver(receiver, IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))

        awaitClose { context.unregisterReceiver(receiver) }
    }

    private fun observeBluetoothStatus(): Flow<SensorStatus> = callbackFlow {
        val bluetoothAdapter = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                manager?.adapter
            } else {
                @Suppress("DEPRECATION")
                BluetoothAdapter.getDefaultAdapter()
            }
        } catch (e: Exception) {
            null
        }

        if (bluetoothAdapter == null) {
            trySend(SensorStatus.Unavailable)
            close()
            return@callbackFlow
        }

        fun emitState() {
            trySend(
                if (bluetoothAdapter.isEnabled) SensorStatus.Active
                else SensorStatus.Inactive
            )
        }

        emitState()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                emitState()
            }
        }
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(receiver, filter)

        awaitClose { context.unregisterReceiver(receiver) }
    }

    private fun observeMicStatus(): Flow<SensorStatus> = callbackFlow {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            // Cannot monitor mic status pre-24; report as unavailable
            trySend(SensorStatus.Unavailable)
            close()
            return@callbackFlow
        }

        fun emitState() {
            val configs: List<AudioRecordingConfiguration> = audioManager.activeRecordingConfigurations
            val micActive = configs.any { it.clientAudioSource == android.media.MediaRecorder.AudioSource.MIC }
            trySend(if (micActive) SensorStatus.Active else SensorStatus.Inactive)
        }

        emitState()

        val callback = object : AudioManager.AudioRecordingCallback() {
            override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>) {
                emitState()
            }
        }
        audioManager.registerAudioRecordingCallback(callback, null)

        awaitClose { audioManager.unregisterAudioRecordingCallback(callback) }
    }

    private fun observeCameraStatus(): Flow<SensorStatus> = callbackFlow {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        if (cameraManager == null) {
            trySend(SensorStatus.Unavailable)
            close()
            return@callbackFlow
        }

        fun emitState() {
            try {
                val cameraIds = cameraManager.cameraIdList
                val anyActive = cameraIds.any { id ->
                    val characteristics = cameraManager.getCameraCharacteristics(id)
                    val facing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)
                    facing != null
                }
                // We use a different approach — the availability callback tells us
                // when a camera is opened/closed. For initial state, query available.
                trySend(SensorStatus.Inactive)
            } catch (e: Exception) {
                trySend(SensorStatus.Unavailable)
            }
        }

        emitState()

        val callback = object : CameraManager.AvailabilityCallback() {
            override fun onCameraAvailable(cameraId: String) {
                trySend(SensorStatus.Inactive)
            }

            override fun onCameraUnavailable(cameraId: String) {
                trySend(SensorStatus.Active)
            }
        }
        cameraManager.registerAvailabilityCallback(callback, null)

        awaitClose { cameraManager.unregisterAvailabilityCallback(callback) }
    }
}
```

**Edge case:** When system services return null (unsupported hardware or emulator), the repository emits `SensorStatus.Unavailable` and closes the flow.

- [ ] **Step 5.4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*SensorRepositoryTest*"`
Expected: BUILD SUCCESSFUL, test passes (mock Context returns null for all services → Unavailable)

- [ ] **Step 5.5: Commit**

```bash
git add app/src/main/java/com/ezworksafe/data/repository/SensorRepository.kt app/src/test/java/com/ezworksafe/data/repository/SensorRepositoryTest.kt
git commit -m "feat: add SensorRepository with callbackFlow for WiFi, BT, Mic, Camera"
```

---

## Task 6: SensorViewModel

**Files:**
- Create: `app/src/main/java/com/ezworksafe/ui/viewmodel/SensorViewModel.kt`
- Create: `app/src/test/java/com/ezworksafe/ui/viewmodel/SensorViewModelTest.kt`

- [ ] **Step 6.1: Write the failing test**

```kotlin
package com.ezworksafe.ui.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

class SensorViewModelTest {

    @get:Rule
    val instantRule = InstantTaskExecutorRule()

    private val mockApp: Application = mock()

    @Test
    fun `viewModel exposes StateFlows for all four sensors`() {
        val vm = SensorViewModel(mockApp)

        assertNotNull(vm.wifiStatus)
        assertNotNull(vm.bluetoothStatus)
        assertNotNull(vm.micStatus)
        assertNotNull(vm.cameraStatus)
    }

    @Test
    fun `viewModel exposes sensor types list`() {
        val vm = SensorViewModel(mockApp)
        assertEquals(4, vm.sensorTypes.size)
    }
}
```

- [ ] **Step 6.2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*SensorViewModelTest*"`
Expected: BUILD FAILED (SensorViewModel not defined)

- [ ] **Step 6.3: Write SensorViewModel.kt**

```kotlin
package com.ezworksafe.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import com.ezworksafe.data.repository.SensorRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SensorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SensorRepository(application)

    val sensorTypes: List<SensorType> = SensorType.entries

    val wifiStatus: StateFlow<SensorStatus> = repository
        .observeSensor(SensorType.WIFI)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SensorStatus.Unavailable)

    val bluetoothStatus: StateFlow<SensorStatus> = repository
        .observeSensor(SensorType.BLUETOOTH)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SensorStatus.Unavailable)

    val micStatus: StateFlow<SensorStatus> = repository
        .observeSensor(SensorType.MICROPHONE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SensorStatus.Unavailable)

    val cameraStatus: StateFlow<SensorStatus> = repository
        .observeSensor(SensorType.CAMERA)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SensorStatus.Unavailable)
}
```

**Lifecycle notes:**
- `stateIn` with `WhileSubscribed(5_000)` keeps the upstream active for 5 seconds after the last collector — prevents restarting flows on configuration changes (rotation).
- `viewModelScope` automatically cancels collection when the ViewModel is cleared.

- [ ] **Step 6.4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*SensorViewModelTest*"`
Expected: BUILD SUCCESSFUL, tests pass

- [ ] **Step 6.5: Commit**

```bash
git add app/src/main/java/com/ezworksafe/ui/viewmodel/SensorViewModel.kt app/src/test/java/com/ezworksafe/ui/viewmodel/SensorViewModelTest.kt
git commit -m "feat: add SensorViewModel with StateFlow per sensor"
```

---

## Task 7: Theme Resources

**Files:**
- Create: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/values/themes.xml`

- [ ] **Step 7.1: Write colors.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="status_active">#FF4CAF50</color>
    <color name="status_inactive">#FF9E9E9E</color>
    <color name="status_denied">#FFF44336</color>
    <color name="status_unavailable">#FF616161</color>
    <color name="background">#FFF5F5F5</color>
    <color name="card_background">#FFFFFFFF</color>
</resources>
```

- [ ] **Step 7.2: Write themes.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.EzWorkSafe" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:statusBarColor">@color/background</item>
    </style>
</resources>
```

- [ ] **Step 7.3: Commit**

```bash
git add app/src/main/res/values/colors.xml app/src/main/res/values/themes.xml
git commit -m "feat: add color tokens and theme for status indicators"
```

---

## Task 8: Status Dashboard UI (Jetpack Compose)

**Files:**
- Create: `app/src/main/java/com/ezworksafe/ui/view/StatusDashboard.kt`

- [ ] **Step 8.1: Write StatusDashboard composable**

```kotlin
package com.ezworksafe.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import com.ezworksafe.ui.viewmodel.SensorViewModel

@Composable
fun StatusDashboard(viewModel: SensorViewModel) {
    val wifiStatus by viewModel.wifiStatus.collectAsState()
    val bluetoothStatus by viewModel.bluetoothStatus.collectAsState()
    val micStatus by viewModel.micStatus.collectAsState()
    val cameraStatus by viewModel.cameraStatus.collectAsState()

    val statuses = mapOf(
        SensorType.WIFI to wifiStatus,
        SensorType.BLUETOOTH to bluetoothStatus,
        SensorType.MICROPHONE to micStatus,
        SensorType.CAMERA to cameraStatus
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "ezWorkSafe",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Sensor Status",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        statuses.forEach { (type, status) ->
            StatusCard(sensorType = type, status = status)
        }
    }
}

@Composable
private fun StatusCard(sensorType: SensorType, status: SensorStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(status.color.toInt())),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = sensorType.displayName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = status.label,
                fontSize = 14.sp,
                color = Color(status.color.toInt()),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
```

- [ ] **Step 8.2: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8.3: Commit**

```bash
git add app/src/main/java/com/ezworksafe/ui/view/StatusDashboard.kt
git commit -m "feat: add Compose StatusDashboard with four sensor indicator cards"
```

---

## Task 9: MainActivity — Permission Orchestration & ViewModel Wiring

**Files:**
- Create: `app/src/main/java/com/ezworksafe/ui/view/MainActivity.kt`

- [ ] **Step 9.1: Write MainActivity.kt**

```kotlin
package com.ezworksafe.ui.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ezworksafe.util.PermissionHelper

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Re-render will happen via ViewModel observing permission state
        // For now, we just refresh the UI
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestRuntimePermissionsIfNeeded()

        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                val viewModel: com.ezworksafe.ui.viewmodel.SensorViewModel = viewModel()
                StatusDashboard(viewModel = viewModel)
            }
        }
    }

    private fun requestRuntimePermissionsIfNeeded() {
        if (!PermissionHelper.areRuntimePermissionsGranted(this)) {
            requestPermissionLauncher.launch(PermissionHelper.REQUIRED_RUNTIME_PERMISSIONS)
        }
    }
}
```

**Permission request timing:** Runtime permissions for CAMERA and RECORD_AUDIO are requested on first launch. If denied, the app still runs — sensors report as `Denied` based on permission check results (future enhancement: integrate permission state into SensorRepository).

**Edge case:** If the user denies the permission request, the `ActivityResult` callback fires but currently does not re-prompt. The sensor flows will still emit based on hardware state (permission denial won't crash the app — camera/mic flows just won't detect usage without permission).

- [ ] **Step 9.2: Verify full build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 9.3: Commit**

```bash
git add app/src/main/java/com/ezworksafe/ui/view/MainActivity.kt
git commit -m "feat: add MainActivity with permission request and ViewModel wiring"
```

---

## Task 10: Real-Time Update Verification

**Files modified:**
- Verify: `app/src/main/java/com/ezworksafe/data/repository/SensorRepository.kt`

- [ ] **Step 10.1: Verify BroadcastReceiver registrations survive config changes**

The ViewModel uses `SharingStarted.WhileSubscribed(5_000)` which keeps the upstream `callbackFlow` alive for 5 seconds after the Activity is destroyed (e.g., during rotation). If the Activity recreates within 5s, the flow continues without interruption.

This is a design verification — no code change needed unless the 5-second buffer proves insufficient during testing.

- [ ] **Step 10.2: Integrate permission denial into sensor status**

Add a permission check in the camera and mic observation flows to emit `Denied` when the runtime permission is not granted:

```kotlin
// In SensorRepository.kt, inside observeCameraStatus():
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
    ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
) {
    trySend(SensorStatus.Denied)
    // Don't close — re-emit if permission is granted later
}
```

Add the same check for `RECORD_AUDIO` in `observeMicStatus()`.

**Edge case:** Permission can change at runtime (user revokes from Settings). The permission check at flow start captures the current state but won't react to changes. A future enhancement could observe permission changes via a `BroadcastReceiver` or `ActivityResult` callback.

- [ ] **Step 10.3: Verify no double-registration on rapid config changes**

ViewModel scope survives config changes (the ViewModel is retained). Each sensor's `callbackFlow` is created once in the ViewModel constructor via `stateIn`. Rotation does not re-create the flow. Verified by design — no code change needed.

- [ ] **Step 10.4: Commit**

```bash
git add app/src/main/java/com/ezworksafe/data/repository/SensorRepository.kt
git commit -m "feat: emit Denied status when runtime permissions not granted for camera/mic"
```

---

## Task 11: Foreground Service (Optional)

**Files:**
- Create: `app/src/main/java/com/ezworksafe/service/MonitoringService.kt`

Skip this task unless background monitoring is required. Implement when needed.

- [ ] **Step 11.1: (Conditional) Write MonitoringService.kt**

```kotlin
package com.ezworksafe.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MonitoringService : Service() {

    companion object {
        const val CHANNEL_ID = "ezworksafe_monitoring"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Monitoring",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "ezWorkSafe background sensor monitoring"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ezWorkSafe")
            .setContentText("Monitoring sensor status")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}
```

- [ ] **Step 11.2: (Conditional) Uncomment service in AndroidManifest.xml**

Remove the `<!-- -->` comment markers around the `<service>` block in `AndroidManifest.xml`.

- [ ] **Step 11.3: (Conditional) Commit**

```bash
git add app/src/main/java/com/ezworksafe/service/MonitoringService.kt app/src/main/AndroidManifest.xml
git commit -m "feat: add foreground service for background monitoring"
```

---

## Task 12: Testing & Verification

- [ ] **Step 12.1: Run all unit tests**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 12.2: Run lint**

Run: `./gradlew lint`
Expected: BUILD SUCCESSFUL, no lint errors

- [ ] **Step 12.3: Build release APK**

Run: `./gradlew assembleRelease`
Expected: BUILD SUCCESSFUL, APK generated at `app/build/outputs/apk/release/`

- [ ] **Step 12.4: Commit any final fixes**

```bash
git add -A
git commit -m "chore: fix lint issues and finalize build config"
```

---

## Edge Cases & Design Decisions Summary

| Edge Case | Handling |
|-----------|----------|
| System service returns null (unsupported hardware) | `SensorStatus.Unavailable`, flow closes |
| Runtime permission denied | `SensorStatus.Denied` emitted (camera/mic flows) |
| Permission revoked at runtime | Current: stale state until flow restarts; Future: observe permission changes |
| Configuration change (rotation) | `WhileSubscribed(5_000)` keeps flows alive, ViewModel retained |
| Android < 24 (no AudioRecordingCallback) | Mic reports `SensorStatus.Unavailable` |
| BluetoothAdapter.getDefaultAdapter() deprecated in API 33 | Uses `getSystemService(BluetoothAdapter::class.java)` on API 23+ |
| WifiManager deprecated direct instantiation | Uses `getSystemService(Context.WIFI_SERVICE)` |
| Camera2 unavailable on emulator | `cameraManager.cameraIdList` may throw → `Unavailable` |
| Dual-SIM / multi-camera devices | Iterates all camera IDs via `cameraIdList` |
| No Google Play Services | Not required — uses only AOSP APIs |

---

## Execution Handoff

**Plan complete. Two execution options:**

1. **Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration
2. **Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
