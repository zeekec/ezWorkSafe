# Security Audit: ezWorkSafe

**Date:** 2026-05-18
**Scope:** Full codebase audit — permissions, IPC, logging, data handling, crypto, network, build pipeline.
**Methodology:** Manual source code review. No dynamic analysis or penetration testing performed.

---

## Summary

| Risk Level | Count | Key Items |
|------------|-------|-----------|
| Critical   | 0     | — |
| High       | 0     | — |
| Medium     | 2     | PendingIntent request code collision, missing Glance ProGuard keep rules |
| Low        | 4     | `buildConfig` enabled, `Log.w` in release, empty permission rationale callback, `START_STICKY` on modern Android, stale state on background permission revocation |
| Informational | 15  | Documented green checks |

The app has a **small attack surface** — no network calls, no storage, no ContentProviders, no WebViews, no third-party
SDKs beyond Jetpack. The primary risk vectors are component exposure (widget receiver) and home-screen data leakage (by
design). All medium-severity issues from the previous audit have been addressed; two medium-severity issues remain open.

---

## Findings

### M-1: BLUETOOTH_CONNECT runtime permission never requested

**Status: ✓ FIXED** (previous audit)

**Files changed (historical):**
- `PermissionHelper.kt` — `REQUIRED_RUNTIME_PERMISSIONS` is now version-gated: includes `BLUETOOTH_CONNECT` on API 31+
- `SystemSensorRepository.kt` — added permission check at top of`observeBluetoothStatus()` ; narrowed
  `catch (e: Exception)` to`catch (e: SecurityException)`
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
- `AndroidManifest.xml` — Replaced `foregroundServiceType="dataSync"` with `foregroundServiceType="specialUse"`,
  replaced `FOREGROUND_SERVICE_DATA_SYNC` permission with `FOREGROUND_SERVICE_SPECIAL_USE`, added
  `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE">` with use case description
- `MonitoringService.kt` — Extracted `startForegroundNotification()` helper using three-parameter
  `startForeground()` overload with `ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE` on API 34+

### M-4: Missing Glance ProGuard keep rules

**Status: ✓ NEW FINDING**

**File:** `app/proguard-rules.pro`

ProGuard/R8 only strips`Log.d()` calls. No`-keep` rules exist for Glance widget classes (`SensorWidget` ,
  `SensorWidgetReceiver` ,`WidgetState` ). Glance accesses widgets via reflection for WorkManager-based initial render.
  Without keep rules, release APKs may strip widget class names, causing:
- Blank/empty widget on home screen
- `ClassNotFoundException` in WorkManager background tasks
- Crash on widget update

**Impact:** Widget rendering may fail silently in release builds.

**Recommended fix:** Add to `proguard-rules.pro`:
```
-keep class com.ezworksafe.widget.** { *; }
```

### M-5: PendingIntent request code collision

**Status: ✓ NEW FINDING**

**File:** `app/src/main/java/com/ezworksafe/service/MonitoringService.kt:101,120`

Two`PendingIntent.getActivity()` calls use identical request code (0) with the same target component (`MainActivity` ).
  `Intent.filterEquals()` considers the two intents equal (same component, no action/data/category differences), so the
  system caches them as a single PendingIntent. With`FLAG_UPDATE_CURRENT` , whichever is created last overwrites the
  first.
- `pushWidgetUpdate()` line 101: request code 0, flags `NEW_TASK | CLEAR_TOP`
- `createNotification()` line 120: request code 0, flags `SINGLE_TOP | CLEAR_TOP`

**Impact:** The launch behavior of widget tap and notification "Refresh" action are identical — both use whichever
  PendingIntent was most recently created. The intent flag differences are lost. Not exploitable (both use
  `FLAG_IMMUTABLE` ), but undefined behavior means a widget tap may not create a new task as intended.

**Recommended fix:** Use distinct request codes:
```kotlin
// pushWidgetUpdate():
PendingIntent.getActivity(this, 0, openIntent, ...)  // request code 0

// createNotification():
PendingIntent.getActivity(this, 1, refreshIntent, ...)  // request code 1
```

---

### L-1: Broad catch blocks mask unexpected errors

**Status: ✓ PREVIOUSLY FIXED** (from original audit)

**Files changed (historical):**
- `SystemSensorRepository.kt`: `isAppOpBlocked()` catch narrowed to `SecurityException`
- `SystemSensorRepository.kt`: camera fallback catch now logs via `Log.w`

Remaining: Camera catch-all at line 199 now logs via `Log.w`, which is appropriate.

### L-2: android:allowBackup enabled

**Status: ✓ FIXED** (previous audit)

`allowBackup="false"` and `fullBackupContent="false"` set in `AndroidManifest.xml:26-27`.

### L-3: Keystore password in plaintext

**Status: ✓ FIXED** (previous audit)

Pre-commit hook (`.githooks/pre-commit`) blocks `keystore.properties` commits.

### L-4: Widget exposes sensor status on home screen / lock screen

**Status: By design, no change needed.**

`android:widgetCategory="home_screen"` in`widget_info_sensor.xml:8` limits display to home screen (not lock screen).
  Home screen data exposure is inherent to the app's purpose (workplace safety monitoring).

### L-5: Conditional release signing silently falls back to unsigned

**Status: ✓ FIXED** (previous audit)

`afterEvaluate` block logs warning when `keystore.properties` is missing.

---

### L-6: `buildConfig = true` enabled

**Status: ✓ NEW FINDING**

**File:** `app/build.gradle.kts:51`

`buildConfig = true` exposes`BuildConfig.DEBUG` and`BuildConfig.VERSION_NAME` . The app uses`BuildConfig.VERSION_NAME`
  only in`AppInfoDialog.kt:73` for display. Not used for security decisions, but disabling reduces the exposed surface.

**Recommended fix:** Set `buildConfig = false` and read version from `packageManager` if needed.

### L-7: `Log.w()` calls survive in release builds

**Status: ✓ NEW FINDING**

**Files:** `SystemSensorRepository.kt:200`, `SensorWidgetReceiver.kt:26`

ProGuard strips `Log.d()` only. Two `Log.w()` calls survive in release builds:
- `SystemSensorRepository.kt:200`: Unexpected camera error logging
- `SensorWidgetReceiver.kt:26`: FGS restriction warning

Content is non-sensitive, but noisier than necessary in release builds.

### L-8: Empty permission rationale callback

**Status: ✓ NEW FINDING**

**File:** `MainActivity.kt:28-29`

The`ActivityResultContracts.RequestMultiplePermissions()` callback body is empty. If the user denies permissions, no
  rationale is shown and the user sees "Denied" status with no guidance on how to grant permissions.

### L-9: `START_STICKY` on modern Android

**Status: ✓ NEW FINDING**

**File:** `MonitoringService.kt:52-54`

On Android 14+,`START_STICKY` restart behavior is restricted. The system may delay restart or not restart the service at
  all under memory pressure.

### L-10: Permission revocation not detected in background

**Status: ✓ NEW FINDING — documented known limitation**

If the user revokes`CAMERA` or`RECORD_AUDIO` in Settings while the app is backgrounded, the service's sensor observation
  continues showing the old state. Detection only occurs when`refresh()` is triggered (app opened or notification
  "Refresh" tapped). This is documented in AGENTS.md.

---

## Attack Surface Summary

| Vector | Present? | Notes |
|--------|----------|-------|
| Network calls          | No | No HTTP, WebSocket, or network I/O |
| Local storage          | No | No files, DB, or SharedPreferences |
| ContentProviders       | No | None declared |
| BroadcastReceivers     | Yes | `SensorWidgetReceiver` (exported, widget system only) |
| Bound services         | No | Not exported |
| WebViews               | No | None used |
| Deep links             | No | No intent filters matching URLs |
| FileProvider           | No | None declared |
| Third-party SDKs       | Low | Jetpack/AndroidX only — official Google libraries |
| Runtime permissions    | Complete | CAMERA + RECORD_AUDIO + BLUETOOTH_CONNECT (API 31+) all requested at runtime. |

---

## Recommendations Priority

| Priority | Issue (since last audit) |
|----------|--------------------------|
| ~~**Medium** | M-3: `foregroundServiceType` mismatch — use `specialUse` or appropriate type |~~ ✓ Fixed (PR #41)
| **Medium** | M-4: Add Glance ProGuard keep rules for release widget rendering |
| **Medium** | M-5: Fix PendingIntent request code collision |
| **Low** | L-6: Consider disabling `buildConfig` for release |
| **Low** | L-7: Strip `Log.w()` or keep for debugging |
| **Low** | L-8: Show permission rationale on denial |
| **Low** | L-9: Evaluate `START_NOT_STICKY` or `START_REDELIVER_INTENT` for modern Android |
| **Low** | L-10: Document permission revocation limitation (already in AGENTS.md) |

---

## Files Examined

| File | Lines |
|------|-------|
| `app/build.gradle.kts` | 113 |
| `AndroidManifest.xml` | 64 |
...
| `service/MonitoringService.kt` | 197 |
| `util/PermissionHelper.kt` | 33 |
| `util/FormatUtils.kt` | 12 |
| `widget/SensorWidget.kt` | 147 |
| `widget/SensorWidgetReceiver.kt` | 43 |
| `widget/WidgetState.kt` | 16 |
| `res/values/*.xml` | 12 total |
| `res/layout/*.xml` | 346 total |
| `res/xml/widget_info_sensor.xml` | 9 |
| `proguard-rules.pro` | 4 |
| `.github/workflows/android.yml` | 57 |
| `keystore.properties.template` | 6 |
| `.githooks/pre-commit` | 7 |
| `.gitignore` | 15 |
| `gradle.properties` | 6 |
| `settings.gradle.kts` | 21 |
| `README.md` | 107 |