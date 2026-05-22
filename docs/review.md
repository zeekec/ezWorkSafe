# Code & Documentation Review

**Date:** 2026-05-22
**Last updated:** 2026-05-22 (session 5)
**Commit:** `27ac322` (HEAD of `main`)
**Review scope:** Full codebase, tests, config, docs, CI, security

---

## Architecture

MVVM with clear separation of concerns. Interface-based `SensorRepository` + `SystemSensorRepository` implementation.
`callbackFlow` + `flatMapLatest` pattern for reactive sensor observations is idiomatic Kotlin coroutines.

The `WidgetState` singleton is a pragmatic choice for cross-component state sharing between the service and Glance
widgets. The `lastRefreshTime` is set by `MonitoringService.observeSensors()` alongside the `WidgetState.statuses` write,
keeping the data layer free of widget dependencies.

---

## Strengths

### Code quality
- Clean MVVM with interface-based repository pattern
- Well-chosen `callbackFlow` + `flatMapLatest` for reactive sensor observations
- `FakeSensorRepository` enables deterministic E2E and unit tests
- `FormatUtils.formatLastUpdated()` extracted as pure-Kotlin function — testable without Android mocking
- `isOpBlocked()` extracted as pure function
- `buildWidgetRemoteViews()` extracted as top-level testable function
- Permission helper is version-aware (`BLUETOOTH_CONNECT` on API 31+)
- ProGuard strips `Log.d` and `Log.w` via `-assumenosideeffects`
- `WidgetColors.kt` centralizes widget colors (eliminates duplication)
- `STOP_TIMEOUT_MILLIS` named constant replaces magic number `5_000`
- No dead imports, no unused resources
- `FakeSensorRepository` consolidated in `testShared` source set (PR #60)
- Configuration cache: `./gradlew build` in < 1s on cache hit
- `SensorStatus.Blocked` state distinguishes hardware-off from denied
- `AppInfoDialog` uses `PackageManager.PackageInfoFlags` on API 33+ (API.md N-2 resolved)
- Previous review issues fixed: `CellIds` dead code removed, `@OptIn(ExperimentalCoroutinesApi)` retained where needed,
  `isWifiEnabled` replaced with `getWifiState()`, widget colors extracted to `WidgetColors.kt`,
  `5_000` extracted to `STOP_TIMEOUT_MILLIS`, layer violation moved to service, unused import removed

### UI
- Edge-to-edge display with `safeDrawingPadding()`
- Dark/light mode via Material You dynamic colors (API 31+) and seeded fallback
- About & Help sheet with expandable sections, documents widget Mic/Cam limitation
- Custom widget initial layout replaces 45s Glance blank loading state
- Triple-path widget click handler (initial XML, RemoteViews push, Glance Compose)
- Widget vertical centering fix applied (FrameLayout overlay approach)
- AppInfoDialog with expandable sections for About, How It Works, Permissions

### Testing
- Unit tests: ViewModel, Repository (System + Fake), WidgetState, FormatUtils, PermissionHelper, MonitoringService, widget receivers, Robolectric flow integration
- E2E tests: dashboard (sensor states), widget metadata, notification via `dumpsys`, theme, compact widget — 1 @Ignored
- `FakeSensorRepository` shared between unit and E2E tests via `testShared` source set
- `SystemSensorRepositoryFlowsTest.kt` uses Robolectric for real system-service integration
- `SensorRepositoryTest.kt` tests `isOpBlocked()` pure function exhaustively (5 test methods)
- E2E notification verification via `dumpsys activity services`

### Security & compliance
- `allowBackup="false"`, `fullBackupContent="false"`
- Pre-commit hook blocks `keystore.properties` commits
- `afterEvaluate` warning when keystore missing
- Dependabot alerts dismissed as build-only transitive deps
- Apache 2.0 license with SPDX headers on all Kotlin files
- All runtime deps Apache 2.0; test-only deps MIT/EPL/BSD
- No network calls, no persistent storage — minimal attack surface

### Documentation
- AGENTS.md: comprehensive build/test commands, architecture, gotchas, Dependabot, emulator commands
- PLAN.md: full implementation plan with post-plan additions
- API.md: detailed API reference with docs links
- review.md: this file
- security.md: security audit
- widget_spacing.md: detailed widget centering analysis

---

## Issues Found

### Medium

~~**1. Missing Glance ProGuard keep rules**~~
- ~~File: `app/proguard-rules.pro`~~
- ~~ProGuard only strips `Log.d()` calls. Glance components (`SensorWidget`, `SensorWidgetReceiver`) are referenced via
  reflection by the Glance framework for WorkManager-based initial render. Without `-keep` rules, release builds may
  crash or render blank widgets.~~
- **Status: ✓ FIXED** — Added `-keep class com.ezworksafe.widget.** { *; }` to `proguard-rules.pro` (PR #43).

~~**2. `foregroundServiceType` may be incorrect**~~
- ~~File: `AndroidManifest.xml:45`, `MonitoringService.kt`~~
- ~~Service uses `foregroundServiceType="dataSync"` but the service monitors sensor state, not data synchronization. On
  Android 14+, the system may restrict services whose declared type doesn't match their actual work.~~
- **Status: ✓ FIXED** — Changed to `specialUse` with `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` declaration (PR #41).

~~**3. `buildWidgetRemoteViews()` in wrong package**~~
- ~~File: `app/src/main/java/com/ezworksafe/service/MonitoringService.kt:148-186`~~
- ~~The top-level `buildWidgetRemoteViews()` function is defined in the `service` package but belongs in the `widget`
  package. It manipulates `widget_sensor_status.xml` IDs and uses `R.layout.widget_sensor_status` — widget rendering
  logic.~~
- ~~**Fix:** Move to `app/src/main/java/com/ezworksafe/widget/BuildWidgetRemoteViews.kt`~~
- **Status: ✓ FIXED** — Moved to `BuildWidgetRemoteViews.kt` in widget package (PR #39).

~~**4. PendingIntent request code collision**~~
- ~~File: `app/src/main/java/com/ezworksafe/service/MonitoringService.kt:101,120`~~
- ~~Both `pushWidgetUpdate()` (widget click) and `createNotification()` (notification "Refresh" action) use
  `PendingIntent.getActivity(this, 0, ...)` with the same request code (0). Since both intents target `MainActivity` with
  the same component (no action/data differences), `Intent.filterEquals()` returns true for both, causing the system to
  treat them as the same PendingIntent. The last one created overwrites the first, making the launch behavior of widget
  click and notification action identical.~~
- ~~**Risk:** No direct security exploit (both use `FLAG_IMMUTABLE`), but intent flag differences (
  `FLAG_ACTIVITY_NEW_TASK` vs `FLAG_ACTIVITY_SINGLE_TOP`) are lost.~~
- ~~**Fix:** Use distinct request codes (e.g., `0` for widget, `1` for notification).~~
- **Status: ✓ FIXED** — Distinct request codes (`REQUEST_CODE_WIDGET = 0`, `REQUEST_CODE_REFRESH = 1`).

### Medium

~~**14. `RECEIVER_NOT_EXPORTED` used without API version guard**~~
- ~~File: `SystemSensorRepository.kt:74,120`~~
- ~~`registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)` on API 26-32 (`minSdk = 26`) silently ignores the flag (`0x4` is unrecognized). The receiver is exported and any app can send spoofed WiFi/BT state broadcasts.~~
- **Status: ✓ FIXED** — Both WiFi and Bluetooth calls now guarded with `Build.VERSION.SDK_INT >= TIRAMISU` checks.

~~**15. `PermissionHelper.REQUIRED_RUNTIME_PERMISSIONS` re-allocates array on every access**~~
- ~~File: `PermissionHelper.kt:14-15`~~
- ~~`val` with custom getter creates a new list + `toTypedArray()` on each access. Called every time `areRuntimePermissionsGranted()` runs.~~
- **Status: ✓ FIXED** — Changed to `by lazy` delegation.

### Low

~~**5. `BuildConfig` enabled for release builds**~~
- ~~File: `app/build.gradle.kts:51`~~
- ~~`buildConfig = true` exposes`BuildConfig.DEBUG` and`BuildConfig.VERSION_NAME` . Not used for security decisions (only
  `VERSION_NAME` in`AppInfoDialog` ), but disabling reduces attack surface.~~
- **Status: ✓ FIXED** (this PR)

~~**6. `Log.w()` calls survive in release builds**~~
- ~~Files: `SystemSensorRepository.kt:200`, `SensorWidgetReceiver.kt:26`~~
- ~~ProGuard strips`Log.d()` only.`Log.w()` calls survive in release builds. Content is non-sensitive (camera error + FGS
  restriction), but noisy.~~
- **Status: ✓ FIXED** (this PR)

~~**7. Empty permission rationale callback**~~
- ~~File: `MainActivity.kt:28-29`~~
- ~~`requestPermissionLauncher` callback body is empty. If user denies permissions, no rationale or re-prompt is shown.~~
- **Status: ✓ FIXED** (this PR)

~~**8. `START_STICKY` on modern Android**~~
- ~~File: `MonitoringService.kt:52-54`~~
- ~~On Android 14+, `START_STICKY` restart behavior is restricted — the system may delay or not restart the service.~~
- **Status: ✓ FIXED** — Changed to `START_REDELIVER_INTENT` (this PR)

~~**9. Permission revocation unnoticed while backgrounded**~~
- ~~If user revokes`CAMERA` or`RECORD_AUDIO` in Settings while app is in background, the service won't detect it until
  `refresh()` is triggered (app opened or notification "Refresh" tapped).~~
- **Status: ✓ FIXED** — Documented as known limitation in AGENTS.md (Android 16 AppOps restriction).

**10. Test quality issues**
- `SensorViewModelTest.kt`: `assertNotNull` on StateFlows replaced with behavioral assertions (PR #64) — partially fixed, `WhileSubscribed` lazy subscription limits synchronous verification.
- `MonitoringServiceTest.kt:129-134`: `bigContentView` assertion may behave differently under Robolectric vs. framework.
- `StatusDashboardE2eTest.kt:64-77`: `waitForAssertion` re-throws last error, losing original stack trace from failing assertion.
- `MonitoringServiceNotificationE2eTest.kt:42-45`: `FileInputStream(pfd.fileDescriptor).use {}` then `pfd.close()` — if read fails, `ParcelFileDescriptor` leaks. Should wrap in `pfd.use {}`.
- ~~`WidgetStateLabelTest.kt`: Entirely redundant with `WidgetStateTest.kt` and `SensorStatusTest.kt`.~~ **Removed from codebase.**

~~**11. README emulator command has race condition**~~
- ~~File: `README.md:75-77`~~
- ~~`android emulator start Pixel_8_Pro &` followed by`./gradlew :app:connectedDebugAndroidTest` — no`wait-for-device` or
  boot check between emulator start and test execution.~~
- **Status: ✓ FIXED** (PR #45)

### Info

| # | Finding | File | Description |
|---|---------|------|-------------|
| 1 | `5_000` magic number | `SensorViewModel.kt:23-28` | Extract `WhileSubscribed` timeout to named constant | ✓ Fixed — `STOP_TIMEOUT_MILLIS` (PR #63) |
| 2 | Color constants duplicated | `MonitoringService.kt:156-158`, `SensorWidget.kt:50,75,85,109` | Same 5 widget colors appear as raw integers in 2 files | ✓ Fixed — `WidgetColors.kt` |
| 3 | Camera/mic emit ignores callback data | `SystemSensorRepository.kt:150-209` | `emitState()` checks permission + AppOps, ignores callback data about actual hardware usage | By design — app reports whether hardware *can* be used, not whether it's currently in use |
| 4 | `Arrangement.spacedBy(12.dp)` | `StatusDashboard.kt:67` | Magic number for spacing |
| 5 | CI doesn't run E2E | `.github/workflows/android.yml:49` | Only `build` + `lint` + `test`. E2E skipped intentionally for cost. |
| 6 | CI secrets in build log | `.github/workflows/android.yml:37-38` | `STORE_PASSWORD`/`KEY_ALIAS`/`KEY_PASSWORD` written to file via `echo` |
| 7 | Gradle constraint warnings suppressed | `gradle.properties:5` | `generateSyncIssueWhenLibraryConstraintsAreEnabled=false` hides useful diagnostics |
| 8 | Camera status detection fragility | `SystemSensorRepository.kt:187` | `getCameraCharacteristics()` may succeed even when camera is in use; `AvailabilityCallback` `onCameraUnavailable` is more reliable |
| 9 | Pre-commit hook scope narrow | `.githooks/pre-commit` | Only checks `keystore.properties` — doesn't catch `.env`, `release.keystore`, etc. |
| 10 | `PermissionRefreshE2eTest` is `@Ignored` | E2E test | Correctly ignored (API 36 shell restriction), but compiles and is never run |
| 12 | testShared sourceSet config | `app/build.gradle.kts:61-65` | Task name pattern matching is fragile | Alternative: `android.sourceSets` |

### New This Session

#### Medium

~~**16. `noteOpNoThrow` side effect on API 28**~~
- ~~File: `SystemSensorRepository.kt:150`~~
- ~~On API 28 (`Build.VERSION_CODES.P`), the else branch called `appOps.noteOpNoThrow()` which *recorded* the AppOp as having been performed (polluting the AppOps usage history / permission usage screen). `checkOpNoThrow()` exists since API 19 and only checks without recording.~~
- **Status: ✓ FIXED** — Replaced `noteOpNoThrow` with `checkOpNoThrow` (PR #91).

**17. Exception-based control flow in camera status**
- File: `SystemSensorRepository.kt:215`
- `cameraManager.getCameraCharacteristics(ids[0])` is called purely for its side effect of possibly throwing. The return value is discarded. This is a control-flow-via-exception anti-pattern.
- **Fix:** Use `tryGetCameraCharacteristics()` or check `onCameraUnavailable` callback data instead.

#### Low

**18. Redundant SDK guard in `isOpBlocked()`**
- File: `SystemSensorRepository.kt:246-247`
- `isOpBlocked(sdk, opResult)` checks `sdk >= Build.VERSION_CODES.P && ...` but the only caller `isAppOpBlocked()` already returns `false` on line 144 when `sdk < P`. The SDK guard is dead code.
- **Fix:** Remove the SDK check from `isOpBlocked()`.

**19. `SensorStatus.Inactive` never emitted by any flow**
- File: `SensorStatus.kt:17`
- `Inactive` is defined as a valid status but never produced by `observeWifiStatus()`, `observeBluetoothStatus()`, `observeMicStatus()`, or `observeCameraStatus()`. It appears only as the default initial value in `WidgetState.kt:12`.
- **Fix:** Either remove `Inactive` and use another default (e.g., `Unavailable`), or document as placeholder-only.

**20. Redundant Glance modifier chaining**
- File: `SensorWidget.kt:76-77, 86-88, 110-111`
- `.width(1.dp)` followed by `.size(1.dp, 30.dp)` — the `.width()` is overridden by `.size()`. Same pattern at lines 86-88 (`.width(2.dp)` before `.size(2.dp, 40.dp)`).
- **Fix:** Remove the redundant `.width()` calls.

**21. Unsafe cast in MonitoringService**
- File: `MonitoringService.kt:68`
- `(application as EzWorkSafeApp)` will throw `ClassCastException` if the Application is not `EzWorkSafeApp`. Works in production (manifest guarantees it) but fragile in Robolectric tests with custom Application classes.
- **Fix:** Use `(application as? EzWorkSafeApp)?.sensorRepository` with a fallback.

**22. WidgetState singleton has no encapsulation**
- File: `WidgetState.kt:9-15`
- `object WidgetState` exposes public `@Volatile var` fields. Nothing prevents third-party code from writing to `WidgetState.statuses` from any thread.
- **Fix:** Make fields `internal` or wrap in `AtomicReference` / `StateFlow`.

**23. Duplicate receiver click-handler code**
- Files: `SensorWidgetReceiver.kt:30-41`, `CompactWidgetReceiver.kt:30-41`
- Both receivers have identical `onUpdate` logic differing only in the layout resource used (`widget_initial_layout` vs `widget_compact_initial`).
- **Fix:** Extract shared logic into a helper function.

**24. Test: PFD leak in notification E2E**
- File: `MonitoringServiceNotificationE2eTest.kt:39-46`
- `executeShellCommand` returns a `ParcelFileDescriptor`. If `FileInputStream.use { readText() }` throws, the `pfd.close()` on line 45 is never reached. Also, closing the `FileInputStream` closes the underlying FD before `pfd.close()` runs.
- **Fix:** Wrap the entire block in `pfd.use { pfd -> ... }`.

**25. Test: Ambiguous matchers in StatusDashboardE2eTest**
- File: `StatusDashboardE2eTest.kt:88,101`
- `onNodeWithText("Active")` matches ANY composable with text "Active". If two sensors show "Active" simultaneously, this crashes with `AmbiguousViewMatcherException`.
- The `microphone_shows_active` test (line 132-134) correctly uses `hasAnySibling(hasText("Microphone"))` to disambiguate. The `wifi_toggles_between_active_and_blocked` and `bluetooth_active_updates_ui` tests should follow this pattern.

**26. Test: WidgetState singleton not reset between E2E classes**
- File: `SensorWidgetE2eTest.kt:68-74`
- The test modifies `WidgetState.statuses` but does not reset it in `@Before`. Since E2E tests share a process, `WidgetState` state leaks across test classes.
- **Fix:** Add `@Before` reset (or use `@BeforeClass` teardown).

**27. Test: Refresh-reemission test is tautological**
- File: `SensorRepositoryTest.kt:64-71`
- `refresh triggers flow re-emission` uses a null-service mock context, so both `first` and `afterRefresh` return `Unavailable`. The test name claims re-emission is verified, but the assertion is tautological.
- **Fix:** Use `SystemSensorRepositoryFlowsTest.kt:148-159` (which actually changes state) instead; remove the tautological test.

**28. Test: Widget rendering not actually tested**
- File: `SensorWidgetE2eTest.kt:67-80`
- Test name says "widget renders" but only checks `WidgetState.statuses` label strings. No RemoteViews layout inflation or rendering verification occurs.
- **Fix:** Rename test to match what it actually tests, or add RemoteViews inflation assertions.

**29. Test: buildWidgetRemoteViews assertions are shallow**
- File: `MonitoringServiceTest.kt:33-99`
- Four tests only assert `assertNotNull(views)` on buildWidgetRemoteViews output. No RemoteViews content (text values, colors, click handlers) is verified.
- **Fix:** Add RemoteViews content assertions or parameterize.

### Info

| # | Finding | File | Description |
|---|---------|------|-------------|
| 13 | `noteOpNoThrow` side effect (API 28) | `SystemSensorRepository.kt:150` | ✓ FIXED — use `checkOpNoThrow()` instead | See Medium #16 |
| 14 | Exception-based camera control flow | `SystemSensorRepository.kt:215` | `getCameraCharacteristics()` called only to throw; return value discarded | See Medium #17 |
| 15 | Redundant SDK guard in `isOpBlocked` | `SystemSensorRepository.kt:246-247` | Caller already guarantees `sdk >= P`; check is dead code | See Low #18 |
| 16 | `Inactive` never emitted | `SensorStatus.kt:17` | Placeholder-only status, may confuse developers | See Low #19 |
| 17 | Glance modifier redundancy | `SensorWidget.kt:76-77,86-88,110-111` | `.width()` overridden by `.size()` | See Low #20 |
| 18 | Unsafe cast in MonitoringService | `MonitoringService.kt:68` | `as EzWorkSafeApp` crashes on custom Application in tests | See Low #21 |
| 19 | WidgetState no encapsulation | `WidgetState.kt:9-15` | Public mutable fields, no concurrency protection | See Low #22 |
| 20 | Duplicate receiver click-handler code | `SensorWidgetReceiver.kt:30-41`, `CompactWidgetReceiver.kt:30-41` | Same logic, different layout resource | See Low #23 |
| 21 | PFD leak in notification E2E | `MonitoringServiceNotificationE2eTest.kt:42-45` | `pfd.close()` not reached if read throws | See Low #24 |
| 22 | Ambiguous "Active" matcher | `StatusDashboardE2eTest.kt:88,101` | `onNodeWithText("Active")` matches any sensor | See Low #25 |
| 23 | WidgetState not reset in E2E | `SensorWidgetE2eTest.kt:68-74` | Singleton state leaks across E2E test classes | See Low #26 |
| 24 | Tautological refresh-reemission test | `SensorRepositoryTest.kt:64-71` | Same value before and after refresh (null-service) | See Low #27 |
| 25 | buildWidgetRemoteViews tests shallow | `MonitoringServiceTest.kt:33-99` | 4 tests, all only check `assertNotNull` | See Low #29 |

### Fixed Since Last Review

| Previous Issue | Status |
|----------------|--------|
| `CellIds` dead code in `MonitoringService` | ✓ Removed |
| `@OptIn(ExperimentalCoroutinesApi::class)` on `flatMapLatest` | Still needed — `flatMapLatest` requires opt-in in kotlinx-coroutines 1.11.0 |
| `WifiManager.isWifiEnabled` deprecated | ✓ Using `getWifiState()` |
| README stale `targetSdk: 33` reference | ✓ Updated |
| Widget vertical centering | ✓ Fixed (FrameLayout overlay approach) |
| Data-layer-to-widget-layer dependency (`WidgetState` in repo) | ✓ Moved to `MonitoringService.observeSensors()` (PR #37) |
| Unused `height` import in `StatusDashboard.kt:15` | ✓ Removed |
| `foregroundServiceType="dataSync"` incorrect | ✓ Changed to `specialUse` with `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` (PR #41) |
| Missing Glance ProGuard keep rules | ✓ Added `-keep class com.ezworksafe.widget.**` to `proguard-rules.pro` (PR #43) |
| `buildWidgetRemoteViews()` in wrong package | ✓ Moved to `BuildWidgetRemoteViews.kt` in widget package (PR #39) |
| PendingIntent request code collision | ✓ Distinct request codes (`REQUEST_CODE_WIDGET = 0`, `REQUEST_CODE_REFRESH = 1`) |
| `FakeSensorRepository` duplicated in test/testShared/androidTest | ✓ Consolidated to `src/testShared/java` (PR #60, fixes #50) |
| Widget enum iteration uses positional `take(2)`/`drop(2)` | ✓ Replaced with `filterKeys` + `wifiBtSensors`/`micCamSensors` sets (PR #61, fixes #56) |
| `RECEIVER_NOT_EXPORTED` flag missing | ✓ Added to WiFi + BT `registerReceiver()` calls (PR #62, fixes #52) |
| `WhileSubscribed(5_000)` magic number | ✓ Extracted to `STOP_TIMEOUT_MILLIS` named constant (PR #63, fixes #55) |
| `SensorViewModelTest` uses Mockito `verify` | ✓ Replaced with `FakeSensorRepository` + behavioral assertions (PR #64, fixes #53) |
| `PermissionHelper.REQUIRED_RUNTIME_PERMISSIONS` array re-allocation | ✓ Changed to `by lazy` delegation (see Medium issue #15) |
| docs: stale references in API.md, PLAN.md, review.md | ✓ Updated `targetSdk`, BOM, lifecycle versions, `Blocked` state, `shortName`, corrected `@OptIn` claim, marked review issues #3/#4 fixed (PR #65, fixes #54) |
| `WidgetStateLabelTest.kt` redundant test | ✓ Removed from codebase |
| `AppInfoDialog.kt` deprecated `getPackageInfo()` (security.md N-2) | ✓ Fixed — version-gated with `PackageInfoFlags` on API 33+ |

---

## Test Coverage Gaps

| Area | Status |
|------|--------|
| `MonitoringService.combine` collector integration | Untested — requires Robolectric with service lifecycle |
| `pushWidgetUpdate()` RemoteViews content | 5 tests verify non-null, none check actual text/color values |
| `SystemSensorRepository` system-service flow re-emission | `SensorRepositoryTest.kt:64` is tautological (null-service); real test exists at `SystemSensorRepositoryFlowsTest.kt:148` |
| `PermissionRefreshE2eTest` | `@Ignored` — `executeShellCommand` crashes on API 36 |
| Widget `AppOps` foreground/background behavior | Untestable without API 36+ device with specific AppOps config |
| `WidgetState` singleton — shared mutable state | Not reset between E2E test classes; leaks across classes |

---

## Documentation Gaps

| Gap | Notes |
|-----|-------|
| CI doesn't run E2E tests | Documented gap — intentional for cost |
 | ~~`foregroundServiceType` rationale undocumented~~ | ✓ Documented via `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` (PR #41) |

---

## Build & CI Health

| Check | Status |
|-------|--------|
| `./gradlew build` | ✅ Passes |
| `./gradlew lint` | ✅ Clean (no warnings) |
| `./gradlew test` | ✅ All unit tests pass |
| `./gradlew connectedDebugAndroidTest` | ✅ E2E tests pass (1 @Ignored) |
| GitHub Actions workflow | ✅ `permissions: contents: read`, guarded keystore steps |
| Dependabot config | ✅ Weekly Gradle scanning |
| Configuration cache | ✅ Enabled, <1s build on cache hit |

---

## Summary

The project is in strong shape. The architecture is clean, security posture is sound, and documentation is
comprehensive. Key findings this session:

- **1 Medium:** Exception-based control flow in camera status detection.
  ~~`noteOpNoThrow` side effect on API 28~~ **(✓ FIXED)**
- **12 Low:** Redundant SDK guard, `Inactive` never emitted, redundant Glance modifiers, unsafe cast in
  MonitoringService, WidgetState encapsulation, duplicate receiver code, PFD leak in E2E test, ambiguous test
  matchers, WidgetState not reset in E2E, tautological refresh test, misleading widget-rendering test,
  shallow RemoteViews assertions.
- **All findings from previous review sessions remain resolved.** No regressions in previously fixed areas.