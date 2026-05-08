# Runtime Permission Change Observation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Observe runtime permission changes by restarting sensor flows on app resume.

**Architecture:** `refreshTrigger: MutableStateFlow<Unit>` in `SystemSensorRepository` wrapped with `flatMapLatest` so each call to `refresh()` restarts all sensor flows, re-checking permissions. Activity lifecycle `ON_RESUME` triggers refresh.

**Tech Stack:** Kotlin Coroutines Flow, `flatMapLatest`, `LifecycleEventObserver`.

---

### Task 1: Add `refresh()` to SensorRepository interface

**Files:**
- Modify: `app/src/main/java/com/ezworksafe/data/repository/SensorRepository.kt`

- [ ] **Step 1.1: Add `fun refresh()` to interface**

```kotlin
interface SensorRepository {
    fun observeSensor(type: SensorType): Flow<SensorStatus>
    fun refresh()
}
```

- [ ] **Step 1.2: Commit**

```bash
git add app/src/main/java/com/ezworksafe/data/repository/SensorRepository.kt
git commit -m "feat: add refresh() to SensorRepository interface"
```

---

### Task 2: Implement `refresh()` in SystemSensorRepository

**Files:**
- Modify: `app/src/main/java/com/ezworksafe/data/repository/SystemSensorRepository.kt`

- [ ] **Step 2.1: Add `refreshTrigger` + `flatMapLatest`**

Add to imports:
```kotlin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
```

Add to class:
```kotlin
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
```

Remove the old `observeSensor` method.

- [ ] **Step 2.2: Commit**

```bash
git add app/src/main/java/com/ezworksafe/data/repository/SystemSensorRepository.kt
git commit -m "feat: add refreshTrigger with flatMapLatest to SystemSensorRepository"
```

---

### Task 3: Add no-op `refresh()` to FakeSensorRepository

**Files:**
- Modify: `app/src/androidTest/java/com/ezworksafe/data/repository/FakeSensorRepository.kt`

- [ ] **Step 3.1: Add `override fun refresh() {}`**

```kotlin
class FakeSensorRepository : SensorRepository {
    // ... existing code unchanged

    override fun refresh() { /* no-op */ }
}
```

- [ ] **Step 3.2: Commit**

```bash
git add app/src/androidTest/java/com/ezworksafe/data/repository/FakeSensorRepository.kt
git commit -m "feat: add no-op refresh() to FakeSensorRepository"
```

---

### Task 4: Add `refresh()` to SensorViewModel

**Files:**
- Modify: `app/src/main/java/com/ezworksafe/ui/viewmodel/SensorViewModel.kt`

- [ ] **Step 4.1: Add `fun refresh()`**

Add after existing fields:
```kotlin
    fun refresh() = repository.refresh()
```

- [ ] **Step 4.2: Commit**

```bash
git add app/src/main/java/com/ezworksafe/ui/viewmodel/SensorViewModel.kt
git commit -m "feat: add refresh() to SensorViewModel"
```

---

### Task 5: Wire lifecycle observer in MainActivity

**Files:**
- Modify: `app/src/main/java/com/ezworksafe/ui/view/MainActivity.kt`

- [ ] **Step 5.1: Add imports and DisposableEffect**

Current code around `StatusDashboard`:
```kotlin
                val viewModel: com.ezworksafe.ui.viewmodel.SensorViewModel = viewModel()
                StatusDashboard(viewModel = viewModel)
```

After:
```kotlin
                val viewModel: com.ezworksafe.ui.viewmodel.SensorViewModel = viewModel()
                StatusDashboard(viewModel = viewModel)

                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            viewModel.refresh()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }
```

Or use explicit imports at top of file:
```kotlin
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
```

Then:
```kotlin
                val lifecycleOwner = LocalLifecycleOwner.current
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

- [ ] **Step 5.2: Commit**

```bash
git add app/src/main/java/com/ezworksafe/ui/view/MainActivity.kt
git commit -m "feat: refresh sensor flows on activity resume via lifecycle observer"
```

---

### Task 6: Verify build

- [ ] **Step 6.1: Run assembleDebug and tests**

```bash
./gradlew :app:assembleDebug
./gradlew test
./gradlew :app:connectedDebugAndroidTest
```

- [ ] **Step 6.2: Commit any final fixes**

```bash
git add -A
git commit -m "chore: finalize permission refresh changes"
```
