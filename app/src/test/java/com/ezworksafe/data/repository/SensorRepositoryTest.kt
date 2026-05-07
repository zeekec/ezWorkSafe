package com.ezworksafe.data.repository

import android.content.Context
import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock

class SensorRepositoryTest {

    private val mockContext: Context = mock()

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
}
