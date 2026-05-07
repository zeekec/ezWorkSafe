package com.ezworksafe.data.repository

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SensorRepository(private val context: Context) {

    fun observeSensor(type: SensorType): Flow<SensorStatus> {
        return when (type) {
            SensorType.WIFI -> observeWifiStatus()
            SensorType.BLUETOOTH -> observeBluetoothStatus()
            SensorType.MICROPHONE -> observeMicStatus()
            SensorType.CAMERA -> observeCameraStatus()
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
            trySend(if (wifiManager.isWifiEnabled) SensorStatus.Active else SensorStatus.Inactive)
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
                else SensorStatus.Inactive
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
            val configs: List<AudioRecordingConfiguration> = audioManager.activeRecordingConfigurations
            val micActive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                configs.any { it.clientAudioSource == android.media.MediaRecorder.AudioSource.MIC }
            } else {
                configs.isNotEmpty()
            }
            trySend(if (micActive) SensorStatus.Active else SensorStatus.Inactive)
        }

        emitState()

        val callback = object : AudioManager.AudioRecordingCallback() {
            override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>) {
                emitState()
            }
        }
        audioManager.registerAudioRecordingCallback(callback, null)

        awaitClose { audioManager.unregisterAudioRecordingCallback(callback) }
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
            try {
                cameraManager.cameraIdList
                trySend(SensorStatus.Inactive)
            } catch (e: Exception) {
                trySend(SensorStatus.Unavailable)
            }
        }

        emitState()

        val callback = object : CameraManager.AvailabilityCallback() {
            override fun onCameraAvailable(cameraId: String) {
                trySend(SensorStatus.Inactive)
            }

            override fun onCameraUnavailable(cameraId: String) {
                trySend(SensorStatus.Active)
            }
        }
        cameraManager.registerAvailabilityCallback(callback, null)

        awaitClose { cameraManager.unregisterAvailabilityCallback(callback) }
    }
}
