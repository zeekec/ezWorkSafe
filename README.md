# ezWorkSafe

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

| App dashboard | Home screen widget |
|:---:|:---:|
| ![App dashboard](docs/screenshot_app.png) | ![Widget](docs/screenshot_widget.png) |

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
./gradlew :app:connectedDebugAndroidTest
```

Or run from Android Studio: Device Manager → start a device → run
`connectedDebugAndroidTest` from the Gradle panel.

## Widget

The home screen widget has two sections:

- **Left** — WiFi and Bluetooth status (updates in real time via system
  broadcasts)
- **Right** — Microphone and Camera status (reflects last foreground refresh;
  Android 16 privacy toggles are not detectable from background)

The divider visually separates the two sections. A timestamp shows when the
right section was last refreshed.

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
