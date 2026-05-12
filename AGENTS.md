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

## Current SDK Versions
| Config | Value |
|--------|-------|
| `compileSdk` | 34 (Glance 1.1.1 minimum) |
| `minSdk` | 26 |
| `targetSdk` | 33 |

## Required Permissions
- `ACCESS_WIFI_STATE` — WiFi status
- `BLUETOOTH` / `BLUETOOTH_CONNECT` — Bluetooth status
- `RECORD_AUDIO` — Mic access monitoring (runtime permission)
- `CAMERA` — Camera access monitoring (runtime permission)

## Architecture: MVVM
**Package Structure:**
```
app/src/main/java/com/ezworksafe/
├── data/
│   ├── repository/    # SensorRepository interface + SystemSensorRepository impl
│   └── model/         # SensorStatus sealed class, SensorType enum
├── ui/
│   ├── viewmodel/     # SensorViewModel (exposes StateFlow per sensor)
│   └── view/          # MainActivity + StatusDashboard (Compose) + EzWorkSafeTheme
├── service/           # MonitoringService (foreground, pushes widget updates)
├── widget/            # SensorWidget (Glance), SensorWidgetReceiver, WidgetState singleton
└── util/              # PermissionHelper
```

**Real-time Pattern:** `callbackFlow` wrapping system callbacks:
- WiFi: `WifiManager` + `BroadcastReceiver`
- Bluetooth: `BluetoothAdapter` + `BroadcastReceiver`
- Mic: `AudioManager` + `AudioRecordingCallback` (API 24+)
- Camera: `CameraManager` + `AvailabilityCallback`

## Android Gotchas
- Mic/Camera need **runtime permission** requests, not just manifest
- Background monitoring may need **foreground service** + battery whitelist
- Camera/mic monitoring APIs vary across **API levels**
- Some `BroadcastReceiver` actions restricted since Android 8+
- **Android 16 AppOps limitation**: `checkOpNoThrow()` returns `MODE_IGNORED` for background processes regardless of actual privacy toggle state. This is server-side enforced with no client-side workaround. Mic/Cam privacy toggle changes are NOT detectable from background — the widget's right section (Mic/Cam) shows stale state until the user opens the app.
- **Widget dual-path architecture**: Glance `AppWidget` provides initial render (via WorkManager, ~45s delay). Real-time updates use direct `RemoteViews` push from `MonitoringService.pushWidgetUpdate()`, bypassing Glance entirely. See `app/src/main/res/layout/widget_sensor_status.xml` for the RemoteViews layout.
- **Widget sections**: Left section (WiFi/BT) updates in real-time via system broadcasts. Right section (Mic/Cam+timestamp) only reflects state from the last foreground refresh. The divider visually separates the two.
- **Notification "Refresh" action**: The foreground notification includes a "Refresh" button that opens `MainActivity`. The activity's `ON_RESUME` handler calls `refreshSensorFlows()` → `repository.refresh()`, which increments `refreshTrigger` causing all sensor flows to re-emit via `flatMapLatest`. This brings Mic/Cam state up to date.
- **`WidgetState` singleton**: Shared mutable state between `MonitoringService` (writer, `Dispatchers.Main`) and `SensorWidget` (reader, Glance thread). Fields are `@Volatile` for visibility. `lastRefreshTime` is set by `repository.refresh()` but NOT by `pushWidgetUpdate()` — the timestamp may lag behind the widget display.

## Plan Execution
Default: **subagent-driven-development** — fresh subagent per task with two-stage
review (spec compliance → code quality) after each. See implementation plans in
`docs/superpowers/plans/`.

## Implementation Plan
See [docs/PLAN.md](docs/PLAN.md) for the full implementation breakdown.

## Testing
- Unit: ViewModel + Repository (JUnit + Mockito + `runTest`)
- E2E: Compose UI + `FakeSensorRepository` (instrumented, `connectedDebugAndroidTest`)
- Widget: Provider metadata + `WidgetState` label verification (instrumented)
- Notification: `dumpsys activity services` via `UiAutomation.executeShellCommand` (E2E)
