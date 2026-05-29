// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.data.repository

import android.app.AppOpsManager
import android.content.Context
import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class SensorRepositoryTest {

    private val mockContext: Context = mock()

    @Test
    fun `isOpBlocked returns true for MODE_IGNORED`() {
        assertTrue(isOpBlocked(AppOpsManager.MODE_IGNORED))
    }

    @Test
    fun `isOpBlocked returns false for MODE_ALLOWED`() {
        assertFalse(isOpBlocked(AppOpsManager.MODE_ALLOWED))
    }

    @Test
    fun `isOpBlocked returns false for MODE_ERRORED`() {
        assertFalse(isOpBlocked(AppOpsManager.MODE_ERRORED))
    }

    @Test
    fun `repository exposes four sensor flows`() = runTest {
        val repo = SystemSensorRepository(mockContext)

        val wifiStatus = repo.observeSensor(SensorType.WIFI).first()
        val btStatus = repo.observeSensor(SensorType.BLUETOOTH).first()
        val camStatus = repo.observeSensor(SensorType.CAMERA).first()

        assertEquals(SensorStatus.Unavailable, wifiStatus)
        assertEquals(SensorStatus.Unavailable, btStatus)
        assertEquals(SensorStatus.Unavailable, camStatus)
    }

    @Test
    fun `fake repository emits configured statuses`() = runTest {
        val fake = FakeSensorRepository()
        fake.setStatus(SensorType.WIFI, SensorStatus.Active)
        fake.setStatus(SensorType.MICROPHONE, SensorStatus.Denied)

        assertEquals(SensorStatus.Active, fake.observeSensor(SensorType.WIFI).first())
        assertEquals(SensorStatus.Unavailable, fake.observeSensor(SensorType.BLUETOOTH).first())
        assertEquals(SensorStatus.Denied, fake.observeSensor(SensorType.MICROPHONE).first())
        assertEquals(SensorStatus.Unavailable, fake.observeSensor(SensorType.CAMERA).first())
    }
}
