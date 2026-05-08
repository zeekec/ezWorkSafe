# Dark Mode Support — ezWorkSafe

**Date:** 2026-05-07  
**Status:** Approved by user

## Overview

Add Material You dynamic color support for dark mode in the Jetpack Compose UI. The app currently hardcodes a light theme — all colors must adapt to system dark mode.

## Approach

**Material You with dynamic colors (API 31+) and seeded fallback (API < 31).**

- API 31+: `dynamicLightColorScheme()` / `dynamicDarkColorScheme()` — picks up user wallpaper
- API < 31: `lightColorScheme()` / `darkColorScheme()` seeded from `#4CAF50` (our brand green)
- Auto-switch via `isSystemInDarkTheme()` — no manual toggle needed

## Files

### Create
- `app/src/main/java/com/ezworksafe/ui/view/EzWorkSafeTheme.kt` — Compose theme composable
- `app/src/main/res/values-night/themes.xml` — dark mode status bar color

### Modify
- `app/src/main/java/com/ezworksafe/ui/view/StatusDashboard.kt` — use `MaterialTheme.colorScheme` instead of hardcoded colors
- `app/src/main/java/com/ezworksafe/ui/view/MainActivity.kt` — wrap `setContent` in `EzWorkSafeTheme`
- `app/src/main/res/values/themes.xml` — change to `DayNight.NoActionBar`

## Theme Composable Design

```kotlin
@Composable
fun EzWorkSafeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) darkColorScheme(primary = Color(0xFF4CAF50))
        else lightColorScheme(primary = Color(0xFF4CAF50))
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
```

## Color Mapping

| Element | Light (current) | Dark (new) |
|---------|----------------|------------|
| Background | `#F5F5F5` hardcoded | `colorScheme.background` |
| Card surface | `Color.White` hardcoded | `colorScheme.surface` |
| Title text | Implicit black | `colorScheme.onSurface` |
| Subtitle text | `Color.Gray` hardcoded | `colorScheme.onSurfaceVariant` |
| Status dot/label | `Color(status.color.toInt())` | Same (unchanged — signal colors) |
| Status bar | `@color/background` | `@color/background` (or dark variant via values-night) |

## Signal Colors (Unchanged)

The four status colors (`Active` green, `Inactive` gray, `Denied` red, `Unavailable` dark gray) are **signal indicators** — they should look the same in light and dark mode so the meaning is consistent.
