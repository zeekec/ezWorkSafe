# Code & Documentation Review

**Date:** 2026-05-25
**Last updated:** 2026-05-27 (session 6 — comprehensive re-review, PR #104 updates)
**Commit:** HEAD of `main`
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
- Unit tests: 56 tests across ViewModel, Repository (System + Fake), WidgetState, FormatUtils, PermissionHelper, MonitoringService, widget receivers, Robolectric flow integration
- E2E tests: 32 tests across dashboard (sensor states), widget metadata, notification via `dumpsys`, theme, compact widget, quick settings toggle — 1 @Ignored
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

### Low (previously identified, still open)

**18. Redundant SDK guard in `isOpBlocked()`**
- File: `SystemSensorRepository.kt:274-275`
- `isOpBlocked(sdk, opResult)` checks `sdk >= Build.VERSION_CODES.P`, but the only production caller `isAppOpBlocked()` (line 175) already returns `false` for SDK < P. The SDK check is redundant for all in-app call paths.
- **Fix:** Remove the SDK check from `isOpBlocked()`.

**19. `SensorStatus.Inactive` never emitted by any flow**
- File: `SensorStatus.kt:27`
- `Inactive` is defined as a valid status but never produced by any sensor flow. It appears only as the default initial value in `WidgetState.kt:21`.
- **Fix:** Either remove `Inactive` and use another default (e.g., `Unavailable`), or document as placeholder-only.

**20. Redundant Glance modifier chaining**
- File: `SensorWidget.kt:84-86, 94-96, 118-120`
- `.width(1.dp)` followed by `.size(1.dp, 30.dp)` — the `.width()` is overridden by `.size()`. Same pattern at lines 94-96 and 118-120.
- **Fix:** Remove the redundant `.width()` calls.

**21. Unsafe cast in MonitoringService**
- File: `MonitoringService.kt:82`
- `(application as EzWorkSafeApp)` will throw `ClassCastException` if the Application is not `EzWorkSafeApp`. Works in production (manifest guarantees it) but fragile in Robolectric tests with custom Application classes.
- **Fix:** Use `(application as? EzWorkSafeApp)?.sensorRepository` with a fallback.

**22. WidgetState singleton has no encapsulation**
- File: `WidgetState.kt:18-25`
- `object WidgetState` exposes public `@Volatile var` fields. Nothing prevents third-party code from writing to `WidgetState.statuses` from any thread.
- **Fix:** Make fields `internal` or wrap in `AtomicReference` / `StateFlow`.

**23. Duplicate receiver click-handler code**
- Files: `SensorWidgetReceiver.kt:30-41`, `CompactWidgetReceiver.kt:30-41`
- Both receivers have identical `onUpdate` logic differing only in the layout resource used. AGENTS.md explicitly documents this as intentional non-refactoring.
- **Status:** By design per AGENTS.md.

**24. PFD leak in notification E2E**
- File: `MonitoringServiceNotificationE2eTest.kt:39-45`
- `ParcelFileDescriptor` may leak if `FileInputStream` constructor throws before `pfd.close()` is reached.
- **Fix:** Wrap the entire block in `pfd.use { pfd -> ... }`.

**25. Ambiguous matchers in StatusDashboardE2eTest**
- File: `StatusDashboardE2eTest.kt:88,101`
- `onNodeWithText("Active")` matches ANY composable with text "Active". If two sensors show "Active" simultaneously, this crashes with `AmbiguousViewMatcherException`.
- **Fix:** Use `hasAnySibling(hasText("SensorName"))` for disambiguation, as done in the mic/cam tests.

**26. WidgetState singleton not reset between E2E classes**
- File: `SensorWidgetE2eTest.kt`
- No `@Before` reset of `WidgetState`. Since E2E tests share a process, `WidgetState` state leaks across test classes.
- **Fix:** Add `@Before` reset (or use `@BeforeClass` teardown).

**27. Tautological refresh-reemission test**
- File: `SensorRepositoryTest.kt:63-71`
- `refresh triggers flow re-emission` uses a null-service mock context, so both `first` and `afterRefresh` return `Unavailable`. The test name claims re-emission is verified, but the assertion is tautological.
- **Fix:** Use `SystemSensorRepositoryFlowsTest.kt:148-159` (which actually changes state) instead; remove the tautological test.

**28. Widget rendering test misleading**
- File: `SensorWidgetE2eTest.kt:68-80`
- Test named `widgetState_can_be_updated_and_widget_renders` only checks `WidgetState.statuses` label strings. No RemoteViews layout inflation or rendering verification occurs.
- **Fix:** Rename test to match what it actually tests, or add RemoteViews inflation assertions.

**29. buildWidgetRemoteViews assertions are shallow**
- File: `MonitoringServiceTest.kt:33-99`
- Four tests only assert `assertNotNull(views)` on buildWidgetRemoteViews output. No RemoteViews content (text values, colors, click handlers) is verified.
- **Fix:** Add RemoteViews content assertions via `ShadowRemoteViews`.

### New This Session (2026-05-25)

#### Medium

**30. Unused `context` parameter in `WidgetContent` composable**
- File: `SensorWidget.kt:57`
- The `context: Context` parameter is passed from `provideGlance` (line 51) but never referenced in the function body. Dead code that increases cognitive load.
- **Fix:** Remove the `context` parameter from `WidgetContent()` and the `context = context` argument at the call site. If context is needed later, use `LocalContext.current`.

**31. Aggressive 2-second polling loop in MainActivity**
- File: `MainActivity.kt:64-71`
- The `while(true)` loop calls `viewModel.refresh()` every 2 seconds while in `Lifecycle.State.STARTED`. This triggers re-subscription of all four sensor flows via `flatMapLatest`, impacting battery life. Mic/Cam are snapshot-only and won't change without foreground interaction.
- **Fix:** Increase the polling interval to 10-30 seconds, or remove the loop entirely since `ON_RESUME` already does a single refresh. The stated purpose (Android 16 AppOps workaround) cannot be fixed by foreground polling.

**32. Unused `audioManager` variable in `observeMicStatus()`**
- File: `SystemSensorRepository.kt:203`
- The `audioManager` variable is assigned via `context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager`, null-checked, but never referenced again after the null-check block. The service lookup is only used as a null-probe.
- **Fix:** Replace with `val hasAudioService = context.getSystemService(Context.AUDIO_SERVICE) != null` or remove the AudioManager lookup entirely.

#### Low

**33. Inconsistent flow termination in Mic/Cam callbackFlows**
- Files: `SystemSensorRepository.kt:206, 241`
- Error paths in `observeMicStatus()` and `observeCameraStatus()` use explicit `close()` calls, while normal paths use `awaitClose { }`. The `close()` calls skip the cleanup callback.
- **Fix:** Replace `close()` with `awaitClose { }` for consistency.

**34. `for` loop with `continue` in `BuildWidgetRemoteViews`**
- File: `BuildWidgetRemoteViews.kt:44`
- Uses traditional `for` loop with `continue` statement instead of idiomatic Kotlin `.forEach {}`. Functional style is used throughout the rest of the codebase.
- **Fix:** Replace with `statuses.forEach { (type, status) -> ... }` using `return@forEach` for skip.

**35. Unused import in MonitoringServiceNotificationE2eTest**
- File: `MonitoringServiceNotificationE2eTest.kt:7`
- `import android.os.ParcelFileDescriptor` is unused — the type is inferred from the return type of `executeShellCommand()`.
- **Fix:** Remove the unused import.

**36. Indentation inconsistency in Glance modifier chain**
- File: `SensorWidget.kt:121`
- `.background(ColorProvider(widgetBorderColor))` indented at the same level as the `Box` composable start rather than under the `.size()` call like its counterpart at lines 85-87.
- **Fix:** Add 4 more spaces of indentation to `.background(...)`.

**37. All user-facing strings hardcoded**
- File: `AppInfoDialog.kt:67-203`
- All UI strings are Kotlin string literals rather than `strings.xml` resources. Prevents localization.
- **Fix:** Extract user-facing strings to `res/values/strings.xml`.

**38. Room ProGuard rule is dead code**
- File: `proguard-rules.pro:9-11`
- `-keep class * extends androidx.room.RoomDatabase { <init>(); }` keeps RoomDatabase constructors, but Room is not a dependency in build.gradle.kts. Dead configuration.
- **Status: ✓ FIXED** (PR #104) — Lines removed by commit `2a1b10a`.

**39. `$OptIn(ExperimentalCoroutinesApi)` annotation unnecessary**
- File: `SystemSensorRepository.kt:51`
- `flatMapLatest` has been stable since kotlinx-coroutines 1.6.0. The annotation is visual noise.
- **Fix:** Remove the `@OptIn` annotation and the `ExperimentalCoroutinesApi` import.

### New Test Quality Issues

**40. SensorViewModelTest missing behavioral coverage**
- File: `SensorViewModelTest.kt`
- Only tests initial state values and `sensorTypes`. Does not test that `refresh()` propagates to the repository or that state flows change when `FakeSensorRepository` emits new values.
- **Fix:** Add tests for `vm.refresh()` invocation and state flow changes on repo emission.

**41. Widget receiver tests only verify non-null RemoteViews**
- Files: `SensorWidgetReceiverTest.kt`, `CompactWidgetReceiverTest.kt`
- Both test classes only verify `updateAppWidget` was called with non-null RemoteViews. No layout resource, click PendingIntent, or view property checks.
- **Fix:** Use `ShadowRemoteViews` to verify layout ID and click handlers.

**42. `lastRefreshTime` test is tautological**
- File: `WidgetStateTest.kt:50-54`
- Sets `WidgetState.lastRefreshTime = time` then asserts `> 0` instead of asserting equality with `time`.
- **Fix:** Replace `assertTrue(WidgetState.lastRefreshTime > 0)` with `assertEquals(time, WidgetState.lastRefreshTime)`.

**43. `areRuntimePermissionsGranted()` never tested**
- File: `PermissionHelperTest.kt`
- Tests validate `getRequiredRuntimePermissions()` but never test `areRuntimePermissionsGranted()`, which has real Android permission-checking behavior.
- **Fix:** Add a Robolectric-based test with `shadowOf().grantPermissions()`.

### Info

| # | Finding | File | Description |
|---|---------|------|-------------|
| 1 | Fixed color scheme uses Material3 defaults for non-primary | `EzWorkSafeTheme.kt:31` | Only `primary` is overridden; all other slots use Material3 defaults. Intentional for simple app. |
| 2 | `afterEvaluate` incompatible with configuration cache | `app/build.gradle.kts:143` | **✓ FIXED (PR #104)** — replaced with top-level `if` block, compatible with config cache. |
| 3 | Duplicate JaCoCo version config | `app/build.gradle.kts:10,76` | **✓ FIXED (PR #104)** — `testCoverage { jacocoVersion }` removed. |
| 4 | Missing explicit `kotlin("android")` plugin | `app/build.gradle.kts:4` | Only `kotlin.plugin.compose` is applied, not the base Kotlin Android plugin. Works via transitive resolution but fragile. |
| 5 | Missing `dataExtractionRules` in manifest | `AndroidManifest.xml:26-27` | **✓ FIXED (PR #104)** — `android:dataExtractionRules="@xml/data_extraction_rules"` added. |

### Documentation Accuracy Issues

| # | Doc | What it says | What it should say |
|---|-----|-------------|-------------------|
| 1 | `API.md:252` | Kotlin Compose plugin `2.2.10` | `2.3.21` |
| 2 | `API.md:277` | `mockito-core:5.7.0`, `mockito-kotlin:5.1.0` | `mockito-core:5.23.0`, `mockito-kotlin:6.3.0` |
| 3 | `API.md:162` | `androidx.test.ext:junit:1.2.1`, `androidx.test:rules:1.6.1` | `androidx.test.ext:junit:1.3.0`, `androidx.test:rules:1.7.0` |
| 4 | `README.md:8` | Kotlin badge `2.2` | `2.3.21` |
| 5 | `DEVELOPMENT.md:41` | 52 unit tests | 56 unit tests |
| 6 | `DEVELOPMENT.md:58` | 22 E2E tests | 32 E2E tests (QuickSettingsToggleE2eTest added) |
| 7 | `PLAN.md:56-68` | Omits `QuickSettingsToggleE2eTest.kt` | Should include it in file listing |
| 8 | `security.md:242` | Stale file line counts | Files have grown ~10-30 lines since last audit |

---

## Build & CI Health

| Check | Status |
|-------|--------|
| `./gradlew build` | ✅ Passes |
| `./gradlew lint` | ✅ Clean (no warnings) |
| `./gradlew test` | ✅ All 56 unit tests pass |
| `./gradlew connectedDebugAndroidTest` | ✅ E2E tests pass (1 @Ignored) |
| GitHub Actions workflow | ✅ `permissions: contents: read`, guarded keystore steps |

### CI Issues — All Resolved

| # | Severity | Issue | Status |
|---|----------|-------|--------|
| 1 | High | `build` step runs lint+test 3x (via `./gradlew build`, then `./gradlew lint`, then `./gradlew test`) | **✓ FIXED (PR #104)** — uses `assembleDebug` instead |
| 2 | Medium | No CodeQL/sast workflow file | **✓ FIXED (PR #104)** — `codeql.yml` added |
| 3 | Medium | No E2E tests in CI | **By design** — hobby project, no budget for emulator CI |
| 4 | Medium | Secrets written via shell heredoc in CI | **✓ FIXED (PR #104)** — uses direct `echo` into file |
| 5 | Low | Dependabot only monitors Gradle, not GitHub Actions | **✓ FIXED (PR #104)** — `github-actions` entry added |
| 6 | Info | Upload release APK before lint/test run | **✓ FIXED (PR #104)** — `if: success()` guard added |
| 7 | Info | JDK 17 in CI vs JDK 21 in gradle-daemon-jvm.properties | **✓ FIXED (PR #104)** — CI now uses JDK 21 |

---

## Summary

The project is in strong shape. The architecture is clean, security posture is sound, and documentation is
comprehensive. Key findings this session:

- **0 Medium security issues:** All previously identified security issues remain properly fixed.
- **3 Medium code quality:** Unused `context` parameter in `WidgetContent`, aggressive 2-second polling loop in `MainActivity`, unused `audioManager` in `observeMicStatus()`.
- **10 Low code quality:** Redundant SDK guard, `Inactive` never emitted, redundant Glance modifiers, unsafe cast, WidgetState encapsulation, PFD leak, ambiguous test matchers, WidgetState not reset in E2E, tautological refresh test, misleading widget-rendering test, shallow RemoteViews assertions, unused import, indentation, hardcoded strings, unnecessary `@OptIn`, inconsistent flow termination, `for`/`continue` style, ViewModel test gaps, receiver test gaps, tautological `lastRefreshTime` test, `areRuntimePermissionsGranted()` untested.
- **9 Documentation accuracy issues** — stale version numbers, test counts, and file listings across `API.md`, `README.md`, `DEVELOPMENT.md`, `PLAN.md`, and `security.md`.
- **0 CI issues** — all resolved (PR #104) or acknowledged as by-design (no budget for emulator CI).
- **All findings from previous review sessions remain resolved.** No regressions in previously fixed areas.
