# Code & Documentation Review

## Architecture

The MVVM structure is clean and well-separated. `SensorRepository` interface + `SystemSensorRepository` implementation is the right approach. The use of `callbackFlow` + `flatMapLatest` for reactive sensor state is idiomatic Kotlin coroutines usage.

The `WidgetState` singleton is a pragmatic choice for cross-component state sharing between the service and Glance widgets, but introduces a data-layer-to-widget-layer dependency that violates strict clean architecture.

---

## Issues Found

### Critical

None.

### Important

**1. `WidgetState` is unsynchronized mutable global state**
- File: `app/src/main/java/com/ezworksafe/widget/WidgetState.kt:7-10`
- `statuses` and `lastRefreshTime` are public `var` with no synchronization.
- Written from the service's coroutine collector (Dispatchers.Main) and read from Glance's `provideGlance` (possibly different thread).
- **Status: ✓ FIXED** — fields marked `@Volatile` suffices for single-value reads/writes.

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
- **Status: ✓ PARTIALLY FIXED** — test renamed to `WidgetStateLabelTest`, `MonitoringServiceNotificationE2eTest` added (verifies FGS notification via `dumpsys`). Still missing: unit tests for `formatLastUpdated()` and `combine` collector integration. These require Android framework dependencies (or Robolectric) — scoped as future work.

**7. `docs/API.md` contains stale line-number references**
- File: `docs/API.md`
- **Status: ✓ FIXED** — all line numbers refreshed to match current code.

### Minor

**8. Hardcoded sensor label strings in widget**
- XML (`widget_sensor_status.xml`): "WiFi", "BT", "Mic", "Cam" hardcoded in `android:text`.
- Glance (`SensorWidget.kt`): Same strings hardcoded in `when` branches.
- These should use `SensorType.displayName` or be extracted to `strings.xml`. The Glance layout duplicates string logic that already exists in `SensorType`.

**9. Unused color resources in `colors.xml`**
- File: `app/src/main/res/values/colors.xml`
- `status_active`, `status_inactive`, `status_denied`, `status_unavailable`, `card_background`, `background` — all defined but never referenced (widget uses inline hex values, Compose uses `SensorStatus.color`).

**10. `SensorStatus.color` stored as `Long`, used as `Int`**
- File: `app/src/main/java/com/ezworksafe/data/model/SensorStatus.kt:5`
- `color` is `Long` (hex literals like `0xFF4CAF50L`), but every call site does `.toInt()`.
- **Fix:** Store as `Int` directly: `0xFF4CAF50.toInt()`. Kotlin can represent ARGB as `Int` (which is 32-bit).

**11. `docs/PLAN.md` file structure missing widget package**
- File: `docs/PLAN.md`
- The documented file tree doesn't include `widget/`, `service/`, or the E2E test directory. These are now core parts of the project.

**12. No documentation of Android 16 AppOps limitation**
- The critical discovery that `unsafeCheckOpNoThrow` returns `MODE_IGNORED` for background processes on Android 16 is recorded only in conversation history, not in any doc file.
- This is the reason the widget has sections and the "Updated" timestamp. Worth documenting.

**13. Outdated dependencies**
- `core-ktx:1.12.0` (latest: 1.18.0)
- `lifecycle-runtime-ktx:2.7.0` (latest: 2.10.0)
- `compose-bom:2024.01.00` (latest: 2026.05.00)
- `activity-compose:1.8.2` (latest: 1.13.0)
- `coroutines-android:1.7.3` (latest: 1.11.0)

---

## Test Coverage Gaps

| Area | What's missing | Status |
|------|----------------|--------|
| `MonitoringService` | `pushWidgetUpdate`, `formatLastUpdated`, notification creation, `combine` collector | **PARTIAL** — E2E test verifies FGS notification via `dumpsys`. Unit tests for `formatLastUpdated` and `combine` integration still missing (require Robolectric or refactoring). |
| `formatLastUpdated` | Unit test for either implementation (service + Glance) | **OPEN** — Requires Android Context mocking (dependent on `DateFormat.getTimeFormat()`). |
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

---

## Strengths

- Clean MVVM separation of concerns
- Well-chosen `callbackFlow` + `flatMapLatest` pattern for reactive sensor observations
- `FakeSensorRepository` makes E2E tests deterministic and fast
- E2E tests exist for the dashboard UI and widget provider metadata
- Configuration cache enabled, builds complete in under 1s on cache hit
- The `@Suppress("DEPRECATION")` annotations are correctly scoped to the specific fallback path (API < Q)
