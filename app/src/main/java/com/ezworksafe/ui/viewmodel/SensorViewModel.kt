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

private const val STOP_TIMEOUT_MILLIS = 5_000L

class SensorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as EzWorkSafeApp).sensorRepository

    val sensorTypes: List<SensorType> = SensorType.entries

    val wifiStatus: StateFlow<SensorStatus> = repository
        .observeSensor(SensorType.WIFI)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SensorStatus.Unavailable)

    val bluetoothStatus: StateFlow<SensorStatus> = repository
        .observeSensor(SensorType.BLUETOOTH)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SensorStatus.Unavailable)

    val micStatus: StateFlow<SensorStatus> = repository
        .observeSensor(SensorType.MICROPHONE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SensorStatus.Unavailable)

    val cameraStatus: StateFlow<SensorStatus> = repository
        .observeSensor(SensorType.CAMERA)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SensorStatus.Unavailable)

    fun refresh() = repository.refresh()
}
