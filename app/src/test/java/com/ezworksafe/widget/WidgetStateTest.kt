package com.ezworksafe.widget

import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WidgetStateTest {

    @Before
    fun setUp() {
        WidgetState.statuses = SensorType.entries.associateWith {
            SensorStatus.Inactive
        }
    }

    @Test
    fun `default statuses are Inactive for all four sensors`() {
        val statuses = WidgetState.statuses
        assertEquals(4, statuses.size)
        SensorType.entries.forEach { type ->
            assertEquals(SensorStatus.Inactive, statuses[type])
        }
    }

    @Test
    fun `can update and read statuses`() {
        val expected = mapOf(
            SensorType.WIFI to SensorStatus.Active,
            SensorType.BLUETOOTH to SensorStatus.Blocked,
            SensorType.MICROPHONE to SensorStatus.Denied,
            SensorType.CAMERA to SensorStatus.Unavailable
        )
        WidgetState.statuses = expected
        assertEquals(expected, WidgetState.statuses)
    }
}
