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
import android.media.AudioRecordingConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Process
import androidx.core.content.ContextCompat
import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import com.ezworksafe.widget.WidgetState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest

class SystemSensorRepository(private val context: Context) : SensorRepository {

    private val refreshTrigger = MutableStateFlow(0)

    override fun refresh() {
        WidgetState.lastRefreshTime = System.currentTimeMillis()
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
            close()
            return@callbackFlow
        }

        fun emitState() {
            trySend(if (wifiManager.isWifiEnabled) SensorStatus.Active else SensorStatus.Blocked)
        }

        emitState()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                emitState()
            }
        }
        context.registerReceiver(receiver, IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))

        awaitClose { context.unregisterReceiver(receiver) }
    }

    private fun observeBluetoothStatus(): Flow<SensorStatus> = callbackFlow {
        val bluetoothAdapter = try {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            manager?.adapter
        } catch (e: Exception) {
            null
        }

        if (bluetoothAdapter == null) {
            trySend(SensorStatus.Unavailable)
            close()
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
        context.registerReceiver(receiver, filter)

        awaitClose { context.unregisterReceiver(receiver) }
    }

    private fun isAppOpBlocked(opStr: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(opStr, Process.myUid(), context.packageName)
            } else {
                appOps.noteOpNoThrow(opStr, Process.myUid(), context.packageName)
            }
            result == AppOpsManager.MODE_IGNORED
        } catch (_: Exception) {
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

        val audioCallback = object : AudioManager.AudioRecordingCallback() {
            override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>) {
                emitState()
            }
        }
        audioManager.registerAudioRecordingCallback(audioCallback, null)

        awaitClose {
            audioManager.unregisterAudioRecordingCallback(audioCallback)
        }
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
                cameraManager.getCameraCharacteristics(ids[0])
                trySend(SensorStatus.Active)
            } catch (_: SecurityException) {
                trySend(SensorStatus.Blocked)
            } catch (_: CameraAccessException) {
                trySend(SensorStatus.Unavailable)
            } catch (_: Exception) {
                trySend(SensorStatus.Unavailable)
            }
        }

        emitState()

        val availabilityCallback = object : CameraManager.AvailabilityCallback() {
            override fun onCameraAvailable(cameraId: String) {
                emitState()
            }

            override fun onCameraUnavailable(cameraId: String) {
                emitState()
            }
        }
        cameraManager.registerAvailabilityCallback(availabilityCallback, null)

        awaitClose {
            cameraManager.unregisterAvailabilityCallback(availabilityCallback)
        }
    }
}
