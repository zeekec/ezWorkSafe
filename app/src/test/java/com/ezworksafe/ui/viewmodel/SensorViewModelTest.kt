package com.ezworksafe.ui.viewmodel

import android.app.Application
import com.ezworksafe.data.model.SensorType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockito.kotlin.mock

class SensorViewModelTest {

    private val mockApp: Application = mock()

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
        assertEquals(com.ezworksafe.data.model.SensorStatus.Unavailable, vm.wifiStatus.value)
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
