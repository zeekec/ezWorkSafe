# ezWorkSafe — Development Guide

## Build

```bash
./gradlew build         # full build (compile + lint + unit tests)
./gradlew lint          # lint checks only
./gradlew installDebug  # build and install debug APK to connected device
```

### App version

The app version is **derived from the nearest `vX.Y.Z` git tag** at configuration time in `app/build.gradle.kts`. There
is deliberately no version in `gradle.properties` — a second source of truth is what let the file drift to `0.1.0` while
v0.2.0 was already released.

| Field | Value | Example |
|---|---|---|
| `versionName` | the tag, plus SemVer build metadata counting commits past it | `0.2.0+7` |
| `versionCode` | `major*10000 + minor*100 + patch` from the tag only | `200` |

`versionCode` deliberately ignores the commit count. Folding it in would widen the encoding and change the value of
already-published releases, which Play Store rejects outright. It stays monotonic and tag-derived so a given tag always
means the same code.

Notes:

- On a tagged commit the count is `0` and the `+0` suffix is omitted, so a release reads plain `0.2.0`.
- **The count is relative to the tag**, so `+7` means "7 commits past v0.2.0", not "commit 7". It resets at each tag.
- **Untagged dev builds report the last released version.** On `main` after v0.2.0, a build is `0.2.0+N` with no
  `-SNAPSHOT` marker; the tag is authoritative by design.
- **Minor and patch must stay below 100.** `v0.150.0` fails the build with an explanatory error rather than silently
  colliding with `v0.1.50`'s code.
- **Config cache handles the git lookup correctly.** Gradle tracks the external `git` process as a configuration
  cache input, so committing invalidates the entry and the `+N` count updates on the next build. Verified: the count
  advanced `+7` to `+8` across a commit with no extra flags, and the cache is still reused when nothing has changed.
- Outside a git checkout, or when no `vX.Y.Z` tag is reachable, the version falls back to `0.0.0` / code `1` and logs
  a warning. In CI this nearly always means `actions/checkout` is missing `fetch-depth: 0`, which fetches **no tags**.

Verify what actually landed in a build with `aapt2 dump badging app/build/outputs/apk/release/*.apk`, or read
`output-metadata.json` next to the APK.

### Release build

`./gradlew assembleRelease` produces a signed APK at `app/build/outputs/apk/release/` (unsigned if `keystore.properties`
is absent).

Automated releases are **tag-driven** (see `.github/workflows/release.yml`):

1. Push a semver tag prefixed with `v` matching the release, e.g. `git tag v0.2.0 && git push origin v0.2.0`.
2. CI runs lint + unit tests, builds the signed APK (version stamped from the tag, so it always matches),
   and creates a GitHub Release with auto-generated notes and the APK attached.

The release job also passes `-PversionName` / `-PversionCode` explicitly from the tag. Those override the git-derived
values and are a safety net: if git metadata were ever unavailable on the runner, a tagged release would still be
stamped correctly instead of falling back to `0.0.0`.

The latest build is downloadable from https://github.com/zeekec/ezWorkSafe/releases/latest.

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

57 tests across:
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

32 tests across dashboard Compose UI, widget provider metadata, notification verification via `dumpsys`, quick settings
toggle, permission refresh, and themes.

A scheduled CI workflow (`.github/workflows/e2e.yml`) runs these tests weekly (Monday 12:00 UTC) on a KVM-accelerated
emulator. Manual trigger also available via the Actions tab.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose + Material 3 |
| Widget | Glance AppWidget |
| Architecture | MVVM (Repository → ViewModel → Composable) |
| Async | Kotlin Coroutines + StateFlow |
| Build | Gradle 9.7.1 + AGP 9.4.1 |
| Min SDK | 26 |
| Target SDK | 36 |
| Compile SDK | 37 |
| Kotlin | 2.4.20 |

---

## Sensor Monitoring

| Sensor | Mechanism | Permission |
|--------|-----------|------------|
| WiFi | `WifiManager` + `BroadcastReceiver` (real-time via broadcasts) | `ACCESS_WIFI_STATE` (manifest only) |
| Bluetooth | `BluetoothAdapter` + `BroadcastReceiver` (real-time via broadcasts) | `BLUETOOTH_CONNECT` (runtime, API 31+) |
| Microphone | AppOps `checkOpNoThrow` (snapshot-only, no callback) | `RECORD_AUDIO` (runtime) |
| Camera | AppOps `checkOpNoThrow` via `CameraManager.cameraIdList` (snapshot-only, no callback) | `CAMERA` (runtime) |

WiFi and Bluetooth are real-time via `callbackFlow` + `BroadcastReceiver`. Mic and Camera are snapshot-only — they emit
once on subscription and re-emit only via `refreshTrigger` + `flatMapLatest`. No `AudioRecordingCallback` or
`AvailabilityCallback` is registered.

**Rationale:** The app reports whether the hardware *can be accessed*, not whether it's in use. "Active" means
permission granted AND AppOps allows access. Mic/Cam check permission + AppOps privacy toggle, then emit `Active` if
both allow. No callbacks are registered — they were removed to avoid spontaneous state changes from callback events.
When backgrounded, `WhileSubscribed(5_000)` stops collection; state refreshes only on `ON_RESUME` via `refresh()` →
`flatMapLatest` re-subscription.

A foreground polling loop in `MainActivity` (`repeatOnLifecycle(STARTED)` + `delay(2_000)` + `viewModel.refresh()`)
re-queries Mic/Cam while visible. When the app goes below STARTED, the coroutine cancels immediately — avoiding the
Android 16 limitation where `checkOpNoThrow()` returns `MODE_IGNORED` for background processes regardless of the actual
toggle state. This is server-side enforced with no client-side workaround.

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

Foreground polling loop in `MainActivity` (`repeatOnLifecycle(STARTED)` + `delay(2_000)` + `viewModel.refresh()`)
periodically re-queries Mic/Cam while visible. On `ON_RESUME`, an immediate single refresh fires.

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

`checkOpNoThrow()` returns `MODE_IGNORED` for background processes regardless of actual privacy toggle state. Mic/Cam
privacy toggle changes are not detectable from background — the widget's right section shows stale state until the user
opens the app. This is server-side enforced with no client-side workaround.

---

## Scripts

| Script | Purpose |
|--------|---------|
| `scripts/wrap-markdown.py` | Word-wrap all markdown files to 120 characters, preserving code blocks, tables, and lists. |

```bash
python3 scripts/wrap-markdown.py
```

Run after editing any markdown file to keep line lengths consistent.

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
