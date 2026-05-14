# Code & Documentation Review

**Date:** 2026-05-14
**Last updated:** 2026-05-14 (session 2)
**Commit:** `44e8b56` (plus uncommitted in-app docs changes)
**Review scope:** Full codebase, tests, config, docs, CI

---

## Architecture

MVVM with clear separation of concerns. Interface-based `SensorRepository` + `SystemSensorRepository` implementation. `callbackFlow` + `flatMapLatest` pattern for reactive sensor observations is idiomatic Kotlin coroutines.

The `WidgetState` singleton is a pragmatic choice for cross-component state sharing between the service and Glance widgets, but introduces a data-layer-to-widget-layer dependency.

---

## Strengths

### Code quality
- Clean MVVM with interface-based repository pattern
- Well-chosen `callbackFlow` + `flatMapLatest` for reactive sensor observations
- `FakeSensorRepository` enables deterministic E2E tests
- `FormatUtils.formatLastUpdated()` extracted as pure-Kotlin function — testable without Android mocking
- `isOpBlocked()` extracted as pure function with 5 dedicated unit tests
- `buildWidgetRemoteViews()` extracted as top-level testable function
- Permission helper is version-aware (`BLUETOOTH_CONNECT` on API 31+)
- ProGuard strips `Log.d` via `-assumenosideeffects`
- No dead imports, no unused resources (colors cleaned up)
- Configuration cache: `./gradlew build` in < 1s on cache hit

### UI
- Edge-to-edge display with `safeDrawingPadding()`
- Dark/light mode via Material You dynamic colors (API 31+) and seeded fallback
- About & Help sheet with expandable sections, documents widget Mic/Cam limitation
- Custom widget initial layout replaces 45s Glance blank loading state
- Triple-path widget click handler (initial XML, RemoteViews push, Glance Compose)

### Testing
- 38 unit tests (Model, Repository, ViewModel, WidgetState, FormatUtils, PermissionHelper, MonitoringService)
- 22 E2E tests (dashboard, widget, notification, theme, widget metadata)
- `FakeSensorRepository` shared between unit and E2E tests
- Robolectric for service notification tests
- E2E notification verification via `dumpsys`

### Security & compliance
- `allowBackup="false"`, `fullBackupContent="false"`
- Pre-commit hook blocks `keystore.properties` commits
- `afterEvaluate` warning when keystore missing
- 28 Dependabot alerts dismissed as build-only transitive deps
- Apache 2.0 license with SPDX headers on all Kotlin files
- All runtime deps Apache 2.0; test-only deps MIT/EPL/BSD

### Documentation
- AGENTS.md: comprehensive build/test commands, architecture, gotchas, Dependabot, emulator commands
- PLAN.md: full implementation plan with post-plan additions
- API.md: detailed API reference with docs links
- review.md: this file ;)

---

## Issues Found

### Minor

**1. Dead code: `CellIds` data class and `cellMap` in `MonitoringService`**
- File: `app/src/main/java/com/ezworksafe/service/MonitoringService.kt:73-80`
- `private data class CellIds` and `private val cellMap` are defined but never used. The actual cell mapping for RemoteViews construction lives in `buildWidgetRemoteViews()` which has its own local `cellMap`.
- The `CellIds` approach (named data class) is actually better than the `Pair` used in `buildWidgetRemoteViews()`.

**2. `@OptIn(ExperimentalCoroutinesApi::class)` is unnecessary**
- File: `app/src/main/java/com/ezworksafe/data/repository/SystemSensorRepository.kt:42`
- `flatMapLatest` was stabilized in Kotlin Coroutines 1.6.0 (current: 1.11.0). The `@OptIn` annotation is dead code.

**3. `WifiManager.isWifiEnabled` is deprecated since API 28**
- File: `app/src/main/java/com/ezworksafe/data/repository/SystemSensorRepository.kt:62`
- Should use `WifiManager.WIFI_STATE_ENABLED` comparison with `getWifiState()`. Currently compiles without warnings for unknown reasons (possibly suppressed by existing `@Suppress` or lint config).

**4. README.md has stale references**
- File: `README.md`
  - Tech stack table says `targetSdk: 33` (should be `35`)
  - Build command example: `emulator -avd Pixel_9_API_34` (should reference `android emulator start Pixel_8_Pro`)
  - The E2E test section references a `Pixel_9_API_34` AVD that doesn't exist in this project

**5. Manifest declares redundant `BLUETOOTH` permission**
- File: `AndroidManifest.xml:9`
- On API 31+, `BLUETOOTH` is subsumed by `BLUETOOTH_CONNECT`. On API < 31, `BLUETOOTH` alone suffices. Having both is correct and harmless — the OS ignores the weaker permission at runtime when the stronger one is granted.
- **Status:** Informational, not a bug.

### Test Coverage Gaps

| Area | Status |
|------|--------|
| `MonitoringService.combine` collector integration | Untested — requires Robolectric with service lifecycle |
| `pushWidgetUpdate()` end-to-end | Untested — requires widget binding |
| `SystemSensorRepository` system-service integration | Untested at unit level (mock Context returns null for all services) |
| `PermissionRefreshE2eTest` | `@Ignored` — `executeShellCommand` crashes on API 36 |
| Widget `AppOps` foreground/background behavior | Untestable without API 36+ device with specific AppOps config |

### Documentation Gaps

| Gap | Notes |
|-----|-------|
| `colors.xml` background value unused since edge-to-edge | The `@color/background` is still referenced by nothing — but harmless |
| README targetSdk mismatch | Easy to fix, just needs a number bump |

---

## B-1 Security Audit Summary

| Check | Status |
|-------|--------|
| Manifest permissions minimal | ✅ Only what's needed |
| `allowBackup=false` | ✅ |
| No hardcoded secrets | ✅ |
| ProGuard / R8 enabled | ✅ `isMinifyEnabled = true` |
| Log stripping via ProGuard | ✅ `-assumenosideeffects` for `Log.d` |
| Verify broadcast receivers protected | ✅ Exported only when needed |
| No debugging endpoint in release | ✅ Debug intent handlers not present |
| No internal IPs / credentials in code | ✅ |
| Dependabot alerts reviewed | ✅ 28 dismissed as build-only |
| SPDX license headers | ✅ All 30+ Kotlin files |
| Pre-commit hook for keystore | ✅ |

---

## Build & CI Health

| Check | Status |
|-------|--------|
| `./gradlew build` | ✅ Passes |
| `./gradlew lint` | ✅ Clean (no warnings) |
| `./gradlew test` | ✅ 38 tests pass |
| `./gradlew connectedDebugAndroidTest` | ✅ 22 tests pass (1 skipped) |
| GitHub Actions workflow | ✅ `permissions: contents: read`, guarded keystore steps |
| Dependabot config | ✅ Weekly Gradle scanning |
| Configuration cache | ✅ Enabled |

---

## Summary

The project is in strong shape. The architecture is clean, tests are thorough, security is handled, and documentation is comprehensive. The remaining issues are minor: one piece of dead code (`CellIds`), one unnecessary annotation (`@OptIn`), a few README stale references, and one deprecated API call. No critical or blocking issues remain.
