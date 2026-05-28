# Widget Vertical Centering Issue

**Last updated:** 2026-05-18 **Branch:** `fix/widget-alignment` **Related files:**
- `app/src/main/res/layout/widget_sensor_status.xml` — RemoteViews live-data layout
- `app/src/main/res/layout/widget_initial_layout.xml` — RemoteViews placeholder layout
- `app/src/main/java/com/ezworksafe/widget/SensorWidget.kt` — Glance Compose layout
- `app/src/main/java/com/ezworksafe/service/MonitoringService.kt` — `pushWidgetUpdate()`
- `app/src/main/java/com/ezworksafe/widget/SensorWidgetReceiver.kt` — `onUpdate()` entry point

---

## Symptom (original)

Before the fix, the widget content was shifted upward when live sensor data was pushed via RemoteViews. The timestamp
overlay overlapped the "Active" status text.

---

## Rendering Pipeline

When a widget is added to the home screen:

```
SensorWidgetReceiver.onUpdate()
  ├── startForegroundService(MonitoringService)        // starts sensor observation
  ├── super.onUpdate()                                  // triggers Glance render
  │     └── SensorWidget.provideGlance()                // Glance Compose layout ("WiFi2")
  └── appWidgetManager.updateAppWidget(initial_layout)  // overrides with XML placeholder ("WiFi1")

...MonitoringService starts observing sensors...

MonitoringService.pushWidgetUpdate()
  └── appWidgetManager.updateAppWidget(sensor_status)   // pushes live data layout ("WiFi3")
```

### Stage-by-stage mapping

| Stage | Layout source | Label | What renders |
|-------|--------------|-------|-------------|
| 1 | Glance (`SensorWidget.kt`) | WiFi2 | Glance Compose layout, no timestamp. Colored dots + real status from `WidgetState`. |
| 2 | `widget_initial_layout.xml` | WiFi1 | Grey dots, "..." text, no timestamp. Pushed in `onUpdate()` and quickly overwritten. |
| 3 | `widget_sensor_status.xml` | WiFi3 | Colored dots, real status text, timestamp overlays at bottom of right section. |

Stage 3 is the primary visible state (pushed within seconds of opening the app).

---

## Root Cause

The old layout used a vertical `LinearLayout` with `sensors_row` at `weight=1` and a separate timestamp row below. The
timestamp row stole vertical space from `sensors_row`, shifting all sensor labels above true vertical center by roughly
half the timestamp height.

---

## Solution: FrameLayout overlay in right section

### `widget_sensor_status.xml` (Stage 3 — RemoteViews push)

Restructured from a vertical `LinearLayout` (sensors_row + timestamp bar) to a **horizontal `LinearLayout`** where the
timestamp overlays at the bottom of the right section via `layout_gravity`:

```
LinearLayout (root, gravity="center_vertical", padding=8dp)
├── LinearLayout (left_section, weight=1, wrap_content)
│   ├── paddingTop=14dp, paddingBottom=14dp  ← symmetrical gap for centering
│   ├── cell_wifi (weight=1)
│   ├── inner divider (1dp)
│   └── cell_bt (weight=1)
├── TextView (section_divider, 2dp, match_parent)
└── FrameLayout (right_section, weight=1, wrap_content, paddingTop=14dp)
    ├── LinearLayout (inner_row, fillMaxWidth, wrap_content, marginBottom=14dp)
    │   ├── cell_mic (weight=1)
    │   ├── inner divider (1dp)
    │   └── cell_cam (weight=1)
    └── TextView (last_updated, layout_gravity="bottom|center_horizontal", 8sp)
```

Key details:
- `left_section` and `right_section` both have `paddingTop=14dp` to balance the `paddingBottom`/`marginBottom`, making both sections the same total height (so they center at the same level in the root)
- `right_section` is `FrameLayout` with `layout_gravity="bottom|center_horizontal"` on the timestamp, placing it below the sensor content
- `marginBottom=14dp` on `inner_row` creates space between the sensor content and the timestamp (not `paddingBottom` on the FrameLayout — that doesn't separate children)
- `left_section` has `paddingBottom=14dp` to match the right section's total height for level centering

### `SensorWidget.kt` (Stage 1 — Glance Compose)

Restructured to use `Box` + `contentAlignment = Alignment.Center` in the right section. The timestamp was removed (not
visible in the Glance stage anyway).

Key details:
- Outer `Row` has `verticalAlignment = Alignment.Vertical.CenterVertically`
- Left section: `Row` with `CenterVertically`
- Right section: `Box` with `contentAlignment = Alignment.Center` containing only the inner sensor row
- No timestamp — the Glance stage is always overwritten by the initial XML layout within minutes of widget placement

### `widget_initial_layout.xml` (Stage 2 — initial XML placeholder)

Unchanged. No timestamp, both sections use `match_parent` height with `gravity="center_vertical"`. Centering is
naturally correct because there's no timestamp stealing space.

---

## Verification

Confirmed via `android layout -p` layout dump on Pixel_8_Pro emulator:

| Element | Stage 3 (WiFi3) y-position | Stage 1 (WiFiN) y-position |
|---------|---------------------------|---------------------------|
| Labels (WiFi, BT, Mic, Cam) | y=708 | y=702 |
| Status (Active) | y=747 | y=702 (no status visible) |
| Timestamp (Updated ...) | y=790 | N/A (removed) |

- Stage 3: All labels and status lines are at identical y-positions across left and right sections — vertically centered.
- Stage 1: Left and right sections were misaligned until timestamp was removed from the Glance layout (now both at y=702).
- Stage 2: Not measured directly, but structurally identical to Stage 3's working centering behavior.

---

## Key Learnings

1. **`paddingBottom` on a `FrameLayout` does NOT create space between children** — it extends the content area; children within the content area still occupy the same region. Use `marginBottom` on inner children instead.
2. **Glance `Box` does not support per-child `align()`** — use `contentAlignment` for all children or nest layouts.
3. **Widget host allocates more space than content needs** — the launcher gives the widget a full grid cell, which is taller than the ~100dp of actual sensor content. Centering within this tall allocation naturally leaves empty space above and below.
4. **Sections must match in total height** for `gravity="center_vertical"` to center their content at the same level. Unequal heights cause a vertical offset even when both are centered independently.
5. **`android layout -p`** is the most reliable way to debug widget layout — it gives exact pixel positions for all visible elements.

---

## How to Inspect Widget Layout

### `android layout` CLI tool

```bash
# Basic dump
android layout -p

# Target emulator
android layout -p --device=emulator-5554

# Search for specific elements
android layout -p | grep -B1 -A3 "label_wifi\|status_mic\|last_updated"
```

**What to look for in the output:**
- Left and right section labels at the same y-position → confirmed aligned centering
- `last_updated` y-position well below `status_*` → no overlap
- `widget_root` center → confirms widget bounds

### Screenshots

Takes full screenshot, then extracts widget region by detecting the `#1a1a2e` background:

```bash
adb exec-out screencap -p > screenshot.png
python3 << 'PYEOF'
from PIL import Image
import numpy as np
img = Image.open('screenshot.png').convert('RGB')
arr = np.array(img)
target = np.array([26, 26, 46])  # #1a1a2e
mask = np.all(arr == target, axis=2)
rows = np.where(mask.sum(axis=1) > arr.shape[1] * 0.3)[0]
cols = np.where(mask.sum(axis=0) > 0)[0]
widget = img.crop((cols[0], rows[0], cols[-1]+1, rows[-1]+1))
widget.save('widget.png')
PYEOF
```

---

## Running Tests

```bash
./gradlew lint                         # lint checks
./gradlew test                         # unit tests
./gradlew connectedDebugAndroidTest    # E2E tests on emulator/device
```
