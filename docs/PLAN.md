# ezWorkSafe Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Native Kotlin Android app (MVVM) that displays real-time status of WiFi, Bluetooth, Microphone access,
and Camera access for work safety/privacy monitoring.

**Architecture:** Single-Activity MVVM app with a `SensorRepository` wrapping Android system services via `callbackFlow`
for reactive updates, a `SensorViewModel` exposing `StateFlow<SensorStatus>` per sensor, and a Jetpack Compose dashboard
UI. Runtime permission requests for MIC and CAMERA. Optional foreground service for background monitoring.

**Tech Stack:** Kotlin, Coroutines + Flow, AndroidX Lifecycle (ViewModel + StateFlow), Jetpack Compose, Jetpack Glance
(widget), JUnit + Mockito (testing), Gradle Kotlin DSL.

---

## Post-Plan Additions

The following were implemented beyond the original plan:

| Feature | Details |
|---------|---------|
| **Home screen widget** | 1×4 Jetpack Glance widget (`widget/`) with dual update path: Glance for initial render, RemoteViews push from service for real-time updates. |
| **Widget split sections** | Left (WiFi/BT, real-time broadcasts) and right (Mic/Cam+timestamp, foreground-refreshed) to work around Android 16 AppOps background restriction. |
| **Compact widget** | 1×1 Glance widget (`CompactWidget.kt`, `CompactWidgetReceiver.kt`) with colored dots and labels, no status text. |
| **Foreground service enhancement** | `MonitoringService` now hosts a `combine` collector that aggregates all 4 sensor flows, pushes to `WidgetState`, and updates the notification text in real time. |
| **SensorRepository refactor** | Changed from class to `interface` + `SystemSensorRepository` implementation, with `refreshTrigger` + `flatMapLatest` for permission re-checks on resume. |
| **`SensorStatus.Blocked` state** | Added for WiFI/BT hardware-off status (distinct from the prior Inactive status, since removed). Color: orange `0xFFFF9800`. |
| **App icon** | Custom adaptive icon (shield + eye) with PNG fallbacks. |
| **Dark mode** | Material You dynamic colors on API 31+, green-seeded fallback. |
| **Notification "Refresh" action** | Notification includes a button that opens MainActivity → triggers `refreshSensorFlows()`. |
| **E2E tests** | Compose UI tests for dashboard (all sensor states), widget provider metadata, notification verification via `dumpsys`, and theme. |
| **`FakeSensorRepository`** | Deterministic test double with `setStatus()` for E2E and unit tests. |
| **Configuration cache** | `org.gradle.configuration-cache=true` — builds complete in <1s on cache hit. |

### Current SDK Versions (from `app/build.gradle.kts`)

| Config | Value |
|--------|-------|
| `compileSdk` | 37 |
| `minSdk` | 26 |
| `targetSdk` | 35 |

### Key File Additions Not in Original Structure

```
app/src/main/java/com/ezworksafe/
└── widget/
    ├── BuildWidgetRemoteViews.kt  # RemoteViews construction
    ├── CompactWidget.kt           # 1×1 GlanceAppWidget
    ├── CompactWidgetReceiver.kt   # GlanceAppWidgetReceiver for compact widget
    ├── SensorWidget.kt            # 1×4 GlanceAppWidget composable layout
    ├── SensorWidgetReceiver.kt    # GlanceAppWidgetReceiver + service starter
    ├── WidgetColors.kt            # Centralized widget color palette
    └── WidgetState.kt             # Singleton for cross-component state sharing
```

```
app/src/androidTest/
└── java/com/ezworksafe/
    ├── service/
    │   └── MonitoringServiceNotificationE2eTest.kt
    ├── ui/view/
    │   ├── StatusDashboardE2eTest.kt
    │   ├── PermissionRefreshE2eTest.kt   # @Ignored (API 36 shell restriction)
    │   ├── QuickSettingsToggleE2eTest.kt
    │   └── EzWorkSafeThemeTest.kt
    ├── data/repository/
    │   └── FakeSensorRepository.kt
    └── widget/
        ├── CompactWidgetE2eTest.kt
        └── SensorWidgetE2eTest.kt
```

```
app/src/main/res/
├── drawable/
│   ├── ic_launcher_foreground.xml     # Adaptive icon foreground (shield + eye)
│   └── ic_launcher_background.xml     # Adaptive icon background (green)
├── mipmap-*/ic_launcher.webp          # Fallback PNG icons
└── xml/
    ├── widget_info_sensor.xml         # Bar widget provider metadata
    └── widget_info_compact.xml        # Compact widget provider metadata
```

### Known Limitations

`AppOpsManager.checkOpNoThrow()` returns `MODE_IGNORED` for background processes on Android 16 regardless of actual
toggle state (server-side enforcement). No client-side workaround exists. `SensorPrivacyManager` is `@SystemApi`. This
is why Mic/Cam show stale state in the widget's right section until the user opens the app.

**Sensor status semantics (N-1 by design):** "Active" means permission granted + AppOps allows hardware use, not that
hardware is currently in use. The app reports whether sensors *can be accessed*, matching its workplace safety
monitoring purpose vs. a usage monitor. See [DEVELOPMENT.md](DEVELOPMENT.md#sensor-monitoring).

---

## Original Plan Tasks

All 12 tasks from the original implementation plan have been fully implemented. Code snippets, SDK versions, and
architecture descriptions in the original task sections are outdated (the project evolved significantly beyond the
initial plan). See [DEVELOPMENT.md](DEVELOPMENT.md) for current build, architecture, and testing documentation.
