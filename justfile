set default-list

# Build the project
build:
    ./gradlew build

# Run Android lint checks
lint:
    ./gradlew lint

# Run unit tests
test:
    ./gradlew test

# Build the debug variant and install to a connected device
install-debug:
    ./gradlew installDebug

# Generate the unit test coverage report (JaCoCo)
coverage:
    ./gradlew createDebugUnitTestCoverageReport

# Run E2E instrumented tests on a device/emulator
e2e:
    ./gradlew connectedDebugAndroidTest

# Build and install the signed release variant (requires keystore.properties)
install-release:
    ./gradlew installRelease

# Uninstall the app from a connected device
uninstall:
    adb uninstall com.ezworksafe

# Start an emulator (default: Pixel_8_Pro_Android_17; pass a name to override)
emulator-start name="Pixel_8_Pro_Android_17":
    android emulator start "{{name}}"

# Stop an emulator (default: Pixel_8_Pro_Android_17; pass a name to override)
emulator-stop name="Pixel_8_Pro_Android_17":
    android emulator stop "{{name}}"
