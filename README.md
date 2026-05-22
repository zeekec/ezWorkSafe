# ezWorkSafe

[![Build](https://img.shields.io/github/actions/workflow/status/zeekec/ezWorkSafe/android.yml?branch=main&logo=github)]()
[![codecov](https://codecov.io/gh/zeekec/ezWorkSafe/branch/main/graph/badge.svg)](https://codecov.io/gh/zeekec/ezWorkSafe)
[![License](https://img.shields.io/github/license/zeekec/ezWorkSafe)]()
[![Min SDK](https://img.shields.io/badge/minSDK-26-brightgreen)]()
[![Kotlin](https://img.shields.io/badge/kotlin-2.3-7A1FA2?logo=kotlin&logoColor=white)]()

> ⚠️ **Disclaimer** — Personal project built to explore agentic coding
> workflows. Almost entirely vibe-coded via AI agents. Not a production-grade
> security tool.

Real-time privacy monitoring for Android — view WiFi, Bluetooth, Microphone,
and Camera access status at a glance.

## Features

- **Status dashboard** — real-time sensor state in a clean Compose UI
- **Home screen widget** — glanceable status without opening the app
- **Foreground notification** — persistent reminder with refresh action
- **MVVM architecture** — clean separation of concerns, testable code
- **Privacy-first** — all monitoring happens on-device, no data leaves your phone

## Sensor monitoring

| Sensor | Mechanism | Permission |
|--------|-----------|------------|
| WiFi | `WifiManager` + `BroadcastReceiver` | `ACCESS_WIFI_STATE` (manifest only) |
| Bluetooth | `BluetoothAdapter` + `BroadcastReceiver` | `BLUETOOTH_CONNECT` (runtime, API 31+) |
| Microphone | `AudioManager` + `AudioRecordingCallback` | `RECORD_AUDIO` (runtime) |
| Camera | `CameraManager` + `AvailabilityCallback` | `CAMERA` (runtime) |

## Tech stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose + Material 3 |
| Widget | Glance AppWidget |
| Architecture | MVVM (Repository → ViewModel → Composable) |
| Async | Kotlin Coroutines + StateFlow |
| Build | Gradle 9.5.1 + AGP 9.2.1 |
| Min SDK | 26 |
| Target SDK | 35 |

## Screenshots

| App dashboard | Home screen widget | Compact widget |
|:---:|:---:|:---:|
| ![App dashboard](docs/screenshot_app.png) | ![Widget](docs/screenshot_widget.png) | ![Compact widget](docs/widget-compact-screenshot.png) |

## Build

```bash
./gradlew build
```

### Debug

```bash
./gradlew installDebug
```

### Release

Create `keystore.properties` (see `keystore.properties.template`) with signing
config, then:

```bash
./gradlew assembleRelease
```

## Tests

### Unit

```bash
./gradlew test
```

### E2E (instrumented)

Requires a connected device or running emulator.

```bash
android emulator start Pixel_8_Pro &
adb wait-for-device
while [ "$(adb shell getprop sys.boot_completed)" != "1" ]; do
  sleep 2
done
./gradlew :app:connectedDebugAndroidTest
```

Or run from Android Studio: Device Manager → start a device → run
`connectedDebugAndroidTest` from the Gradle panel.

## Widgets

Two home screen widgets are available:

- **Bar widget** — horizontal bar with two sections:
  - **Left** — WiFi and Bluetooth status (updates in real time via system
    broadcasts)
  - **Right** — Microphone and Camera status (reflects last foreground refresh;
    Android 16 privacy toggles are not detectable from background)
  - A divider separates the two sections. A timestamp shows when the right
    section was last refreshed.
- **Compact widget** — 1×1 square showing colored dots + labels for all four
  sensors (WiFi, BT, Mic, Cam). No status text or timestamp.

## Permissions

| Permission | When requested | Purpose |
|------------|---------------|---------|
| `RECORD_AUDIO` | App launch | Detect microphone usage |
| `CAMERA` | App launch | Detect camera usage |
| `BLUETOOTH_CONNECT` | App launch (API 31+) | Read Bluetooth adapter state |

WiFi status (`ACCESS_WIFI_STATE`) is a normal (not dangerous) permission and
is granted at install time.

## License

Apache 2.0 — see [LICENSE](LICENSE).
