package com.ezworksafe.data.repository

import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSensorRepository : SensorRepository {

    private val statuses = SensorType.entries.associateWith {
        MutableStateFlow<SensorStatus>(SensorStatus.Inactive)
    }.toMutableMap()

    override fun observeSensor(type: SensorType): Flow<SensorStatus> {
        return statuses.getValue(type)
    }

    override fun refresh() = Unit

    fun setStatus(type: SensorType, status: SensorStatus) {
        statuses.getValue(type).value = status
    }
}
