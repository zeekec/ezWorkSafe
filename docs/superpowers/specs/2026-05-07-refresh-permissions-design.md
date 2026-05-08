# Runtime Permission Change Observation

**Date:** 2026-05-07  
**Status:** Approved by user

## Problem

Camera and mic sensor flows check runtime permissions inside `emitState()` at flow creation time and when system callbacks fire (`onRecordingConfigChanged`, `onCameraAvailable`/`onCameraUnavailable`). If the user revokes a permission from Settings while the app is running, no system callback fires, so the flow continues emitting stale state.

## Solution: `refreshTrigger` + `flatMapLatest`

When the app resumes (user returns from Settings), restart all sensor flows so they re-check permissions.

### Flow

```
User opens Settings → revokes CAMERA → returns to app
  → Activity.onResume() → LifecycleEventObserver(ON_RESUME)
  → ViewModel.refresh() → Repository.refresh()
  → refreshTrigger emits new Unit
  → flatMapLatest cancels old callbackFlow, starts new one
  → new emitState() → checkSelfPermission() → SensorStatus.Denied
```

## Files

| File | Change |
|------|--------|
| `SensorRepository.kt` | Add `fun refresh()` to interface |
| `SystemSensorRepository.kt` | Add `refreshTrigger: MutableStateFlow<Unit>`, wrap flows with `flatMapLatest`, implement `refresh()` |
| `FakeSensorRepository.kt` | Add no-op `refresh()` |
| `SensorViewModel.kt` | Add `fun refresh()` delegating to repository |
| `MainActivity.kt` | Add `DisposableEffect` with `LifecycleEventObserver(ON_RESUME)` calling `viewModel.refresh()` |

### SensorRepository.kt

```kotlin
interface SensorRepository {
    fun observeSensor(type: SensorType): Flow<SensorStatus>
    fun refresh()
}
```

### SystemSensorRepository.kt

```kotlin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest

class SystemSensorRepository(private val context: Context) : SensorRepository {

    private val refreshTrigger = MutableStateFlow(Unit)

    override fun refresh() {
        refreshTrigger.value = Unit
    }

    override fun observeSensor(type: SensorType): Flow<SensorStatus> {
        return refreshTrigger.flatMapLatest {
            when (type) {
                SensorType.WIFI -> observeWifiStatus()
                SensorType.BLUETOOTH -> observeBluetoothStatus()
                SensorType.MICROPHONE -> observeMicStatus()
                SensorType.CAMERA -> observeCameraStatus()
            }
        }
    }
    // ... rest unchanged
}
```

### FakeSensorRepository.kt

```kotlin
class FakeSensorRepository : SensorRepository {
    // ... existing code unchanged
    override fun refresh() { /* no-op */ }
}
```

### SensorViewModel.kt

```kotlin
fun refresh() = repository.refresh()
```

### MainActivity.kt

Inside `setContent`, after the `StatusDashboard` call:

```kotlin
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            viewModel.refresh()
        }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
}
```

## Edge Cases

| Case | Handling |
|------|----------|
| Permission revoked while app is in background | On next resume, all flows restart → re-check → correct state |
| Permission granted while app is in background | Same — picked up on resume |
| Rapid config changes (rotation) | WhileSubscribed(5s) prevents unnecessary restarts; refresh on resume handles the actual permission change |
| API < 23 (no runtime permissions) | Permission checks in emitState still work — both paths return `Inactive`/`Unavailable` based on hardware state |
| FakeSensorRepository in tests | `refresh()` is no-op, tests call `setStatus()` directly |
