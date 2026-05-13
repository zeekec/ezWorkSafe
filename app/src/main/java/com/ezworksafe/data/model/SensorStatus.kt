// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.data.model

private val ACTIVE_COLOR = 0xFF4CAF50.toInt()
private val INACTIVE_COLOR = 0xFF9E9E9E.toInt()
private val DENIED_COLOR = 0xFFF44336.toInt()
private val BLOCKED_COLOR = 0xFFFF9800.toInt()
private val UNAVAILABLE_COLOR = 0xFF616161.toInt()

sealed class SensorStatus(
    val label: String,
    val color: Int
) {
    data object Active : SensorStatus("Active", ACTIVE_COLOR)
    data object Inactive : SensorStatus("Inactive", INACTIVE_COLOR)
    data object Denied : SensorStatus("Denied", DENIED_COLOR)
    data object Blocked : SensorStatus("Blocked", BLOCKED_COLOR)
    data object Unavailable : SensorStatus("Unavailable", UNAVAILABLE_COLOR)
}

enum class SensorType(val displayName: String, val shortName: String) {
    WIFI("WiFi", "WiFi"),
    BLUETOOTH("Bluetooth", "BT"),
    MICROPHONE("Microphone", "Mic"),
    CAMERA("Camera", "Cam")
}
