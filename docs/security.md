# Security Audit: ezWorkSafe

**Date:** 2026-05-12
**Scope:** Full codebase audit — permissions, IPC, logging, data handling, crypto, network, build pipeline.
**Methodology:** Manual source code review. No dynamic analysis or penetration testing performed.

---

## Summary

| Risk Level | Count |
|------------|-------|
| Critical   | 0     |
| High       | 0     |
| Medium     | 0     |
| Low        | 5     |
| Informational | 3  |

The app has a **small attack surface** — no network calls, no storage, no ContentProviders, no WebViews, no third-party SDKs beyond Jetpack. The primary risk vectors are component exposure (widget receiver) and home-screen data leakage (by design). All medium-severity issues have been addressed.

---

## Findings

### M-1: BLUETOOTH_CONNECT runtime permission never requested

**Status: ✓ FIXED**

**Files changed:**
- `app/src/main/java/com/ezworksafe/util/PermissionHelper.kt` — `REQUIRED_RUNTIME_PERMISSIONS` is now version-gated: includes `BLUETOOTH_CONNECT` on API 31+
- `app/src/main/java/com/ezworksafe/data/repository/SystemSensorRepository.kt` — added permission check at top of `observeBluetoothStatus()`; narrowed `catch (e: Exception)` to `catch (e: SecurityException)`
- `app/src/test/java/com/ezworksafe/util/PermissionHelperTest.kt` — tests for both API level paths

`BLUETOOTH_CONNECT` is a dangerous permission on API 31+ but the app never requests it at runtime. `SystemSensorRepository.observeBluetoothStatus()` calls `BluetoothAdapter.isEnabled()` which throws `SecurityException` without this permission. The broad `catch (e: Exception)` at line 78 catches this and returns `null`, degrading to `Unavailable`.

**Impact:** On API 31+ devices, Bluetooth status always reports `Unavailable` instead of `Active`/`Blocked`. The user never sees a permission prompt and has no way to grant this permission from within the app.

**Fix:**
1. Added `BLUETOOTH_CONNECT` to `PermissionHelper.REQUIRED_RUNTIME_PERMISSIONS` (version-gated to API 31+)
2. Narrowed `catch (e: Exception)` in `observeBluetoothStatus()` to `catch (e: SecurityException)` for explicit handling
3. Added `BLUETOOTH_CONNECT` permission check (API 31+) that emits `SensorStatus.Denied` if not granted

### M-2: Release build has minification disabled, debug logs persist in production

**Status: ✓ FIXED**

**Files changed:**
- `app/build.gradle.kts` — `isMinifyEnabled` set to `true` for release builds
- `app/proguard-rules.pro` — added ProGuard rules to strip `Log.d` calls

`MonitoringService.pushWidgetUpdate()` logs sensor statuses via `Log.d()`. With `isMinifyEnabled = false` and no ProGuard rules, `Log.d` calls are preserved in the release APK. While the data is not sensitive (Active/Inactive/Blocked/Denied/Unavailable), the log tag includes class names and the messages include widget IDs.

**Impact:** Low-severity information disclosure. Widget IDs and sensor state are visible to any app with `READ_LOGS` permission (disallowed on API 24+ for non-system apps, but `adb logcat` on a debug-connected device exposes them).

**Fix:**
1. Enabled minification (`isMinifyEnabled = true`) for release builds
2. Added ProGuard rule to strip `Log.d` calls in release builds

---

### L-1: Broad catch blocks mask unexpected errors

**File:** `SystemSensorRepository.kt:78,119,187`

Three locations catch `Exception` broadly rather than specific exception types:
- Line 78: `catch (e: Exception)` in `observeBluetoothStatus()`
- Line 119: `catch (_: Exception)` in `isAppOpBlocked()`
- Line 187: `catch (_: Exception)` in `observeCameraStatus()`

**Impact:** Unexpected `RuntimeException` subtypes (e.g., `NullPointerException`, `IllegalStateException`) are silently swallowed, making debugging difficult.

**Recommendation:** Narrow to the expected exception types:
- Line 78: `SecurityException` (missing BLUETOOTH_CONNECT) and potentially `NullPointerException`
- Line 119: `SecurityException` (missing permission for AppOps) and `NullPointerException`
- Line 187: Already preceded by specific `SecurityException` and `CameraAccessException` catches — the broad catch is the final fallback and is acceptable as a crash-prevention measure, but should log the unexpected exception.

### L-2: android:allowBackup enabled

**File:** `AndroidManifest.xml:26`

`android:allowBackup="true"` allows the device's backup mechanism to extract the app's data. The app stores no persistent data (no database, SharedPreferences, or files), so the practical risk is near-zero. However, it's a best-practice violation.

**Recommendation:** Set `android:allowBackup="false"` if no backup is needed, or add `android:dataExtractionRules` to explicitly control what can be backed up.

### L-3: Keystore password stored in plaintext (in CI and local template)

**File:** `keystore.properties.template`, `.github/workflows/android.yml:37-42`

The release keystore password, key alias, and key password are read from `keystore.properties` and written to disk in CI via `echo`. The file is in `.gitignore` locally, and CI uses ephemeral runners.

**Impact:** Standard Android practice, but plaintext keystore passwords on disk are a risk if the CI runner artifact is compromised or if `keystore.properties` is accidentally committed.

**Recommendation:** Add a git hook or CI step to validate that `keystore.properties` is not in the staging area:
```bash
# .git/hooks/pre-commit
if git diff --cached --name-only | grep -q "keystore.properties"; then
    echo "ERROR: keystore.properties is staged for commit"
    exit 1
fi
```

### L-4: Widget exposes sensor status on home screen / lock screen

**File:** `SensorWidget.kt`, `widget_sensor_status.xml`

The home screen widget displays real-time sensor status (WiFi Active/Inactive, Mic Active/Inactive, etc.). This is visible without unlocking the device on the lock screen if the widget is placed there.

**Impact:** In shared-device or public-display scenarios, sensor state could be observed by bystanders. This is inherent to the app's purpose (workplace safety monitoring in an employer-provided device context).

**Recommendation:** None required — this is a design feature, not a vulnerability. Document in the app's privacy notice.

### L-5: Conditional release signing silently falls back to unsigned

**File:** `app/build.gradle.kts:30-46`

If `keystore.properties` file doesn't exist, `signingConfigs.findByName("release")` on line 45 returns `null`, and the release build is unsigned. An unsigned APK cannot be installed on a device.

**Impact:** Build process may produce an unusable APK without warning if keystore is misconfigured.

**Recommendation:** Add an explicit null check or fail-fast:
```kotlin
signingConfig = signingConfigs.findByName("release") ?: error("keystore.properties not found")
```

---

### I-1: No network calls, storage, or third-party SDKs

**Positive finding.** The app makes zero network requests, stores zero data persistently, and uses only Jetpack/AndroidX libraries. This dramatically reduces the attack surface compared to typical Android apps.

### I-2: PendingIntents correctly use FLAG_IMMUTABLE

**File:** `MonitoringService.kt:134`

All PendingIntents use `PendingIntent.FLAG_IMMUTABLE`, preventing PendingIntent hijacking attacks (CVE-2019-2114). No implicit PendingIntents exist.

### I-3: Android 16 AppOps background limitation documented

**File:** `AGENTS.md`

The server-side enforcement of AppOps for background processes on Android 16 is documented as a known limitation. The widget's split-section architecture (left section real-time, right section foreground-refreshed) is a deliberate workaround.

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

| Priority | Issue |
|----------|-------|
| **Fixed** | M-1: Request BLUETOOTH_CONNECT at runtime on API 31+ |
| **Fixed** | M-2: Enable minification + strip debug logs in release builds |
| **Low** | L-1: Narrow exception types in catch blocks |
| **Low** | L-2: Set `android:allowBackup="false"` |
| **Low** | L-3: Add pre-commit hook for `keystore.properties` |
| **Low** | L-5: Fail fast on missing keystore config |

---

## Files Examined

| File | Lines |
|------|-------|
| `app/build.gradle.kts` | 102 |
| `AndroidManifest.xml` | 59 |
| `EzWorkSafeApp.kt` | 15 |
| `data/repository/SystemSensorRepository.kt` | 212 |
| `data/repository/SensorRepository.kt` | 10 |
| `data/model/SensorStatus.kt` | 25 |
| `ui/view/MainActivity.kt` | 62 |
| `ui/viewmodel/SensorViewModel.kt` | 36 |
| `service/MonitoringService.kt` | 159 |
| `util/PermissionHelper.kt` | 20 |
| `widget/SensorWidget.kt` | 160 |
| `widget/SensorWidgetReceiver.kt` | 23 |
| `widget/WidgetState.kt` | 13 |
| `res/values/*.xml` | 13 total |
| `res/layout/*.xml` | 364 total |
| `.github/workflows/android.yml` | 51 |
| `keystore.properties.template` | 6 |
| `.gitignore` | 10 |
