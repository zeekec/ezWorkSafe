# ezWorkSafe

Android app that displays real-time status of WiFi, Bluetooth, Microphone,
and Camera access for work safety/privacy monitoring.

## Build

```bash
./gradlew build
```

## Run on device

```bash
./gradlew installDebug
```

## Unit tests

```bash
./gradlew test
```

## E2E (instrumented) tests

Requires a connected device or running emulator.

### Start an emulator (CLI)

```bash
# List available virtual devices
emulator -list-avds

# Start one (example)
emulator -avd Pixel_9_API_34 &
```

Wait for the emulator to fully boot, then:

```bash
./gradlew :app:connectedDebugAndroidTest
```

### Using Android Studio

Open Device Manager, create/start a virtual device, then run
`connectedDebugAndroidTest` from the Gradle panel.
