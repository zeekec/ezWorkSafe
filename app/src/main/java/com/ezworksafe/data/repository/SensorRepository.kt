package com.ezworksafe.data.repository

import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import kotlinx.coroutines.flow.Flow

interface SensorRepository {
    fun observeSensor(type: SensorType): Flow<SensorStatus>
    fun refresh()
}
