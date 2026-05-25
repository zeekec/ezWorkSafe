# ezWorkSafe — Development Guide

## Build

```bash
./gradlew build         # full build (compile + lint + unit tests)
./gradlew lint          # lint checks only
./gradlew installDebug  # build and install debug APK to connected device
```

### Release build

Create `keystore.properties` from the template:

```bash
cp keystore.properties.template keystore.properties
# edit keystore.properties with your signing config
./gradlew assembleRelease
```

The signed APK is at `app/build/outputs/apk/release/`.

### Code coverage

```bash
./gradlew createDebugUnitTestCoverageReport
```

Report at `app/build/reports/coverage/test/debug/index.html`.

---

## Tests

### Unit

```bash
./gradlew test
```

52 tests across:
- ViewModel + Repository + Service notification (JUnit, Mockito, Robolectric, `runTest`)
- Widget state, format utils, permission helper

### E2E (instrumented)

Requires a connected device or running emulator:

```bash
android emulator start Pixel_8_Pro &
adb wait-for-device
while [ "$(adb shell getprop sys.boot_completed)" != "1" ]; do
  sleep 2
done
./gradlew :app:connectedDebugAndroidTest
```

22 tests across dashboard Compose UI, widget provider metadata, notification verification via `dumpsys`, and themes.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose + Material 3 |
| Widget | Glance AppWidget |
| Architecture | MVVM (Repository → ViewModel → Composable) |
| Async | Kotlin Coroutines + StateFlow |
| Build | Gradle 9.5.1 + AGP 9.2.1 |
| Min SDK | 26 |
| Target SDK | 35 |
| Compile SDK | 36 |
| Kotlin | 2.3.21 |

---

## Sensor Monitoring

| Sensor | Mechanism | Permission |
|--------|-----------|------------|
| WiFi | `WifiManager` + `BroadcastReceiver` (real-time via broadcasts) | `ACCESS_WIFI_STATE` (manifest only) |
| Bluetooth | `BluetoothAdapter` + `BroadcastReceiver` (real-time via broadcasts) | `BLUETOOTH_CONNECT` (runtime, API 31+) |
| Microphone | AppOps `checkOpNoThrow` (snapshot-only, no callback) | `RECORD_AUDIO` (runtime) |
| Camera | AppOps `checkOpNoThrow` via `CameraManager.cameraIdList` (snapshot-only, no callback) | `CAMERA` (runtime) |

WiFi and Bluetooth are real-time via `callbackFlow` + `BroadcastReceiver`. Mic and Camera are snapshot-only — they emit once on subscription and re-emit only via `refreshTrigger` + `flatMapLatest`. No `AudioRecordingCallback` or `AvailabilityCallback` is registered. See [security.md](security.md#n-1-info-cameramic-monitoring-shows-active-based-on-permissionappops-not-actual-hardware-usage) for rationale.

---

## Architecture (MVVM)

```
app/src/main/java/com/ezworksafe/
├── data/
│   ├── repository/     # SensorRepository interface + SystemSensorRepository impl
│   └── model/          # SensorStatus sealed class, SensorType enum
├── ui/
│   ├── viewmodel/      # SensorViewModel (exposes StateFlow per sensor)
│   └── view/           # MainActivity + StatusDashboard + AppInfoDialog + EzWorkSafeTheme
├── service/            # MonitoringService (foreground, pushes widget updates)
├── widget/             # SensorWidget (Glance), SensorWidgetReceiver, WidgetState singleton
└── util/               # PermissionHelper, FormatUtils
```

### Data flow

```
WiFi/BT: system broadcasts → BroadcastReceiver
Mic/Cam: snapshot (permission + AppOps)
  → callbackFlow
  → flatMapLatest (refreshTrigger)
  → StateFlow (SensorViewModel)
  → Compose UI (StatusDashboard)
  → combine (MonitoringService)
  → WidgetState → RemoteViews push
```

Foreground polling loop in `MainActivity` (`repeatOnLifecycle(STARTED)` + `delay(2_000)` + `viewModel.refresh()`) periodically re-queries Mic/Cam while visible. On `ON_RESUME`, an immediate single refresh fires.

---

## Permissions

| Permission | When requested | Purpose |
|------------|---------------|---------|
| `RECORD_AUDIO` | App launch | Check microphone accessibility (never records) |
| `CAMERA` | App launch | Check camera accessibility (never captures) |
| `BLUETOOTH_CONNECT` | App launch (Android 12+) | Read Bluetooth on/off state |

WiFi status uses `ACCESS_WIFI_STATE`, a normal permission granted at install time.

---

## Widgets

Two home screen widgets:

- **Bar widget** — horizontal bar with two sections:
  - **Left** — WiFi and Bluetooth status (updates in real time via system broadcasts)
  - **Right** — Microphone and Camera status (reflects last foreground refresh; Android 16 privacy toggles are not detectable from background)
  - A divider separates the two sections. A timestamp shows when the right section was last refreshed.
- **Compact widget** — 1×1 square showing colored dots + labels for all four sensors (WiFi, BT, Mic, Cam). No status text or timestamp.

### Widget update paths

1. **Initial render** — Glance `AppWidget` via WorkManager (~45s delay)
2. **Real-time updates** — `MonitoringService.pushWidgetUpdate()` pushes `RemoteViews` directly, bypassing Glance
3. **On refresh** — tapping "Refresh" in the notification opens `MainActivity`, which triggers `repository.refresh()` → `flatMapLatest` restarts all sensor flows

### Known limitation (Android 16)

`checkOpNoThrow()` returns `MODE_IGNORED` for background processes regardless of actual privacy toggle state. Mic/Cam privacy toggle changes are not detectable from background — the widget's right section shows stale state until the user opens the app. This is server-side enforced with no client-side workaround.

---

## Reference Docs

| Document | Description |
|----------|-------------|
| [API.md](API.md) | Full API reference: every system service, Jetpack library, Kotlin construct, and test framework used |
| [PLAN.md](PLAN.md) | Original implementation plan and post-plan feature additions |
| [security.md](security.md) | Full security audit (findings, fixes, remaining low-priority items) |
| [review.md](review.md) | Code review findings, test coverage gaps, build health |
| [widget_spacing.md](widget_spacing.md) | Widget vertical centering deep-dive |
| [AGENTS.md](../AGENTS.md) | AI agent workflow instructions, build commands, Android gotchas |
