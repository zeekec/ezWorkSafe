// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.ui.viewmodel

import com.ezworksafe.EzWorkSafeApp
import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import com.ezworksafe.data.repository.SensorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class SensorViewModelTest {

    private val mockRepo = mock<SensorRepository> {
        on { observeSensor(SensorType.WIFI) } doReturn flowOf(SensorStatus.Unavailable)
        on { observeSensor(SensorType.BLUETOOTH) } doReturn flowOf(SensorStatus.Unavailable)
        on { observeSensor(SensorType.MICROPHONE) } doReturn flowOf(SensorStatus.Unavailable)
        on { observeSensor(SensorType.CAMERA) } doReturn flowOf(SensorStatus.Unavailable)
    }

    private val mockApp = mock<EzWorkSafeApp> {
        on { sensorRepository } doReturn mockRepo
    }

    @Test
    fun `viewModel exposes StateFlows for all four sensors`() {
        val vm = SensorViewModel(mockApp)

        assertNotNull(vm.wifiStatus)
        assertNotNull(vm.bluetoothStatus)
        assertNotNull(vm.micStatus)
        assertNotNull(vm.cameraStatus)
    }

    @Test
    fun `viewModel exposes sensor types list`() {
        val vm = SensorViewModel(mockApp)
        assertEquals(4, vm.sensorTypes.size)
    }

    @Test
    fun `initial wifi status is Unavailable with mock context`() = runTest {
        val vm = SensorViewModel(mockApp)
        assertEquals(SensorStatus.Unavailable, vm.wifiStatus.value)
    }

    @Test
    fun `sensorTypes contains all four sensor types`() {
        val vm = SensorViewModel(mockApp)
        assertEquals(
            listOf(SensorType.WIFI, SensorType.BLUETOOTH, SensorType.MICROPHONE, SensorType.CAMERA),
            vm.sensorTypes
        )
    }

    @Test
    fun `refresh delegates to repository`() {
        val vm = SensorViewModel(mockApp)
        vm.refresh()
        verify(mockRepo).refresh()
    }
}
