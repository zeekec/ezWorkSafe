# ezWorkSafe

[![Build](https://img.shields.io/github/actions/workflow/status/zeekec/ezWorkSafe/android.yml?branch=main&logo=github)](https://github.com/zeekec/ezWorkSafe/actions)
[![Download APK](https://img.shields.io/badge/Download-APK-blue?logo=android)](https://github.com/zeekec/ezWorkSafe/actions/workflows/android.yml)
[![codecov](https://codecov.io/gh/zeekec/ezWorkSafe/branch/main/graph/badge.svg)](https://codecov.io/gh/zeekec/ezWorkSafe)
[![License](https://img.shields.io/github/license/zeekec/ezWorkSafe)](https://github.com/zeekec/ezWorkSafe/blob/main/LICENSE)
[![Min SDK](https://img.shields.io/badge/minSDK-26-brightgreen)](https://developer.android.com/guide/topics/manifest/uses-sdk-element)
[![Kotlin](https://img.shields.io/badge/kotlin-2.2-7A1FA2?logo=kotlin&logoColor=white)](https://kotlinlang.org)

> ⚠️ **Personal project** — intended as a workplace privacy awareness tool, not a
> security product.

ezWorkSafe shows you at a glance whether your WiFi, Bluetooth, Microphone, and
Camera are accessible on your Android device. It's designed for workplace
privacy — verify that sensors are blocked when they should be, and know when
your mic or camera could be active.

Everything runs on-device. No data ever leaves your phone.

## Features

- **Real-time status dashboard** — open the app to see all four sensors at once
- **Home screen widgets** — two sizes: a bar widget with all sensor states and a
  compact 1×1 widget with colored dots
- **Persistent notification** — a discreet foreground notification keeps the
  service running; tapping it opens the app and refreshes mic/camera state
- **Dark mode** — automatically adapts to your system theme

## Screenshots

| App dashboard | Home screen widget | Compact widget |
|:---:|:---:|:---:|
| ![App dashboard](docs/screenshot_app.png) | ![Widget](docs/screenshot_widget.png) | ![Compact widget](docs/widget-compact-screenshot.png) |

## Widgets

Two home screen widgets show your sensor status without opening the app. The bar
widget has WiFi and Bluetooth on the left (updating in real time) and
Microphone and Camera on the right (refreshed when you open the app). The
compact 1×1 widget shows colored dots for all four sensors.

## Permissions

| Permission | When requested | Purpose |
|------------|---------------|---------|
| `RECORD_AUDIO` | App launch | Check microphone accessibility (never records) |
| `CAMERA` | App launch | Check camera accessibility (never captures) |
| `BLUETOOTH_CONNECT` | App launch (Android 12+) | Read Bluetooth on/off state |

WiFi status uses `ACCESS_WIFI_STATE`, a normal permission granted at install time.

## Quick start

```bash
./gradlew installDebug
```

Or [download the latest APK](https://github.com/zeekec/ezWorkSafe/actions/workflows/android.yml)
from the latest CI run (look for the **ezWorkSafe-release** artifact).

## For developers

See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) for build commands, test setup,
architecture overview, and technical reference.

## License

Apache 2.0 — see [LICENSE](LICENSE).
