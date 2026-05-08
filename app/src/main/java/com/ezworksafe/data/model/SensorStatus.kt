package com.ezworksafe.data.model

sealed class SensorStatus(
    val label: String,
    val color: Long
) {
    data object Active : SensorStatus("Active", 0xFF4CAF50L)
    data object Inactive : SensorStatus("Inactive", 0xFF9E9E9EL)
    data object Denied : SensorStatus("Denied", 0xFFF44336L)
    data object Blocked : SensorStatus("Blocked", 0xFFFF9800L)
    data object Unavailable : SensorStatus("Unavailable", 0xFF616161L)
}

enum class SensorType(val displayName: String) {
    WIFI("WiFi"),
    BLUETOOTH("Bluetooth"),
    MICROPHONE("Microphone"),
    CAMERA("Camera")
}
