# ezWorkSafe - AGENTS.md

## Project Overview
Android app (Native Kotlin, MVVM) that displays real-time status of
WiFi, Bluetooth, Microphone access, and Camera access for work
safety/privacy monitoring.

## Context7 Usage
Use Context7 MCP for ALL Android SDK/API lookups — permission APIs,
system services (WifiManager, BluetoothAdapter, AudioManager,
CameraManager), Flow/LiveData patterns. Training data may be outdated.

**Steps:**
1. `resolve-library-id` with library name (e.g., "Android SDK")
2. Pick best match by name, description, snippet count, reputation
3. `query-docs` with library ID + full question
4. Retry with `researchMode: true` if unsatisfied

## Build Commands
| Command | Purpose |
|---------|---------|
| `./gradlew build` | Build project |
| `./gradlew lint` | Run lint checks |
| `./gradlew test` | Run unit tests |
| `./gradlew installDebug` | Install to device |
| `./gradlew connectedDebugAndroidTest` | Run E2E tests (device/emulator) |

## Emulator Commands
| Command | Purpose |
|---------|---------|
| `android emulator list` | List available AVDs |
| `android emulator start Pixel_8_Pro` | Start emulator (AVD: Pixel_8_Pro) |
| `android emulator start --cold Pixel_8_Pro` | Cold boot (no snapshot) |
| `android emulator stop Pixel_8_Pro` | Stop emulator |

> `main` branch has repository rulesets requiring all changes through PRs (no direct pushes).

**PR workflow:**
- Use `Fixes #N` (not `Fixes Issue #N`) in the PR body to auto-close issues on merge
- GitHub does NOT recognize `Fixes Issue #N` — the word "Issue" breaks keyword detection
- The commit message does not matter for auto-close, only the PR body
- Use `gh pr merge --auto --squash` to enable auto-merge once CI/CodeQL pass
- **Before merging**, verify docs are updated: search `docs/` for references to the old behavior — check `review.md`, `security.md`, `API.md`, `PLAN.md`, and any `docs/superpowers/` specs/plans

**CI notes:**
- Workflow requires `permissions: contents: read` for GITHUB_TOKEN
- Both "Decode keystore" and "Create keystore.properties" steps guarded by `if: env.KEYSTORE_B64 != ''`
- Release builds fail on PRs from forks unless keystore secrets are available

## Current SDK Versions & Tools
| Config | Value |
|--------|-------|
| `compileSdk` | 36 |
| `minSdk` | 26 |
| `targetSdk` | 35 |
| AGP | 9.2.1 |
| Gradle wrapper | 9.5.1 |

## Required Permissions
- `ACCESS_WIFI_STATE` — WiFi status
- `BLUETOOTH` / `BLUETOOTH_CONNECT` — Bluetooth status
- `RECORD_AUDIO` — Mic access monitoring (runtime permission)
- `CAMERA` — Camera access monitoring (runtime permission)
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` — Foreground monitoring service

## Architecture: MVVM
**Package Structure:**
```
app/src/main/java/com/ezworksafe/
├── data/
│   ├── repository/    # SensorRepository interface + SystemSensorRepository impl
│   └── model/         # SensorStatus sealed class, SensorType enum
├── ui/
│   ├── viewmodel/     # SensorViewModel (exposes StateFlow per sensor)
│   └── view/          # MainActivity + StatusDashboard + AppInfoDialog + EzWorkSafeTheme
├── service/           # MonitoringService (foreground, pushes widget updates)
├── widget/            # SensorWidget (Glance), SensorWidgetReceiver, WidgetState singleton
└── util/              # PermissionHelper, FormatUtils
```

**Real-time Pattern:** `callbackFlow` wrapping system callbacks:
- WiFi: `WifiManager` + `BroadcastReceiver`
- Bluetooth: `BluetoothAdapter` + `BroadcastReceiver`
- Mic: `AudioManager` + `AudioRecordingCallback` (API 24+)
- Camera: `CameraManager` + `AvailabilityCallback`

## Android Gotchas
- Mic/Camera need **runtime permission** requests, not just manifest
- Background monitoring uses a **foreground service** (`MonitoringService`, `foregroundServiceType="specialUse"`) — the service is started on app launch and runs persistently to push widget updates
- Camera/mic monitoring APIs vary across **API levels**
- Some `BroadcastReceiver` actions restricted since Android 8+
- **Android 16 AppOps limitation**: `checkOpNoThrow()` returns `MODE_IGNORED` for background processes regardless of actual privacy toggle state. This is server-side enforced with no client-side workaround. Mic/Cam privacy toggle changes are NOT detectable from background — the widget's right section (Mic/Cam) shows stale state until the user opens the app.
- **Widget dual-path architecture**: Glance `AppWidget` provides initial render (via WorkManager, ~45s delay). Real-time updates use direct `RemoteViews` push from `MonitoringService.pushWidgetUpdate()`, bypassing Glance entirely. See `app/src/main/res/layout/widget_sensor_status.xml` for the RemoteViews layout.
- **Widget sections**: Left section (WiFi/BT) updates in real-time via system broadcasts. Right section (Mic/Cam+timestamp) only reflects state from the last foreground refresh. The divider visually separates the two.
- **Notification "Refresh" action**: The foreground notification includes a "Refresh" button that opens `MainActivity`. The activity's `ON_RESUME` handler calls `refreshSensorFlows()` → `repository.refresh()`, which increments `refreshTrigger` causing all sensor flows to re-emit via `flatMapLatest`. This brings Mic/Cam state up to date.
- **`WidgetState` singleton**: Shared mutable state between `MonitoringService` (writer, `Dispatchers.Main`) and `SensorWidget` (reader, Glance thread). Fields are `@Volatile` for visibility. `lastRefreshTime` is set by `repository.refresh()` but NOT by `pushWidgetUpdate()` — the timestamp may lag behind the widget display.
- **Widget click handler — triple path**: Tapping a Glance widget to open the app requires covering all three rendering paths:
  1. Initial XML layout — `setOnClickPendingIntent` in `SensorWidgetReceiver.onUpdate()`
  2. RemoteViews push — `openAppIntent` param in `buildWidgetRemoteViews()`
  3. Glance Compose — `.clickable(actionStartActivity<MainActivity>())` in `SensorWidget.kt`
  Each path has an independent click action; the Glance `clickable` only covers the last.

## Plan Execution
Default: **subagent-driven-development** — fresh subagent per task with two-stage
review (spec compliance → code quality) after each. See implementation plans in
`docs/superpowers/plans/`.

## Implementation Plan
See [docs/PLAN.md](docs/PLAN.md) for the full implementation breakdown.

## Dependabot Alerts
- Alerts page (GitHub auth required): `https://github.com/zeekec/ezWorkSafe/security/dependabot`
- Fetch alerts via API:
  ```
  gh api -H "Accept: application/vnd.github+json" \
    -H "X-GitHub-Api-Version: 2022-11-28" \
    /repos/zeekec/ezWorkSafe/dependabot/alerts --paginate
  ```
- Dismiss as build-only (transitive toolchain deps not shipped in APK):
  ```
  gh api -X PATCH /repos/zeekec/ezWorkSafe/dependabot/alerts/<id> \
    --field state=dismissed \
    --field dismissed_reason=tolerable_risk \
    --field dismissed_comment="Build-time transitive dependency, not shipped in the APK"
  ```

## Testing
- Unit: ViewModel + Repository + Service notification (JUnit + Mockito + Robolectric + `runTest`)
- E2E: Compose UI + `FakeSensorRepository` (instrumented, `connectedDebugAndroidTest`)
- Widget: Provider metadata + `WidgetState` label verification (instrumented)
- Notification: `dumpsys activity services` via `UiAutomation.executeShellCommand` (E2E)
