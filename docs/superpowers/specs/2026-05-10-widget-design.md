# Widget Design — ezWorkSafe 1×4 Home Screen Widget

**Date:** 2026-05-10
**Status:** Approved design

## Overview

A 1×4 Android home screen widget displaying real-time status of WiFi, Bluetooth,
Microphone, and Camera. Built with Jetpack Glance (Compose-like DSL for widgets).
Updates pushed from the existing `MonitoringService` whenever sensor state changes.

## Architecture

```
WidgetState (singleton object)
  └── var statuses: Map<SensorType, SensorStatus>
  └── set by MonitoringService, read by SensorWidget

MonitoringService (extended)
  └── combine(wifi, bt, mic, cam) → on each emission:
       ├── WidgetState.statuses = mapOf(WIFI→wifi, BT→bt, MIC→mic, CAM→cam)
       └── SensorWidget().updateAll(context)

SensorWidget (GlanceAppWidget)
  └── provideGlance() → read WidgetState.statuses → WidgetContent(statuses)
       ├── 4-column Row
       │    ├── Colored dot (status.color)
       │    ├── Sensor label (WiFi/BT/Mic/Cam)
       │    └── Status label (status.label)
       └── onClick → actionStartActivity<MainActivity>()

SensorWidgetReceiver (GlanceAppWidgetReceiver)
  └── references SensorWidget
  └── onUpdate → provideGlance reads WidgetState (may be initial defaults)
```

## Widget Layout

```
┌──────────────────────────────────────────────────┐
│  ● WiFi    ● BT      ● Mic     ● Cam             │
│  Active    Blocked   Active    Denied             │
└──────────────────────────────────────────────────┘
```

- Dark background matching app surface color
- Rounded corners (12dp)
- Each cell: 10dp colored dot (CircleShape), short sensor label, status label
- Shortened labels: **BT** (Bluetooth), **Mic** (Microphone), **Cam** (Camera)
- Status label text colored same as dot
- Tap anywhere → opens `MainActivity`

## Data Flow

1. `MonitoringService` runs `combine(wifi, bt, mic, cam)` in a coroutine
2. On each emission, store statuses in `WidgetState.statuses` and call
   `SensorWidget().updateAll(context)`
3. `SensorWidget.provideGlance()` reads current statuses from `WidgetState`
   and renders via `WidgetContent()`
4. On first placement (`onUpdate`), `WidgetState` contains initial defaults
   (all `Inactive`) until the service pushes the first update

## Widget Metadata

- **minWidth:** 250dp
- **minHeight:** 40dp
- **updatePeriodMillis:** 0 (no periodic — real-time from MonitoringService)
- **resizeMode:** horizontal
- **configure:** None
- **initialLayout:** `@layout/glance_default_loading_layout`

## Files

| # | File | Action | Purpose |
|---|------|--------|---------|
| 1 | `app/build.gradle.kts` | Edit | Add `glance-appwidget:1.1.1` + `glance-material3:1.1.1` |
| 2 | `AndroidManifest.xml` | Edit | Register `<receiver>` for `SensorWidgetReceiver` |
| 3 | `res/xml/widget_info_sensor.xml` | Create | `AppWidgetProviderInfo` metadata |
| 4 | `widget/SensorWidget.kt` | Create | `GlanceAppWidget` subclass with `WidgetContent` |
| 5 | `widget/SensorWidgetReceiver.kt` | Create | `GlanceAppWidgetReceiver` subclass |
| 6 | `widget/WidgetState.kt` | Create | Singleton holding latest `Map<SensorType, SensorStatus>` |
| 7 | `service/MonitoringService.kt` | Edit | Push sensor updates to widget on emission |

## Edge Cases

| Scenario | Handling |
|----------|----------|
| Service not running | Widget shows last known state; service start triggers first update |
| Widget removed | `GlanceAppWidgetReceiver` handles lifecycle; no cleanup needed |
| No widget placed | `SensorWidget().update()` no-ops silently |
| Multiple widget instances | Update all via `updateAll(context)` |
| Permission changes | Service flow restarts → new emission → widget updates |

## Testing

| Test | Type | What it verifies |
|------|------|-----------------|
| `SensorWidgetTest` | Unit | Renders correct labels/colors for all status states |
| `MonitoringServiceWidgetTest` | Unit | Service calls `SensorWidget.update()` on flow emission |
