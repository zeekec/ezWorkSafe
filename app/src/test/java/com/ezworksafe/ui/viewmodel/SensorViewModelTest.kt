// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.ui.viewmodel

import com.ezworksafe.EzWorkSafeApp
import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import com.ezworksafe.data.repository.FakeSensorRepository
import com.ezworksafe.data.repository.SensorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class SensorViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val fakeRepo = FakeSensorRepository()
    private val mockApp = mock<EzWorkSafeApp> {
        on { sensorRepository } doReturn fakeRepo
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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

    @Test
    fun `refresh calls repository refresh`() {
        val repo = mock<SensorRepository>()
        val app = mock<EzWorkSafeApp> {
            on { sensorRepository } doReturn repo
        }
        val vm = SensorViewModel(app)
        vm.refresh()
        verify(repo).refresh()
    }

    @Test
    fun `status flow updates when repository emits`() = runTest(testDispatcher) {
        val vm = SensorViewModel(mockApp)
        val collectJob: Job = launch {
            vm.wifiStatus.collect { }
        }
        testDispatcher.scheduler.advanceUntilIdle()

        fakeRepo.setStatus(SensorType.WIFI, SensorStatus.Active)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SensorStatus.Active, vm.wifiStatus.value)
        collectJob.cancel()
    }
}
