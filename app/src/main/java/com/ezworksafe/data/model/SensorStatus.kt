// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.data.model

private val ACTIVE_COLOR = 0xFF4CAF50.toInt()
private val INACTIVE_COLOR = 0xFF9E9E9E.toInt()
private val DENIED_COLOR = 0xFFF44336.toInt()
private val BLOCKED_COLOR = 0xFFFF9800.toInt()
private val UNAVAILABLE_COLOR = 0xFF616161.toInt()

/**
 * Represents the status of a device sensor.
 *
 * **Semantics:** The app reports whether a sensor *can be accessed*, not whether it is
 * currently in use. [Active] means the runtime permission is granted AND AppOps allows
 * hardware access. For WiFi/BT it means the radio is enabled.
 */
sealed class SensorStatus(
    val label: String,
    val color: Int
) {
    /** Permission granted and AppOps permits access (or radio is on for WiFi/BT). */
    data object Active : SensorStatus("Active", ACTIVE_COLOR)

    /** Placeholder default set in [WidgetState]; never emitted by sensor flows. */
    data object Inactive : SensorStatus("Inactive", INACTIVE_COLOR)

    /** Runtime permission not granted by the user. */
    data object Denied : SensorStatus("Denied", DENIED_COLOR)

    /** Permission granted but system privacy toggle blocks access. */
    data object Blocked : SensorStatus("Blocked", BLOCKED_COLOR)

    /** Sensor hardware or system service not available on this device. */
    data object Unavailable : SensorStatus("Unavailable", UNAVAILABLE_COLOR)
}

/** Identifies which device sensor is being queried. */
enum class SensorType(val displayName: String, val shortName: String) {
    WIFI("WiFi", "WiFi"),
    BLUETOOTH("Bluetooth", "BT"),
    MICROPHONE("Microphone", "Mic"),
    CAMERA("Camera", "Cam")
}
