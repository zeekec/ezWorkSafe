# Widget Vertical Centering Issue

**Last updated:** 2026-05-15
**Affected branches:** `fix/widget-alignment`
**Related files:**
- `app/src/main/res/layout/widget_sensor_status.xml` — RemoteViews live-data layout
- `app/src/main/res/layout/widget_initial_layout.xml` — RemoteViews placeholder layout
- `app/src/main/java/com/ezworksafe/widget/SensorWidget.kt` — Glance Compose layout
- `app/src/main/java/com/ezworksafe/service/MonitoringService.kt` — `pushWidgetUpdate()`
- `app/src/main/java/com/ezworksafe/widget/SensorWidgetReceiver.kt` — `onUpdate()` entry point

---

## Symptom

When the widget is placed on the home screen, three distinct stages appear:

1. **Centered, grey** — sensor labels (WiFi, BT, Mic, Cam) are vertically centered in the widget with grey placeholder dots and "..." status text
2. **Colored** — the grey placeholders are replaced with colored status dots and real text from sensor readings
3. **Shifted to top** — all four sensor labels jump to the top of the widget and stay there

The transition from stage 1 → stage 2+3 happens when `MonitoringService.pushWidgetUpdate()` pushes a `RemoteViews` update with live sensor data.

---

## Rendering Pipeline

When a widget is added to the home screen:

```
SensorWidgetReceiver.onUpdate()
  ├── startForegroundService(MonitoringService)        // starts sensor observation
  ├── super.onUpdate()                                  // triggers Glance render
  │     └── SensorWidget.provideGlance()                // Glance Compose layout
  └── appWidgetManager.updateAppWidget(initial_layout)  // overrides with XML placeholder

...MonitoringService starts observing sensors...

MonitoringService.pushWidgetUpdate()
  └── appWidgetManager.updateAppWidget(sensor_status)   // pushes live data layout
```

### Stage-by-stage mapping

| Stage | Layout source | What renders |
|-------|--------------|--------------|
| 1 | `widget_initial_layout.xml` | Grey dots, "..." text, no timestamp. Uses `LinearLayout` root + `weight=1` on sensors row. **Centering works here.** |
| 2+3 | `widget_sensor_status.xml` | Colored dots, real status text, timestamp. Uses the layout that's failing to center. |

The Glance layout (`SensorWidget.kt`) is rarely visible because the initial XML layout is pushed immediately after in the same `onUpdate()` call.

---

## What's Been Tried

### Fix 1: Add `gravity="center_vertical"` to right section (commit `ad6017f`)

Changed the inner Mic/Cam container in `widget_sensor_status.xml` from `height=0dp, weight=1` (filling all space) to `height=wrap_content` with parent `gravity="center_vertical"`. This matched what `widget_initial_layout.xml` already did.

**Result:** Did not fix the shift. The right section's Mic/Cam row was still being pushed upward.

### Fix 2: Move timestamp out of `right_section` (commit `c615649`, `24cc82d`, then restructure `52d8250`)

The `last_updated` timestamp was nested inside `right_section`, making it two children (Mic/Cam row + timestamp) that competed for vertical space. Moved the timestamp to root level.

For `widget_sensor_status.xml`, the root was changed from a vertical `LinearLayout` to a `FrameLayout` with `sensors_row` at `match_parent` height and the timestamp overlapping at `layout_gravity="bottom|center_horizontal"`.

**Result:** Still did not fix the shift. Despite `gravity="center_vertical"` on the sensor sections, the content was not centering.

### Fix 3: Return to LinearLayout root with weight-based sizing (current)

`widget_sensor_status.xml` was returned to a vertical `LinearLayout` root with `sensors_row` at `height=0dp, weight=1` (matching the structure of the working `widget_initial_layout.xml`). The timestamp is placed below as a non-weighted sibling.

**Status:** Built and installed to real device (`39111FDJG00F1K`) at commit `131370e`. Not yet verified — user ended session before observing result.

---

## Root Cause Hypothesis

The working layout (`widget_initial_layout.xml`) uses:
- **Root:** `LinearLayout` with `orientation="vertical"`
- **sensors_row:** `height="0dp"`, `layout_weight="1"` — fills all remaining vertical space
- **No timestamp at root level**

The non-working layout used:
- **Root:** `FrameLayout`
- **sensors_row:** `height="match_parent"` — fills FrameLayout height

Both approaches should theoretically give `sensors_row` the same height. However, in practice, the `FrameLayout` + `match_parent` combination does not produce the same centering behavior in the nested `gravity="center_vertical"` LinearLayouts.

The `LinearLayout` + `weight=1` pattern is proven to work (stage 1). The fix aligns all layout paths to use this same structural approach.

---

## How to Inspect Widget Layout

### Layout Inspector (Android Studio)

1. Run the app on the emulator
2. In Android Studio: **View → Tool Windows → Layout Inspector**
3. Select the `ezWorkSafe` process
4. Click the widget on the emulator screen to capture the layout hierarchy
5. Inspect `sensors_row` → `left_section` / `right_section` → verify measured heights vs wrap_content heights
6. Check that `gravity="center_vertical"` is applied on the correct parent views

### `android layout` CLI tool

The `android layout` command dumps the full view hierarchy as JSON. This gives exact measured positions, dimensions, and layout parameters for every view in the widget — no need to guess.

```bash
# Basic dump (prints to stdout)
android layout -p

# Save to file for analysis
android layout -p -o widget_layout.json

# Diff mode — shows only elements that changed since last dump
android layout -d -p

# Target a specific device
android layout -p --device=39111FDJG00F1K
```

**What to look for in the output:**
- `sensors_row` measured height vs available height — confirms whether it fills the container
- `left_section` / `right_section` children — check that `gravity="center_vertical"` is applied and measured positions reflect centering
- `cell_wifi` / `cell_bt` / `cell_mic` / `cell_cam` top/bottom coordinates — are they equidistant from parent center?
- `last_updated` position — is it overlapping or below `sensors_row`?

**Workflow for debugging:**
1. Place widget on home screen (stage 1) → run `android layout` → confirm centered
2. Wait for MonitoringService push (stage 3) → run `android layout` again → compare view positions
3. `android layout -d` highlights only the changed nodes between the two dumps

### `dumpsys` for widget dimensions

```bash
# Get widget info (bounding box)
adb shell dumpsys appwidget | grep -A 20 "ezWorkSafe"

# Get widget view hierarchy (API 31+)
adb shell dumpsys activity containers | grep -A 30 "widget"
```

### RemoteViews debugging

Add logging to `MonitoringService.pushWidgetUpdate()`:

```kotlin
Log.d("WidgetSpacing", "pushWidgetUpdate: ids=${ids.contentToString()}, sensor_row_height=${...}")
```

Or temporarily change `buildWidgetRemoteViews()` to use `widget_initial_layout.xml` instead of `widget_sensor_status.xml` to test whether the centering issue follows the XML or the RemoteViews modification calls.

---

## Current Path Forward

1. **Verify Fix 3** — Build and install the current LinearLayout-based `widget_sensor_status.xml` on the emulator and observe whether centering persists through all three stages
2. **If still broken** — Remove variables one at a time:
   a. Temporarily set background colors in XML instead of via RemoteViews `setInt`
   b. Simplify the layout to isolate which view/attribute causes the centering to break
   c. Compare exact measured heights via Layout Inspector between working initial_layout and broken sensor_status
3. **Unify layout files** — Consider whether all three layout paths (initial XML, live-data RemoteViews, Glance Compose) should share a single XML layout definition to prevent structural divergence
4. **Test** — Add a widget E2E test that verifies vertical centering by checking child view positions or the parent `gravity` attribute

---

## Open Questions

- Does the issue reproduce at different widget heights (different launchers, different grid sizes)?
- Does `setBackgroundColor` via RemoteViews on LinearLayout trigger any layout pass that resets gravity?
- Can the `dumpsys appwidget` output confirm whether `sensors_row` receives the expected height in both layouts?
- Should the initial layout be removed entirely, relying solely on Glance + MonitoringService push?
