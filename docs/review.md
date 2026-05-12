# Code & Documentation Review

## Architecture

The MVVM structure is clean and well-separated. `SensorRepository` interface + `SystemSensorRepository` implementation is the right approach. The use of `callbackFlow` + `flatMapLatest` for reactive sensor state is idiomatic Kotlin coroutines usage.

The `WidgetState` singleton is a pragmatic choice for cross-component state sharing between the service and Glance widgets, but introduces a data-layer-to-widget-layer dependency that violates strict clean architecture.

---

## Issues Found

### Critical

**14. Widget shows blank/loading state for ~45 seconds on first add**
- File: `app/src/main/res/xml/widget_info_sensor.xml:7`
- `initialLayout="@layout/glance_default_loading_layout"` — Glance's built-in loading spinner. The WorkManager update (~45s) must fire before content appears, and `MonitoringService` must be running for `pushWidgetUpdate()` to activate.
- First-impression UX: user adds widget and sees a blank/loading state for nearly a minute.
- **Fix:** Create a custom initial layout (`widget_initial_layout.xml`) matching the real widget structure but with placeholder/loading state. Reference it via `android:initialLayout`.

### Important

**1. `WidgetState` is unsynchronized mutable global state**
- File: `app/src/main/java/com/ezworksafe/widget/WidgetState.kt:7-12`
- `statuses` and `lastRefreshTime` are public `var` with no synchronization.
- Written from the service's coroutine collector (Dispatchers.Main) and read from Glance's `provideGlance` (possibly different thread).
- **Status: ✓ FIXED** — `@Volatile` ensures cross-thread visibility. The `Map` from `mapOf()` is immutable, so reference assignment is atomic.

**2. Redundant `refreshSensorFlows()` calls in `MainActivity`**
- File: `app/src/main/java/com/ezworksafe/ui/view/MainActivity.kt:28-51`
- Both `ON_RESUME` (line 29) and `onWindowFocusChanged` (line 47) call `refreshSensorFlows()`.
- **Status: ✓ FIXED** — removed `onWindowFocusChanged` override.

**3. `tools:targetApi` in manifest out of sync**
- File: `app/src/main/AndroidManifest.xml:31`
- `tools:targetApi="34"` but `targetSdk = 36` in `build.gradle.kts:25`.
- **Status: ✓ FIXED** — `targetSdk` is now 33, `compileSdk` 34, `tools:targetApi="34"` is correct.

**4. `SensorWidgetReceiver` catches overly broad `Exception`**
- File: `app/src/main/java/com/ezworksafe/widget/SensorWidgetReceiver.kt:18`
- `catch (e: Exception)` should be `catch (e: ForegroundServiceStartNotAllowedException)`.
- **Status: ✓ FIXED** — narrowed to `catch (e: IllegalStateException)` (parent of the API 35+ exception, since compileSdk=34).

**5. `SensorRepositoryTest` uses mock Context, tests error path only**
- File: `app/src/test/java/com/ezworksafe/data/repository/SensorRepositoryTest.kt`
- `mock(Context::class.java)` returns null for all `getSystemService` calls, so every sensor flow emits `Unavailable`.
- **Status: ✓ FIXED** — `FakeSensorRepository` copied to `src/test`, added tests for `refresh()` updates `WidgetState.lastRefreshTime` and fake emits configured statuses.

**6. `MonitoringService` has no unit tests**
- File: `app/src/test/java/com/ezworksafe/widget/MonitoringServiceWidgetTest.kt`
- **Status: ✓ PARTIALLY FIXED** — test renamed to `WidgetStateLabelTest`, `MonitoringServiceNotificationE2eTest` added (verifies FGS notification via `dumpsys`). `formatLastUpdated` extracted to `FormatUtils.kt` with dedicated unit tests. Still missing: unit tests for `combine` collector integration and `pushWidgetUpdate()` (34 lines of RemoteViews construction logic) — both require Robolectric.

**7. `docs/API.md` contains stale line-number references**
- File: `docs/API.md:311,321`
- Lines cited (`SensorStatus.kt:3-11`, `SensorStatus.kt:13-17`) are off by ~2-7 lines since private color constants were added. `SensorStatus` sealed class is now at lines 9-18, `SensorType` enum at lines 20-25.
- **Status: ✓ RE-FIXED** — line numbers refreshed.

### Minor

**8. Hardcoded sensor label strings in widget**
- XML (`widget_sensor_status.xml`): "WiFi", "BT", "Mic", "Cam" hardcoded in `android:text`.
- Glance (`SensorWidget.kt:68-70,106-108`): Same strings hardcoded in `when` branches.
- These should use `SensorType.displayName` or be extracted to `strings.xml`. The Glance layout duplicates string logic that already exists in `SensorType`.

**9. Unused color resources in `colors.xml`**
- File: `app/src/main/res/values/colors.xml`
- `status_active`, `status_inactive`, `status_denied`, `status_unavailable`, `card_background` — all defined but never referenced.
- **Status: ✓ FIXED** — removed all unused entries, keeping only `background` (used by `themes.xml`).

**10. `SensorStatus.color` stored as `Long`, used as `Int`**
- File: `app/src/main/java/com/ezworksafe/data/model/SensorStatus.kt:5`
- `color` is `Long` (hex literals like `0xFF4CAF50L`), but every call site does `.toInt()`.
- **Status: ✓ FIXED** — `color` changed to `Int`, hex values extracted to private top-level `val` constants (e.g. `private val ACTIVE_COLOR = 0xFF4CAF50.toInt()`). All 6 `.toInt()` call sites updated.

**17. Redundant `ViewModelProvider` lookup on every resume**
- File: `app/src/main/java/com/ezworksafe/ui/view/MainActivity.kt:47-49`
- `refreshSensorFlows()` creates a new `ViewModelProvider(this)` lookup. The ViewModel is already obtained in `setContent` via `viewModel()` (line 40). Two lookups for the same ViewModel.
- **Fix:** Store the ViewModel as a field and reuse, or pass it to `refreshSensorFlows()`.

**18. `PermissionHelper.isPermissionGranted()` is unused dead code**
- File: `app/src/main/java/com/ezworksafe/util/PermissionHelper.kt:21-23`
- `isPermissionGranted()` is never called. `SystemSensorRepository` uses `ContextCompat.checkSelfPermission` directly.
- **Fix:** Remove the unused function.

**11. `docs/PLAN.md` file structure missing widget package** _(superseded — PLAN.md already updated)_
- File: `docs/PLAN.md`
- The documented file tree doesn't include `widget/`, `service/`, or the E2E test directory. These are now core parts of the project.
- **Status: ✓ FIXED** — PLAN.md file tree includes `widget/` and `service/` directories in multiple locations (lines 43-61).

**12. No documentation of Android 16 AppOps limitation** _(superseded)_
- The critical discovery that `unsafeCheckOpNoThrow` returns `MODE_IGNORED` for background processes on Android 16 is recorded only in conversation history, not in any doc file.
- This is the reason the widget has sections and the "Updated" timestamp. Worth documenting.
- **Status: ✓ FIXED** — Documented in AGENTS.md "Android Gotchas" and PLAN.md "Post-Plan Additions".

**13. Outdated dependencies**
- `core-ktx:1.12.0` (latest: 1.18.0)
- `lifecycle-runtime-ktx:2.7.0` (latest: 2.10.0)
- `compose-bom:2024.01.00` (latest: 2026.05.00)
- `activity-compose:1.8.2` (latest: 1.13.0)
- `coroutines-android:1.7.3` (latest: 1.11.0)

**15. `SystemSensorRepository` permission/AppOp logic untested at unit level**
- File: `app/src/main/java/com/ezworksafe/data/repository/SystemSensorRepository.kt:108-121`
- `isAppOpBlocked()` (14 lines, API-level branching) has no unit tests. The only `SensorRepositoryTest` uses a mock Context that returns `null` for all system services, so only the `Unavailable` fallback path is exercised.
- The `Denied` (permission not granted), `Blocked` (AppOp = MODE_IGNORED), and `Active` (AppOp = MODE_ALLOWED) paths are never tested.
- **Fix:** Add a `FakeSystemSensorRepository` or use mock system services with controlled return values. At minimum, add a unit test that verifies each `isAppOpBlocked` API branch (pre-Q, Q+, per-op string).

**16. Missing `@Suppress("DEPRECATION")` on deprecated `noteOpNoThrow` call**
- File: `app/src/main/java/com/ezworksafe/data/repository/SystemSensorRepository.kt:115`
- `appOps.noteOpNoThrow()` is deprecated (API < Q fallback path). Build emits: `'fun noteOpNoThrow(p0: String, p1: Int, p2: String): Int' is deprecated.`
- Generates a build warning. Add `@Suppress("DEPRECATION")` to the method or call site.

---

## Test Coverage Gaps

| Area | What's missing | Status |
|------|----------------|--------|
| `MonitoringService` | `pushWidgetUpdate`, `formatLastUpdated`, notification creation, `combine` collector | **PARTIAL** — E2E test verifies FGS notification via `dumpsys`. `formatLastUpdated` extracted to shared utility with unit tests. `combine` collector and `pushWidgetUpdate` integration still missing (require Robolectric). |
| `formatLastUpdated` | Unit test for either implementation (service + Glance) | **✓ FIXED** — Extracted to shared `FormatUtils.formatLastUpdated(time, dateFormat)` in `util/FormatUtils.kt`, pure-Kotlin function with no Android dependency. Tested in `FormatUtilsTest` (zero-time + non-zero-time cases via `SimpleDateFormat`). No mocking needed. |
| `SystemSensorRepository` | Permission/AppOp logic untested (only `Unavailable` path covered) | **OPEN** — `isAppOpBlocked()` has API-level branching (pre-Q, Q+) with zero test coverage. `observeMicStatus`/`observeCameraStatus` permission and AppOp paths untested. Requires mock system services or refactoring. |
| `repository.refresh()` | SensorViewModel `refresh()` delegation | **✓ FIXED** — `SensorViewModelTest` now calls `verify(mockRepo).refresh()`. |
| `WidgetState.lastRefreshTime` | `lastRefreshTime` updated by `repository.refresh()` | **✓ FIXED** — `SensorRepositoryTest.refresh updates WidgetState lastRefreshTime` added. |
| `FakeSensorRepository` | Not used by unit tests | **✓ FIXED** — Copied to `src/test`, used in `SensorRepositoryTest.fake repository emits configured statuses`. |
| Widget AppOps limitation | No test for `isAppOpBlocked` foreground/background behavior | **OPEN** — Requires device with API 36+ and specific AppOps configuration. Documented in AGENTS.md and PLAN.md instead. |

## Documentation Gaps

| What's missing | Where it should go | Status |
|----------------|-------------------|--------|
| Android 16 AppOps background restriction + widget section rationale | `docs/API.md` or new doc | **✓ FIXED** — Documented in PLAN.md "Post-Plan Additions" and AGENTS.md "Android Gotchas". |
| Widget architecture (dual-path: Glance initial render + RemoteViews push) | `docs/PLAN.md` or `AGENTS.md` | **✓ FIXED** — Documented in PLAN.md file tree + Post-Plan Additions, AGENTS.md "Android Gotchas". |
| `WidgetState` singleton contract (written from service, read by Glance) | `AGENTS.md` in the widget subsection | **✓ FIXED** — Documented in AGENTS.md "Android Gotchas". |
| Why Mic/Cam only update on foreground refresh | `AGENTS.md` "Android Gotchas" section | **✓ FIXED** — Covered under Android 16 AppOps limitation entry. |
| How the notification "Refresh" action works | `docs/PLAN.md` or `AGENTS.md` | **✓ FIXED** — Documented in AGENTS.md "Notification 'Refresh' action" and PLAN.md Post-Plan Additions. |
| `docs/API.md` line references need refreshing after code changes | `docs/API.md` | **CYCLICAL** — Line references go stale whenever SensorStatus.kt is modified. Consider removing line numbers from API.md or adding a CI check. |

---

## Strengths

- Clean MVVM separation of concerns with interface-based `SensorRepository` + `SystemSensorRepository` implementation
- Well-chosen `callbackFlow` + `flatMapLatest` pattern for reactive sensor observations
- `FakeSensorRepository` makes E2E tests deterministic and fast
- Strong E2E coverage: dashboard UI (all sensor states), widget provider metadata, FGS notification via `dumpsys`, theme rendering
- `formatLastUpdated` extracted to shared `FormatUtils.kt` — eliminates code duplication between `MonitoringService` and `SensorWidget`, and enables pure-Kotlin unit testing without Android mocking
- Dual-path widget architecture is pragmatic: Glance for initial render (WorkManager), RemoteViews push via service for real-time updates
- `WidgetState` uses `@Volatile` for cross-thread visibility — appropriate for simple single-value reads/writes on immutable maps
- Configuration cache enabled, builds complete in under 1s on cache hit
- Clean lint output (no errors or warnings beyond pre-existing `noteOpNoThrow` deprecation)
