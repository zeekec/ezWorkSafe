# ezWorkSafe - Implementation Plan

## Goal
Build a Native Kotlin Android app (MVVM) that displays real-time status of WiFi, Bluetooth, Microphone access, and Camera access for work safety/privacy monitoring.

---

## Phase 1: Project Setup
- [ ] Initialize Android project (min SDK 24 for mic callback support)
- [ ] Configure build.gradle with required dependencies (Kotlin, Coroutines, Lifecycle)
- [ ] Setup package structure (`data/`, `ui/`, `service/`, `util/`)
- [ ] Create AGENTS.md and PLAN.md

## Phase 2: Permissions
- [ ] Declare permissions in AndroidManifest.xml:
  - `ACCESS_WIFI_STATE`
  - `BLUETOOTH` / `BLUETOOTH_CONNECT`
  - `RECORD_AUDIO`
  - `CAMERA`
- [ ] Implement runtime permission request flow for RECORD_AUDIO and CAMERA
- [ ] Add permission status checking utility in `util/`

## Phase 3: Data Layer
- [ ] Define status data classes in `data/model/` (SensorStatus: enum of ACTIVE/INACTIVE/DENIED)
- [ ] Create `SensorRepository` in `data/repository/`:
  - WiFi status via `WifiManager`
  - Bluetooth status via `BluetoothAdapter`
  - Mic access via `AudioManager.AudioRecordingCallback` (API 24+)
  - Camera access via `CameraManager.AvailabilityCallback`
- [ ] Wrap system callbacks in `callbackFlow` for reactive updates

## Phase 4: ViewModel Layer
- [ ] Create `SensorViewModel` in `ui/viewmodel/`
- [ ] Expose `StateFlow` or `LiveData` for each sensor status
- [ ] Handle lifecycle awareness and cleanup of system callbacks
- [ ] Integrate permission state into status reporting

## Phase 5: UI Layer
- [ ] Create main Activity (`ui/view/MainActivity`)
- [ ] Build status dashboard screen:
  - 4 status indicators (WiFi, Bluetooth, Mic, Camera)
  - Visual state: Active (green), Inactive (gray), Denied (red)
- [ ] Choose UI approach: Jetpack Compose (recommended) or XML layouts
- [ ] Add permission request UI flow if permissions not granted

## Phase 6: Real-Time Updates
- [ ] Register `BroadcastReceiver` for WiFi/Bluetooth system broadcasts
- [ ] Register `AudioRecordingCallback` for mic access changes
- [ ] Register `AvailabilityCallback` for camera access changes
- [ ] Ensure callbacks survive configuration changes (ViewModel scope)
- [ ] Test rapid state transitions

## Phase 7: Foreground Service (Optional)
- [ ] Implement foreground service for background monitoring (if needed)
- [ ] Add battery optimization whitelist prompt
- [ ] Show persistent notification with summary status

## Phase 8: Testing
- [ ] Unit tests for `SensorViewModel`
- [ ] Unit tests for `SensorRepository` (with mocked system services)
- [ ] Integration tests for permission handling
- [ ] UI tests (Espresso or Compose Test)

## Phase 9: Polish & Release
- [ ] Run `./gradlew lint` and fix issues
- [ ] Run `./gradlew test` and ensure all pass
- [ ] Add app icon and theming
- [ ] Test on physical device (real sensor access needed)
- [ ] Build release APK/AAB

---

## Notes
- Use Context7 MCP for Android SDK API lookups during implementation
- Camera/Mic monitoring APIs vary by API level — check compatibility
- Some BroadcastReceiver actions restricted since Android 8+
