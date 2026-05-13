// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.widget

import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WidgetStateLabelTest {

    @Before
    fun setUp() {
        WidgetState.lastRefreshTime = 0L
        WidgetState.statuses = SensorType.entries.associateWith {
            SensorStatus.Inactive
        }
    }

    @Test
    fun `widget state is updated with all four sensor statuses`() {
        val statuses = mapOf(
            SensorType.WIFI to SensorStatus.Active,
            SensorType.BLUETOOTH to SensorStatus.Blocked,
            SensorType.MICROPHONE to SensorStatus.Active,
            SensorType.CAMERA to SensorStatus.Denied
        )
        WidgetState.statuses = statuses

        val wifiLabel = WidgetState.statuses[SensorType.WIFI]?.label
        val btLabel = WidgetState.statuses[SensorType.BLUETOOTH]?.label
        val micLabel = WidgetState.statuses[SensorType.MICROPHONE]?.label
        val camLabel = WidgetState.statuses[SensorType.CAMERA]?.label

        assertEquals("Active", wifiLabel)
        assertEquals("Blocked", btLabel)
        assertEquals("Active", micLabel)
        assertEquals("Denied", camLabel)
    }
}
