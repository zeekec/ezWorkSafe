// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.ui.viewmodel

import com.ezworksafe.EzWorkSafeApp
import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import com.ezworksafe.data.repository.FakeSensorRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class SensorViewModelTest {

    private val fakeRepo = FakeSensorRepository()
    private val mockApp = mock<EzWorkSafeApp> {
        on { sensorRepository } doReturn fakeRepo
    }

    @Test
    fun `initial sensor statuses are set to Unavailable`() {
        val vm = SensorViewModel(mockApp)

        assertEquals(SensorStatus.Unavailable, vm.wifiStatus.value)
        assertEquals(SensorStatus.Unavailable, vm.bluetoothStatus.value)
        assertEquals(SensorStatus.Unavailable, vm.micStatus.value)
        assertEquals(SensorStatus.Unavailable, vm.cameraStatus.value)
    }

    @Test
    fun `sensorTypes contains all four sensor types`() {
        val vm = SensorViewModel(mockApp)
        assertEquals(
            listOf(SensorType.WIFI, SensorType.BLUETOOTH, SensorType.MICROPHONE, SensorType.CAMERA),
            vm.sensorTypes
        )
    }
}
