// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ezworksafe.EzWorkSafeApp
import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Exposes sensor statuses as [StateFlow] for the Compose UI.
 *
 * Uses [SharingStarted.WhileSubscribed] with a 5-second stop timeout so that
 * Mic/Cam snapshot flows (which don't auto-emit) are re-queried when the UI
 * returns to foreground. See [SystemSensorRepository.observeMicStatus].
 */
private const val STOP_TIMEOUT_MILLIS = 5_000L

class SensorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as EzWorkSafeApp).sensorRepository

    val sensorTypes: List<SensorType> = SensorType.entries

    /** Emits [SensorStatus] changes for WiFi. Updates in real-time via broadcasts. */
    val wifiStatus: StateFlow<SensorStatus> = repository
        .observeSensor(SensorType.WIFI)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SensorStatus.Unavailable)

    /** Emits [SensorStatus] changes for Bluetooth. Updates in real-time via broadcasts. */
    val bluetoothStatus: StateFlow<SensorStatus> = repository
        .observeSensor(SensorType.BLUETOOTH)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SensorStatus.Unavailable)

    /** Emits [SensorStatus] for microphone access. Snapshot-only — stale while backgrounded. */
    val micStatus: StateFlow<SensorStatus> = repository
        .observeSensor(SensorType.MICROPHONE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SensorStatus.Unavailable)

    /** Emits [SensorStatus] for camera access. Snapshot-only — stale while backgrounded. */
    val cameraStatus: StateFlow<SensorStatus> = repository
        .observeSensor(SensorType.CAMERA)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SensorStatus.Unavailable)

    /** Triggers a re-query of all sensor statuses. */
    fun refresh() = repository.refresh()
}
