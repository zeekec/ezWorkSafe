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

# Start the Pixel_8_Pro emulator
emulator-start:
    android emulator start Pixel_8_Pro

# Stop the Pixel_8_Pro emulator
emulator-stop:
    android emulator stop Pixel_8_Pro
