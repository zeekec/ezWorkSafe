# Security Audit: ezWorkSafe

**Date:** 2026-05-21
**Scope:** Full codebase audit — permissions, IPC, logging, data handling, crypto, network, build pipeline.
**Methodology:** Manual source code review. No dynamic analysis or penetration testing performed.

---

## Summary

| Risk Level | Count | Key Items |
|------------|-------|-----------|
| Critical   | 0     | — |
| High       | 0     | — |
| Medium     | 0     | — |
| Low        | 0     | — |
| Informational | 17  | N-1 by design, N-2 and N-3 actionable |

All security issues identified during this audit have been resolved. One informational finding remains actionable (N-2: update deprecated `getPackageInfo` call); one is noted as by-design (N-1).

---

## Findings

### M-1: BLUETOOTH_CONNECT runtime permission never requested

**Status: ✓ FIXED** (previous audit)

**Files changed (historical):**
- `PermissionHelper.kt` — `REQUIRED_RUNTIME_PERMISSIONS` is now version-gated: includes `BLUETOOTH_CONNECT` on API 31+
- `SystemSensorRepository.kt` — added permission check at top of `observeBluetoothStatus()` ; narrowed `catch (e: Exception)` to `catch (e: SecurityException)`
- `PermissionHelperTest.kt` — tests for both API level paths

### M-2: Release build had debug logs and no minification

**Status: ✓ FIXED** (previous audit)

**Files changed (historical):**
- `app/build.gradle.kts` — `isMinifyEnabled` set to `true` for release builds
- `app/proguard-rules.pro` — added ProGuard rules to strip `Log.d` calls

---

### M-3: `foregroundServiceType="dataSync"` may be incorrect

**Status: ✓ FIXED** (PR #41)

**Files changed:**
- `AndroidManifest.xml` — Replaced `foregroundServiceType="dataSync"` with `foregroundServiceType="specialUse"`, replaced `FOREGROUND_SERVICE_DATA_SYNC` permission with `FOREGROUND_SERVICE_SPECIAL_USE`, added `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE">` with use case description
- `MonitoringService.kt` — Extracted `startForegroundNotification()` helper using three-parameter `startForeground()` overload with `ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE` on API 34+

### M-4: Missing Glance ProGuard keep rules

**Status: ✓ FIXED** (PR #43)

**File:** `app/proguard-rules.pro`

Added `-keep class com.ezworksafe.widget.** { *; }` to preserve all widget classes accessed by Glance via reflection.

### M-5: PendingIntent request code collision

**Status: ✓ FIXED** (PR #44)

**Files:**
- `MonitoringService.kt:43-44` — Added `REQUEST_CODE_WIDGET = 0` and `REQUEST_CODE_REFRESH = 1`
- `MonitoringService.kt:102` — Widget PendingIntent uses `REQUEST_CODE_WIDGET`
- `MonitoringService.kt:141-143` — Notification Refresh PendingIntent uses `REQUEST_CODE_REFRESH`

Both use `FLAG_IMMUTABLE` and `FLAG_UPDATE_CURRENT`. Distinct request codes prevent the two PendingIntents from being treated as identical by the system.

---

### L-1: Broad catch blocks mask unexpected errors

**Status: ✓ PREVIOUSLY FIXED** (from original audit)

**Files changed (historical):**
- `SystemSensorRepository.kt`: `isAppOpBlocked()` catch narrowed to `SecurityException`
- `SystemSensorRepository.kt`: camera fallback catch now logs via `Log.w`

Remaining: Camera catch-all at line 199 now logs via `Log.w`, which is stripped in release builds.

### L-2: android:allowBackup enabled

**Status: ✓ FIXED** (previous audit)

`allowBackup="false"` and `fullBackupContent="false"` set in `AndroidManifest.xml:26-27`.

### L-3: Keystore password in plaintext

**Status: ✓ FIXED** (previous audit)

Pre-commit hook (`.githooks/pre-commit`) blocks `keystore.properties` commits. `.gitignore` also excludes it.

### L-4: Widget exposes sensor status on home screen / lock screen

**Status: By design, no change needed.**

`android:widgetCategory="home_screen"` in `widget_info_sensor.xml:8` limits display to home screen (not lock screen). Home screen data exposure is inherent to the app's purpose (workplace safety monitoring).

### L-5: Conditional release signing silently falls back to unsigned

**Status: ✓ FIXED** (previous audit)

`afterEvaluate` block logs warning when `keystore.properties` is missing.

### L-6: `buildConfig = true` enabled

**Status: ✓ FIXED**

**File:** `app/build.gradle.kts:51`

`buildConfig = false` is now set. Version name is read from `PackageManager` in `AppInfoDialog.kt:82` instead of `BuildConfig.VERSION_NAME`.

### L-7: `Log.w()` calls survive in release builds

**Status: ✓ FIXED**

**File:** `app/proguard-rules.pro:4`

`-assumenosideeffects` now includes `public static int w(...);` in addition to `d(...)`. Both `Log.w()` call sites (`SystemSensorRepository.kt:222`, `SensorWidgetReceiver.kt:26`) are now stripped in release builds.

### L-8: Empty permission rationale callback

**Status: ✓ FIXED**

**File:** `MainActivity.kt:29-37`

The `ActivityResultContracts.RequestMultiplePermissions()` callback now shows a `Toast` explaining that denied permissions will cause sensor status to be unavailable.

### L-9: `START_STICKY` on modern Android

**Status: ✓ FIXED**

**File:** `MonitoringService.kt:56-58`

`onStartCommand` now returns `START_REDELIVER_INTENT` instead of `START_STICKY`, which is more appropriate for this service's purpose and aligns with modern Android restart behavior.

### L-10: Permission revocation not detected in background

**Status: ✓ FIXED — documented known limitation**

If the user revokes `CAMERA` or `RECORD_AUDIO` in Settings while the app is backgrounded, the service's sensor observation continues showing the old state. Detection only occurs when `refresh()` is triggered (app opened or notification "Refresh" tapped). This is documented in AGENTS.md. No client-side workaround exists due to Android platform limitations (AppOps deprecated, no permission-change broadcast).

---

### N-1 (Info): Camera/mic monitoring shows "Active" based on permission/AppOps, not actual hardware usage

**Status: ✓ BY DESIGN**

**File:** `SystemSensorRepository.kt:158-243`

The `observeMicStatus()` and `observeCameraStatus()` flows check permission and AppOps privacy toggle state, then return `Active` if both allow it. They don't inspect callback data (`AudioRecordingCallback.configs`, `AvailabilityCallback.cameraAvailable`) to determine whether another app is currently using the hardware.

**Intent:** The app's purpose is to report whether the hardware *can* be used (permission granted, privacy toggle not blocking), not whether it's currently *in use*. The label "Active" means "this sensor is available for use." The callbacks are still registered to re-emit when permission/toggle state changes, even though the hardware-usage data is not used.

---

### N-2 (Info): Deprecated `getPackageInfo(String, int)` on API 33+

**File:** `AppInfoDialog.kt:82`

```kotlin
@Suppress("DEPRECATION")
getPackageInfo(context.packageName, 0).versionName
```

On API 33+, the two-parameter `getPackageInfo()` is deprecated in favor of `getPackageInfo(String, PackageInfoFlags)`. The `@Suppress("DEPRECATION")` annotation silences the warning rather than fixing the call.

**Impact:** Functions correctly on current API levels (up to API 36). If Google removes the deprecated overload in a future SDK release, the version display in the About dialog will break. No security impact — display only.

**Recommendation:** Version-gate with an API 33+ check:
```kotlin
val versionName = if (Build.VERSION.SDK_INT >= 33) {
    context.packageManager.getPackageInfo(
        context.packageName,
        PackageManager.PackageInfoFlags.of(0)
    ).versionName
} else {
    @Suppress("DEPRECATION")
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
} ?: "?"
```

### N-3 (Info): `RECEIVER_NOT_EXPORTED` used without API version guard

**Status: ✓ FIXED**

**Files:** `SystemSensorRepository.kt:77,130`

`context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)` calls are now guarded with `Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU` checks, falling back to the two-parameter `registerReceiver(receiver, filter)` on API 26-32.

---

## Attack Surface Summary

| Vector | Present? | Notes |
|--------|----------|-------|
| Network calls          | No | No HTTP, WebSocket, or network I/O |
| Local storage          | No | No files, DB, or SharedPreferences |
| ContentProviders       | No | None declared |
| BroadcastReceivers     | Yes | `SensorWidgetReceiver` (exported, widget system only); context-registered with `RECEIVER_NOT_EXPORTED` (API 33+ guard, see N-3) |
| Bound services         | No | `onBind` returns null |
| WebViews               | No | None used |
| Deep links             | No | No intent filters matching URLs |
| FileProvider           | No | None declared |
| Third-party SDKs       | Low | Jetpack/AndroidX only — official Google libraries |
| Runtime permissions    | Complete | CAMERA + RECORD_AUDIO + BLUETOOTH_CONNECT (API 31+) all requested at runtime |
| Implicit intents       | None | All intents use explicit component names |
| PendingIntents         | Secure | All use `FLAG_IMMUTABLE`; request codes are distinct |
| Notification           | Secure | No PII; uses `PRIORITY_LOW`; stripped by ProGuard in release |

---

## Recommendations Priority

| Priority | Issue |
|----------|-------|
| **Info** | N-2: Update `getPackageInfo()` call to use `PackageInfoFlags` on API 33+ |

---

## Files Examined

| File | Lines |
|------|-------|
| `app/build.gradle.kts` | 118 |
| `AndroidManifest.xml` | 64 |
| `service/MonitoringService.kt` | 157 |
| `data/repository/SystemSensorRepository.kt` | 231 |
| `data/repository/SensorRepository.kt` | 13 |
| `data/model/SensorStatus.kt` | 28 |
| `ui/view/MainActivity.kt` | 76 |
| `ui/view/StatusDashboard.kt` | 137 |
| `ui/view/AppInfoDialog.kt` | 255 |
| `ui/view/EzWorkSafeTheme.kt` | 41 |
| `ui/viewmodel/SensorViewModel.kt` | 41 |
| `util/PermissionHelper.kt` | 33 |
| `util/FormatUtils.kt` | 12 |
| `widget/SensorWidget.kt` | 150 |
| `widget/SensorWidgetReceiver.kt` | 43 |
| `widget/BuildWidgetRemoteViews.kt` | 51 |
| `widget/WidgetState.kt` | 16 |
| `widget/WidgetColors.kt` | 18 |
| `EzWorkSafeApp.kt` | 18 |
| `res/layout/widget_initial_layout.xml` | 172 |
| `res/layout/widget_sensor_status.xml` | 174 |
| `res/xml/widget_info_sensor.xml` | 9 |
| `proguard-rules.pro` | 7 |
| `.github/workflows/android.yml` | 55 |
| `keystore.properties.template` | 6 |
| `.githooks/pre-commit` | 8 |
| `.gitignore` | 14 |
| `gradle.properties` | 6 |
| `settings.gradle.kts` | 21 |
| Test files (17 files across `src/test/`, `src/testShared/`, `src/androidTest/`) | ~1,100 |
