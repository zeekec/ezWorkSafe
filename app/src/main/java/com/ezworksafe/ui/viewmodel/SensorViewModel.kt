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

class SensorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as EzWorkSafeApp).sensorRepository

    val sensorTypes: List<SensorType> = SensorType.entries

    val wifiStatus: StateFlow<SensorStatus> = repository
        .observeSensor(SensorType.WIFI)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SensorStatus.Unavailable)

    val bluetoothStatus: StateFlow<SensorStatus> = repository
        .observeSensor(SensorType.BLUETOOTH)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SensorStatus.Unavailable)

    val micStatus: StateFlow<SensorStatus> = repository
        .observeSensor(SensorType.MICROPHONE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SensorStatus.Unavailable)

    val cameraStatus: StateFlow<SensorStatus> = repository
        .observeSensor(SensorType.CAMERA)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SensorStatus.Unavailable)
}
