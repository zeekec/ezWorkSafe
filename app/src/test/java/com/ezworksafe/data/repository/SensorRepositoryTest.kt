// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.data.repository

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import com.ezworksafe.widget.WidgetState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class SensorRepositoryTest {

    private val mockContext: Context = mock()

    @Before
    fun setUp() {
        WidgetState.lastRefreshTime = 0L
    }

    @Test
    fun `isOpBlocked returns false for pre-P SDK`() {
        assertFalse(isOpBlocked(27, AppOpsManager.MODE_IGNORED))
    }

    @Test
    fun `isOpBlocked returns true for MODE_IGNORED on P+`() {
        assertTrue(isOpBlocked(Build.VERSION_CODES.P, AppOpsManager.MODE_IGNORED))
    }

    @Test
    fun `isOpBlocked returns true for MODE_IGNORED on Q+`() {
        assertTrue(isOpBlocked(Build.VERSION_CODES.Q, AppOpsManager.MODE_IGNORED))
    }

    @Test
    fun `isOpBlocked returns false for MODE_ALLOWED`() {
        assertFalse(isOpBlocked(Build.VERSION_CODES.P, AppOpsManager.MODE_ALLOWED))
    }

    @Test
    fun `isOpBlocked returns false for MODE_ERRORED`() {
        assertFalse(isOpBlocked(Build.VERSION_CODES.P, AppOpsManager.MODE_ERRORED))
    }

    @Test
    fun `repository exposes four sensor flows`() = runTest {
        val repo = SystemSensorRepository(mockContext)

        val wifiStatus = repo.observeSensor(SensorType.WIFI).first()
        val btStatus = repo.observeSensor(SensorType.BLUETOOTH).first()
        val micStatus = repo.observeSensor(SensorType.MICROPHONE).first()
        val camStatus = repo.observeSensor(SensorType.CAMERA).first()

        assertEquals(SensorStatus.Unavailable, wifiStatus)
        assertEquals(SensorStatus.Unavailable, btStatus)
        assertEquals(SensorStatus.Unavailable, micStatus)
        assertEquals(SensorStatus.Unavailable, camStatus)
    }

    @Test
    fun `refresh updates WidgetState lastRefreshTime`() {
        WidgetState.lastRefreshTime = 0L
        val repo = SystemSensorRepository(mockContext)
        repo.refresh()
        assertTrue(WidgetState.lastRefreshTime > 0)
    }

    @Test
    fun `fake repository emits configured statuses`() = runTest {
        val fake = FakeSensorRepository()
        fake.setStatus(SensorType.WIFI, SensorStatus.Active)
        fake.setStatus(SensorType.MICROPHONE, SensorStatus.Denied)

        assertEquals(SensorStatus.Active, fake.observeSensor(SensorType.WIFI).first())
        assertEquals(SensorStatus.Inactive, fake.observeSensor(SensorType.BLUETOOTH).first())
        assertEquals(SensorStatus.Denied, fake.observeSensor(SensorType.MICROPHONE).first())
        assertEquals(SensorStatus.Inactive, fake.observeSensor(SensorType.CAMERA).first())
    }
}
