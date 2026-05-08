# App Icon Design — ezWorkSafe

**Date:** 2026-05-07  
**Status:** Approved by user

## Overview

Android adaptive launcher icon for ezWorkSafe, a work safety/privacy monitoring app. The icon uses a shield + eye motif on a green background.

## Design

| Element | Detail |
|---------|--------|
| **Background** | Solid `#4CAF50` (green) |
| **Foreground** | White shield with green eye silhouette |
| **Motif** | Shield = safety/protection; Eye = monitoring/awareness |
| **Style** | Flat vector, Material Design-aligned |

## Intent

- Shield conveys the app's core value: keeping the user safe
- Eye conveys the app's function: monitoring sensor access
- Green conveys "all clear"/"safe" — universally associated with safety
- Simple two-shape design reads well at small sizes on home screens

## Technical Assets

### Android Adaptive Icon (API 26+)
- `res/drawable/ic_launcher_foreground.xml` — Shield + Eye vector drawable
- `res/drawable/ic_launcher_background.xml` — Solid `#4CAF50` color drawable
- `res/mipmap-anydpi-v26/ic_launcher.xml` — Adaptive icon definition

### Legacy Fallback (API < 26)
- `res/mipmap-mdpi/ic_launcher.png` (48×48)
- `res/mipmap-hdpi/ic_launcher.png` (72×72)
- `res/mipmap-xhdpi/ic_launcher.png` (96×96)
- `res/mipmap-xxhdpi/ic_launcher.png` (144×144)
- `res/mipmap-xxxhdpi/ic_launcher.png` (192×192)

### Manifest Changes
- Remove `tools:ignore="MissingApplicationIcon"` from `<application>` tag
- Add `android:icon="@mipmap/ic_launcher"` to `<application>` tag

## Foreground Vector Design

The vector drawable (`ic_launcher_foreground.xml`) uses a 108×108 viewport with the 72dp safe zone centered. The shield path starts near the top of the safe zone and extends to the bottom, with the eye positioned at the center of the shield body.

### Shield path
- Top center: approx (54, 18)  
- Shoulders: approx (28, 30)  
- Bottom point: approx (54, 90)  
- Overall: fills roughly 75% of the 108×108 viewport

### Eye path
- Centered approx at (54, 57)  
- Ellipse body filled with green (#4CAF50)  
- White iris center  
- Subtle upper/lower eyelid strokes

## Files to Create

1. `app/src/main/res/drawable/ic_launcher_foreground.xml`
2. `app/src/main/res/drawable/ic_launcher_background.xml`
3. `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
4. `app/src/main/res/mipmap-mdpi/ic_launcher.png`
5. `app/src/main/res/mipmap-hdpi/ic_launcher.png`
6. `app/src/main/res/mipmap-xhdpi/ic_launcher.png`
7. `app/src/main/res/mipmap-xxhdpi/ic_launcher.png`
8. `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`

## Files to Modify

1. `app/src/main/AndroidManifest.xml` — add `android:icon` attribute, remove `tools:ignore`
