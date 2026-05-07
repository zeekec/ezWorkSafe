package com.ezworksafe.data.model

@androidx.annotation.ColorRes
sealed class SensorStatus(
    val label: String,
    val color: Long
) {
    data object Active : SensorStatus("Active", 0xFF4CAF50)
    data object Inactive : SensorStatus("Inactive", 0xFF9E9E9E)
    data object Denied : SensorStatus("Denied", 0xFFF44336)
    data object Unavailable : SensorStatus("Unavailable", 0xFF616161)
}

enum class SensorType(val displayName: String) {
    WIFI("WiFi"),
    BLUETOOTH("Bluetooth"),
    MICROPHONE("Microphone"),
    CAMERA("Camera")
}
