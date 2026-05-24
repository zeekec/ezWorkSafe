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
import android.media.AudioManager
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

class SystemSensorRepository(private val context: Context) : SensorRepository {

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
            isOpBlocked(Build.VERSION.SDK_INT, result)
        } catch (_: SecurityException) {
            false
        }
    }

    private fun observeMicStatus(): Flow<SensorStatus> = callbackFlow {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager == null) {
            trySend(SensorStatus.Unavailable)
            close()
            return@callbackFlow
        }

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

fun isOpBlocked(sdk: Int, opResult: Int): Boolean =
    sdk >= Build.VERSION_CODES.P && opResult == AppOpsManager.MODE_IGNORED
