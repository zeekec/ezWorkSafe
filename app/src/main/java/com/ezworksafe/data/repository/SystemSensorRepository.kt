// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.data.repository

import android.app.AppOpsManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Process
import androidx.core.content.ContextCompat
import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Production sensor repository.
 *
 * **Architecture by sensor:**
 * - **WiFi / Bluetooth**: Real-time via [BroadcastReceiver] + [callbackFlow].
 * - **Microphone / Camera**: Snapshot-only — emits a single value on subscription.
 *   No [AudioRecordingCallback] or [AvailabilityCallback] is registered.
 *   Queries permission + [AppOpsManager.checkOpNoThrow] only.
 *
 * **Key invariant:** Sensor status reflects *access* (can the sensor be used?),
 * not *usage* (is another app currently using it?). See [SensorStatus.Active].
 */
class SystemSensorRepository(private val context: Context) : SensorRepository {

    /** Incremented on each [refresh] to trigger re-subscription via [flatMapLatest]. */
    private val refreshTrigger = MutableStateFlow(0)

    override fun refresh() {
        refreshTrigger.value++
    }

    @OptIn(ExperimentalCoroutinesApi::class)
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

    /**
     * Observes WiFi radio state via [WifiManager] and [BroadcastReceiver].
     *
     * Returns [SensorStatus.Active] when the radio is enabled/enabling,
     * [SensorStatus.Blocked] when disabled, [SensorStatus.Unavailable] if
     * the system service is absent.
     */
    private fun observeWifiStatus(): Flow<SensorStatus> = callbackFlow {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiManager == null) {
            trySend(SensorStatus.Unavailable)
            awaitClose { }
            return@callbackFlow
        }

        fun emitState() {
            val enabled = wifiManager.wifiState == WifiManager.WIFI_STATE_ENABLED ||
                wifiManager.wifiState == WifiManager.WIFI_STATE_ENABLING
            trySend(if (enabled) SensorStatus.Active else SensorStatus.Blocked)
        }

        emitState()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                emitState()
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                receiver,
                IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(
                receiver,
                IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION)
            )
        }

        awaitClose { context.unregisterReceiver(receiver) }
    }

    /**
     * Observes Bluetooth radio state via [BluetoothAdapter] and [BroadcastReceiver].
     *
     * On API 31+ the [android.Manifest.permission.BLUETOOTH_CONNECT] runtime
     * permission must be granted before querying the adapter. Returns
     * [SensorStatus.Denied] if the permission is missing.
     */
    private fun observeBluetoothStatus(): Flow<SensorStatus> = callbackFlow {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            trySend(SensorStatus.Denied)
            awaitClose { }
            return@callbackFlow
        }

        val bluetoothAdapter = try {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            manager?.adapter
        } catch (e: SecurityException) {
            null
        }

        if (bluetoothAdapter == null) {
            trySend(SensorStatus.Unavailable)
            awaitClose { }
            return@callbackFlow
        }

        fun emitState() {
            trySend(
                if (bluetoothAdapter.isEnabled) SensorStatus.Active
                else SensorStatus.Blocked
            )
        }

        emitState()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                emitState()
            }
        }
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                receiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(
                receiver,
                filter
            )
        }

        awaitClose { context.unregisterReceiver(receiver) }
    }

    /**
     * Checks whether an AppOp is blocked by the system privacy toggle.
     *
     * Uses [AppOpsManager.unsafeCheckOpNoThrow] on API 29+ and the deprecated
     * [AppOpsManager.checkOpNoThrow] on API 19-28. Returns `false` below API 28
     * where AppOps is not available.
     */
    @Suppress("DEPRECATION")
    private fun isAppOpBlocked(opStr: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(opStr, Process.myUid(), context.packageName)
            } else {
                appOps.checkOpNoThrow(opStr, Process.myUid(), context.packageName)
            }
            isOpBlocked(result)
        } catch (_: SecurityException) {
            false
        }
    }

    /**
     * Snapshot of microphone access.
     *
     * Checks [android.Manifest.permission.RECORD_AUDIO] + AppOps privacy toggle.
     * Does **not** register [android.media.AudioRecordingCallback] — the status
     * reflects *access*, not active recording by another app. Emits once and
     * never re-emits until re-subscribed via [refreshTrigger].
     *
     * **Known limitation on Android 16:** [AppOpsManager.checkOpNoThrow] returns
     * [AppOpsManager.MODE_IGNORED] for background processes regardless of the
     * actual toggle state. Refresh requires app visibility.
     */
    private fun observeMicStatus(): Flow<SensorStatus> = callbackFlow {
        fun emitState() {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                trySend(SensorStatus.Denied)
                return
            }
            if (isAppOpBlocked(AppOpsManager.OPSTR_RECORD_AUDIO)) {
                trySend(SensorStatus.Blocked)
                return
            }
            trySend(SensorStatus.Active)
        }

        emitState()
        awaitClose { }
    }

    /**
     * Snapshot of camera access.
     *
     * Checks [android.Manifest.permission.CAMERA] + AppOps privacy toggle.
     * Does **not** register [android.hardware.camera2.CameraManager.AvailabilityCallback] —
     * the status reflects *access*, not camera-in-use by another app. Emits once and
     * never re-emits until re-subscribed via [refreshTrigger].
     *
     * **Known limitation on Android 16:** Same background detection restriction
     * as [observeMicStatus].
     */
    private fun observeCameraStatus(): Flow<SensorStatus> = callbackFlow {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (cameraManager == null) {
            trySend(SensorStatus.Unavailable)
            close()
            return@callbackFlow
        }

        fun emitState() {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                trySend(SensorStatus.Denied)
                return
            }
            if (isAppOpBlocked(AppOpsManager.OPSTR_CAMERA)) {
                trySend(SensorStatus.Blocked)
                return
            }
            try {
                val ids = cameraManager.cameraIdList
                if (ids.isEmpty()) {
                    trySend(SensorStatus.Unavailable)
                    return
                }
                trySend(SensorStatus.Active)
            } catch (_: SecurityException) {
                trySend(SensorStatus.Blocked)
            } catch (_: CameraAccessException) {
                trySend(SensorStatus.Unavailable)
            }
        }

        emitState()
        awaitClose { }
    }
}

/** Returns `true` when [opResult] indicates an AppOps block. */
fun isOpBlocked(opResult: Int): Boolean =
    opResult == AppOpsManager.MODE_IGNORED
