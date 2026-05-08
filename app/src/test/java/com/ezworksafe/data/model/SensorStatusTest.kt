package com.ezworksafe.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SensorStatusTest {

    @Test
    fun `active displays green label`() {
        assertEquals("Active", SensorStatus.Active.label)
        assertEquals(0xFF4CAF50L, SensorStatus.Active.color)
    }

    @Test
    fun `inactive displays gray label`() {
        assertEquals("Inactive", SensorStatus.Inactive.label)
        assertEquals(0xFF9E9E9EL, SensorStatus.Inactive.color)
    }

    @Test
    fun `denied displays red label`() {
        assertEquals("Denied", SensorStatus.Denied.label)
        assertEquals(0xFFF44336L, SensorStatus.Denied.color)
    }

    @Test
    fun `blocked displays orange label`() {
        assertEquals("Blocked", SensorStatus.Blocked.label)
        assertEquals(0xFFFF9800L, SensorStatus.Blocked.color)
    }

    @Test
    fun `unavailable displays dark gray label`() {
        assertEquals("Unavailable", SensorStatus.Unavailable.label)
        assertEquals(0xFF616161L, SensorStatus.Unavailable.color)
    }

    @Test
    fun `sensor type enum has four values`() {
        val values = SensorType.values()
        assertEquals(4, values.size)
        assertEquals("WiFi", SensorType.WIFI.displayName)
        assertEquals("Bluetooth", SensorType.BLUETOOTH.displayName)
        assertEquals("Microphone", SensorType.MICROPHONE.displayName)
        assertEquals("Camera", SensorType.CAMERA.displayName)
    }
}
