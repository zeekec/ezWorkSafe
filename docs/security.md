# Security Audit: ezWorkSafe

**Date:** 2026-05-25 **Last updated:** 2026-09-22 (re-audit after dependency bumps) **Scope:** Full codebase audit —
permissions, IPC, logging, data handling, crypto, network, build pipeline. **Methodology:** Manual source code review.
No dynamic analysis or penetration testing performed.

---

## Summary

| Risk Level | Count | Key Items |
|------------|-------|-----------|
| Critical   | 0     | — |
| High       | 0     | — |
| Medium     | 0     | — (M-7 targetSdk 36 bump **resolved** in PR #158) |
| Low        | 3     | N-4 notification content disclosure, N-5 WidgetState weak concurrency, L-11 polling/widget write churn |
| Informational | 18  | N-1 through N-17 (new + existing info findings); build/CI hardening items |

All security issues identified during this audit have been resolved or documented as by-design.

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
- `MonitoringService.kt:56-57` — Added `REQUEST_CODE_WIDGET = 0` and `REQUEST_CODE_REFRESH = 1`
- `MonitoringService.kt:122-125` — Widget PendingIntent uses `REQUEST_CODE_WIDGET`
- `MonitoringService.kt:167-170` — Notification Refresh PendingIntent uses `REQUEST_CODE_REFRESH`

Both use `FLAG_IMMUTABLE` and `FLAG_UPDATE_CURRENT`. Distinct request codes prevent the two PendingIntents from being
treated as identical by the system.

### M-6: `noteOpNoThrow` records AppOp on API 28 (side effect)

**Status: ✓ FIXED**

**File:** `SystemSensorRepository.kt:182`

Replaced `noteOpNoThrow()` with `checkOpNoThrow()` (exists since API 19, only checks without recording).

---

### L-1: Broad catch blocks mask unexpected errors

**Status: ✓ FIXED** (previous audit / PR #92)

All catch blocks target specific exception types:
- `SystemSensorRepository.kt:185` — `catch (_: SecurityException)`
- `SystemSensorRepository.kt:261` — `catch (_: SecurityException)` and `catch (_: CameraAccessException)`
- `MonitoringService.kt:31` — `catch (e: IllegalStateException)`

### L-2: android:allowBackup enabled

**Status: ✓ FIXED** (previous audit)

`allowBackup="false"` and `fullBackupContent="false"` set in `AndroidManifest.xml:26-27`.

**Note:** On Android 12+, `allowBackup="false"` does NOT prevent cloud backups unless `android:dataExtractionRules` is
also specified. **✓ RESOLVED (2026-09-22)** — `android:dataExtractionRules="@xml/data_extraction_rules"` is set and the
rules file excludes root/cloud-backup and root/device-transfer (see N-15).

### L-3: Keystore password in plaintext

**Status: ✓ FIXED** (previous audit)

Pre-commit hook (`.githooks/pre-commit`) blocks `keystore.properties` commits. `.gitignore` also excludes it.

### L-4: Widget exposes sensor status on home screen

**Status: By design, no change needed.**

`android:widgetCategory="home_screen"` in `widget_info_sensor.xml:8` limits display to home screen (not lock screen).
Home screen data exposure is inherent to the app's purpose (workplace safety monitoring).

### L-5: Conditional release signing silently falls back to unsigned

**Status: ✓ FIXED** (previous audit)

`afterEvaluate` block logs warning when `keystore.properties` is missing.

### L-6: `buildConfig = true` enabled

**Status: ✓ FIXED**

**File:** `app/build.gradle.kts:67`

`buildConfig = false` is now set. Version name is read from `PackageManager` in `AppInfoDialog.kt:82` instead of
`BuildConfig.VERSION_NAME`.

### L-7: `Log.w()` calls survive in release builds

**Status: ✓ FIXED**

**File:** `app/proguard-rules.pro:1-5`

`-assumenosideeffects` now includes `public static int w(...);` in addition to `d(...)`. Both `Log.w()` call sites are
now stripped in release builds.

### L-8: Empty permission rationale callback

**Status: ✓ FIXED**

**File:** `MainActivity.kt:43`

The `ActivityResultContracts.RequestMultiplePermissions()` callback now shows a `Toast` explaining that denied
permissions will cause sensor status to be unavailable.

### L-9: `START_STICKY` on modern Android

**Status: ✓ FIXED**

**File:** `MonitoringService.kt:70`

`onStartCommand` now returns `START_REDELIVER_INTENT` instead of `START_STICKY`.

### L-10: Permission revocation not detected in background

**Status: ✓ ACKNOWLEDGED LIMITATION**

If the user revokes `CAMERA` or `RECORD_AUDIO` in Settings while the app is backgrounded, the service's sensor
observation continues showing the old state. Detection only occurs when `refresh()` is triggered (app opened or
notification "Refresh" tapped). This is documented in AGENTS.md and in-app help dialog. No client-side workaround exists
due to Android 16 AppOps server-side enforcement.

---

### N-1 (Info): Camera/mic monitoring shows "Active" based on permission/AppOps, not actual hardware usage

**Status: ✓ BY DESIGN**

See [DEVELOPMENT.md](DEVELOPMENT.md#sensor-monitoring) for the full rationale and architecture description.

---

### N-2 (Info): Deprecated `getPackageInfo(String, int)` on API 33+

**Status: ✓ FIXED**

**File:** `AppInfoDialog.kt:82-88`

Now version-gated with `PackageInfoFlags` on API 33+ with a fallback for earlier versions.

### N-3 (Info): `RECEIVER_NOT_EXPORTED` used without API version guard

**Status: ✓ FIXED**

**Files:** `SystemSensorRepository.kt:93,153`

`context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)` calls are now guarded with
`Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU` checks, falling back to the two-parameter
`registerReceiver(receiver, filter)` on API 26-32.

### N-4 (Low): Notification content exposes sensor status on lock screen

**Status: Informed — design trade-off.**

**File:** `MonitoringService.kt:98,173-174`

The persistent foreground notification displays sensor status labels (e.g., "WiFi: Active | BT: Blocked | Mic: Denied |
Cam: Active") via both `setContentText` and `BigTextStyle`. This information is visible on the lock screen and
accessible to notification listeners. In a workplace safety context, this could leak which sensors the user has
disabled.

**Options:**
- (a) Use generic notification text ("Tap to view sensor status") and keep detailed status in expanded `BigTextStyle`
- (b) Accept as by-design (status-at-a-glance is the app's purpose)

The current notification uses `PRIORITY_LOW` and `setOngoing(true)`.

### N-5 (Low): WidgetState map reference not atomically read between threads

**File:** `WidgetState.kt:19`

`WidgetState.statuses` is a `@Volatile Map` reference. `MonitoringService.pushWidgetUpdate()` writes a new map from
`Dispatchers.Main` while Glance's `provideGlance` reads it. The reference update is atomic (`mapOf` creates a new
immutable map) and `@Volatile` ensures visibility, but there is a theoretical race: the Glance thread could read a
reference whose entries contain stale `SensorStatus` objects. In practice this is negligible since `SensorStatus`
objects are immutable value objects.

**Fix (optional):** Replace with `AtomicReference` or `StateFlow` for stronger guarantees.

### N-6 (Info): Room ProGuard keep rule without Room dependency

**Status: ✓ FIXED** (PR #104)

**File:** `proguard-rules.pro:9-11`

`-keep class * extends androidx.room.RoomDatabase { <init>(); }` keeps constructors of RoomDatabase subclasses, but Room
is not in the dependency tree. This is dead configuration. Lines removed by commit `2a1b10a`.

### N-7 (Info): Pre-Tiramisu BroadcastReceivers registered without RECEIVER_NOT_EXPORTED

**File:** `SystemSensorRepository.kt:98`

On API < 33, WiFi and Bluetooth BroadcastReceivers are registered without `RECEIVER_NOT_EXPORTED` because the flag does
not exist. However, they only handle system-only broadcast actions (`WIFI_STATE_CHANGED_ACTION`,
`ACTION_STATE_CHANGED`), which third-party apps cannot send with correct signatures.

**Status:** No fix needed — platform limitation.

### N-8 (Info): Widget receivers exported without permission guard

**File:** `AndroidManifest.xml:54`

`SensorWidgetReceiver` and `CompactWidgetReceiver` are both `android:exported="true"` without `android:permission`. Any
third-party app could send `APPWIDGET_UPDATE` broadcasts, causing unnecessary processing.

**Status:** Standard widget pattern — no fix needed. DoS impact is minimal (quick processing).

### N-9 (Info): Theoretical PendingIntent request code collision with widget ID 0

**File:** `MonitoringService.kt:56`

`REQUEST_CODE_WIDGET = 0` is used for push-widget-update PendingIntents, while widget receivers use `appWidgetId` as
their request code. If the system assigns a widget ID of 0 (extremely unlikely), there would be a collision. Both
PendingIntents target `MainActivity` with the same intent structure, so impact is just a PendingIntent update.

**Fix (optional):** Use `Int.MAX_VALUE` or `100000` as `REQUEST_CODE_WIDGET` to avoid any potential collision.

### N-10 (Info): Permission denial Toast message is inaccurate for BLUETOOTH_CONNECT

**File:** `MainActivity.kt:46`

The Toast says "Camera and microphone permissions were denied" regardless of which permissions were actually denied. On
API 31+, `BLUETOOTH_CONNECT` is also requested. If the user denies only Bluetooth but grants camera/mic, the message is
misleading.

**Fix:** Update the Toast to say "Some permissions were denied. Sensor status may be unavailable." or enumerate denied
permissions.

### N-11 (Info): `@OptIn(ExperimentalCoroutinesApi)` unnecessary

**File:** `SystemSensorRepository.kt:51`

`flatMapLatest` has been stable since kotlinx-coroutines 1.6.0. The `@OptIn` annotation is visual noise.

**Fix:** Remove the annotation and import.

---

## Re-audit 2026-09-22 — Status of prior findings + new items

Re-verified against current code after PRs #146-#155/#158 (AGP 9.4.1, Gradle 9.7.1, Kotlin 2.4.20, Glance 1.2.0, JaCoCo
0.8.15). Prior findings M-1..M-6, L-1..L-9, N-1..N-3, N-6, N-7, N-8 remain resolved/by-design as documented above.
`targetSdk = 36` (bumped in PR #158) and `tools:targetApi="36"` in the manifest; E2E emulator is Android 17.

### M-7 (Medium): target SDK 35 not yet bumped for Android 16

**Status: ✓ RESOLVED (PR #158, 2026-09-22).**

**File:** `app/build.gradle.kts:42`

`targetSdk = 35` with `compileSdk = 37`. On Android 16 devices the app runs in legacy-target mode. Bumping `targetSdk`
to 36 opts into Android 16 behavior changes — including the AppOps/Mic-Cam background enforcement this app documents as
a limitation — and is required for future Play Store policy compliance. The app already handles edge-to-edge
(`enableEdgeToEdge` + `safeDrawingPadding`), which is the main visual risk of the bump. Re-run unit/E2E tests and
device-verify the Mic/Cam background behavior after the bump.

**Resolution verification (Pixel_8_Pro_Android_17, Android 17/API 37):** `targetSdk` bumped to 36; `./gradlew lint test`
clean; `connectedDebugAndroidTest` green (31 tests, 0 failed, 1 ignored, exit 0); installed APK reports `targetSdk=36
minSdk=26`. No predictive-back surface (`onBackPressed`/`BackHandler`/`KEYCODE_BACK` absent), no orientation/resize
locks, no NDK. Mic/Cam privacy-toggle flow device-verified via `QuickSettingsToggleE2eTest` (8 tests: blocked/restore
via QS toggle + polling).

### L-11 (Low): Foreground polling re-triggers all flows and re-writes widgets unconditionally

**Status: Open — efficiency/energy.**

**Files:** `MainActivity.kt:64-70`, `SystemSensorRepository.kt:44-59`, `MonitoringService.kt:88-107`

The 2s polling loop calls `viewModel.refresh()` → `refreshTrigger++`, so `flatMapLatest` cancels/restarts **all four**
sensor flows (WiFi/BT broadcast receiver churn) and the service pushes RemoteViews to both widgets plus a notification
rebuild every 2 seconds regardless of whether state changed. Only Mic/Cam are stale in the background (L-10/N-1).
Recommend a mic/cam-only refresh trigger and/or a change-guard in `pushWidgetUpdate` that skips identical status maps to
reduce wake-ups, widget writes, and notification churn.

### N-12 (Info): Glance 1.2.0 preview APIs unused

Glance 1.2.0 adds `GlanceAppWidget.providePreview`, `GlanceAppWidgetManager.setWidgetPreview`, and
`MultiProcessGlanceAppWidget`; none are used. Optional UX enhancement for the widget picker. Also note `minSdk` for
Glance 1.2.0 is 23 (app minSdk is 26) — no conflict.

### N-13 (Info): Custom `RemoteViews` layout bypasses Glance — verified current

`MonitoringService.pushWidgetUpdate()` still drives the direct `widget_sensor_status.xml` RemoteViews path and the
compact widget's `widget_compact_initial.xml` dots. Confirmed working after the Glance 1.2.0 bump (E2E
`SensorWidgetE2eTest`
+ `CompactWidgetE2eTest` pass on Android 16 emulator).

### N-14 (Info): Status held in-memory only — no persistence boundary added

Re-verified: no SharedPreferences, files, DB, network, ContentProviders, or bound services. All state lives in
`WidgetState`/`StateFlow`; backup disabled via `allowBackup="false"` + `dataExtractionRules` (excludes everything).
Attack surface unchanged from the original audit.

### N-15 (Info): Android 12+ `dataExtractionRules` confirmed present

**Status: ✓ CLOSED** (was L-2 note) — `android:dataExtractionRules="@xml/data_extraction_rules"` is set and the rules
file excludes `root` for both cloud-backup and device-transfer.

### N-16 (Info): Dependabot alerts re-checked

Both previously-dismissed Bouncy Castle alerts (`bcprov-jdk18on`, build-time transitive via `settings.gradle.kts`) were
re-examined — no new open alerts as of 2026-09-22.

### N-17 (Info): Android 17 emulator reports `@Ignore`'d class as empty `null` JUnit failure

**Status: By design — cosmetic, no impact.**

On the `Pixel_8_Pro_Android_17` AVD (Android 17 / API 37), `connectedDebugAndroidTest` is **green**: the runner logcat
says `31 tests, 0 failed, 1 ignored` and the exit code is 0. The Android 17 instrumentation serializes the class-level
`@Ignore` on `PermissionRefreshE2eTest` into the JUnit XML as `<testcase name="null"><failure/></testcase>` (empty
body), inflating the XML `failures` count to 1. This is unrelated to the app — the intentionally-ignored test never
executes (its `pm revoke` would kill the instrumentation process, which is exactly why it is `@Ignore`d). The runner
exit code governs `connectedDebugAndroidTest` success, so no build impact.

---

## Attack Surface Summary

| Vector | Present? | Notes |
|--------|----------|-------|
| Network calls          | No | No HTTP, WebSocket, or network I/O |
| Local storage          | No | No files, DB, or SharedPreferences |
| ContentProviders       | No | None declared |
| BroadcastReceivers     | Yes | `SensorWidgetReceiver` + `CompactWidgetReceiver` (exported, widget framework only); WiFi/BT receivers context-registered with `RECEIVER_NOT_EXPORTED` on API 33+ |
| Bound services         | No | `onBind` returns null |
| WebViews               | No | None used |
| Deep links             | No | No intent filters matching URLs |
| FileProvider           | No | None declared |
| Third-party SDKs       | Low | Jetpack/AndroidX only — official Google libraries |
| Runtime permissions    | Complete | CAMERA + RECORD_AUDIO + BLUETOOTH_CONNECT (API 31+) all requested at runtime |
| Implicit intents       | None | All intents use explicit component names |
| PendingIntents         | Secure | All use `FLAG_IMMUTABLE`; request codes are distinct |
| Notification           | Low | Sensor status visible on lock screen (see N-4); uses `PRIORITY_LOW`; stripped by ProGuard in release |
| AppOps checking        | Low | `unsafeCheckOpNoThrow` on API 29+; `checkOpNoThrow` fallback on API 19-28 |
| Persistent storage     | None | No SharedPreferences, no files, no database — all state is in-memory |

---

## Recommendations Priority

| Priority | Issue |
|----------|-------|
| Medium   | ~~M-7: Bump `targetSdk` to 36~~ **RESOLVED (PR #158)** — see M-7 section for verification |
| Low      | L-11: Add change-guard to `pushWidgetUpdate` and/or mic/cam-only refresh to stop 2s widget/notification write churn |
| Low      | N-4: Consider generic notification text to reduce lock-screen exposure |
| Low      | N-5: Optional hardening of WidgetState with AtomicReference |
| Info     | N-9: Consider increasing REQUEST_CODE_WIDGET to avoid theoretical collision |
| Info     | N-10: Fix permission denial toast message to be accurate |
| Info     | N-11: Remove unnecessary `@OptIn(ExperimentalCoroutinesApi)` |

---

## Files Examined

| File | Lines |
|------|-------|
| `app/build.gradle.kts` | 143 |
| `AndroidManifest.xml` | 76 |
| `service/MonitoringService.kt` | 208 |
| `data/repository/SystemSensorRepository.kt` | 267 |
| `data/repository/SensorRepository.kt` | 22 |
| `data/model/SensorStatus.kt` | 41 |
| `ui/view/MainActivity.kt` | 100 |
| `ui/view/StatusDashboard.kt` | 138 |
| `ui/view/AppInfoDialog.kt` | 278 |
| `ui/view/EzWorkSafeTheme.kt` | 42 |
| `ui/viewmodel/SensorViewModel.kt` | 53 |
| `util/PermissionHelper.kt` | 43 |
| `util/FormatUtils.kt` | 13 |
| `widget/SensorWidget.kt` | 159 |
| `widget/SensorWidgetReceiver.kt` | 51 |
| `widget/CompactWidget.kt` | 123 |
| `widget/CompactWidgetReceiver.kt` | 49 |
| `widget/BuildWidgetRemoteViews.kt` | 59 |
| `widget/WidgetState.kt` | 25 |
| `widget/WidgetColors.kt` | 20 |
| `EzWorkSafeApp.kt` | 20 |
| `res/layout/widget_initial_layout.xml` | 171 |
| `res/layout/widget_sensor_status.xml` | 174 |
| `res/layout/widget_compact_initial.xml` | 124 |
| `res/xml/widget_info_sensor.xml` | 9 |
| `res/xml/widget_info_compact.xml` | 9 |
| `res/xml/data_extraction_rules.xml` | 9 |
| `proguard-rules.pro` | 8 |
| `.github/workflows/android.yml` | 85 |
| `.github/workflows/e2e.yml` | 52 |
| `keystore.properties.template` | 6 |
| `.githooks/pre-commit` | 8 |
| `.gitignore` | 15 |
| `gradle.properties` | 6 |
| `settings.gradle.kts` | 21 |
| Test files (18 files across `src/test/`, `src/testShared/`, `src/androidTest/`) | ~1,300 |
