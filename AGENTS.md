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
│   ├── repository/    # SensorRepository (wraps system services)
│   └── model/         # Status data classes
├── ui/
│   ├── viewmodel/     # SensorViewModel (exposes Flow/LiveData)
│   └── view/          # Activity + Compose/XML screens
├── service/           # Foreground service (optional, for background)
└── util/              # Permission helpers, callbacks
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

## Implementation Plan
See [docs/PLAN.md](docs/PLAN.md) for the full implementation breakdown.

## Testing
- Unit: ViewModel + Repository
- Integration: System service wrappers
- UI: Espresso or Compose Test Framework
