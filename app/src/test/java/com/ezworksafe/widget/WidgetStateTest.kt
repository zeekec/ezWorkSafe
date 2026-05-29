// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.widget

import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WidgetStateTest {

    @Before
    fun setUp() {
        WidgetState.lastRefreshTime = 0L
        WidgetState.statuses = SensorType.entries.associateWith {
            SensorStatus.Unavailable
        }
    }

    @Test
    fun `default statuses are Unavailable for all four sensors`() {
        val statuses = WidgetState.statuses
        assertEquals(4, statuses.size)
        SensorType.entries.forEach { type ->
            assertEquals(SensorStatus.Unavailable, statuses[type])
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

    @Test
    fun `lastRefreshTime starts at zero`() {
        assertEquals(0L, WidgetState.lastRefreshTime)
    }

    @Test
    fun `lastRefreshTime can be updated`() {
        val time = System.currentTimeMillis()
        WidgetState.lastRefreshTime = time
        assertEquals(time, WidgetState.lastRefreshTime)
    }
}
