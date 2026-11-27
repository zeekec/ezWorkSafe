// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.widget

import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType

/**
 * Shared mutable state between [com.ezworksafe.service.MonitoringService] (writer)
 * and [SensorWidget] / [CompactWidget] (reader, Glance thread).
 *
 * Fields are [@Volatile][kotlin.jvm.Volatile] for cross-thread visibility.
 * [lastRefreshTime] is updated inside [MonitoringService.observeSensors] on every
 * sensor emission (including real-time WiFi/Bluetooth broadcasts and foreground
 * Mic/Cam refreshes), but **not** by [MonitoringService.pushWidgetUpdate] itself —
 * the timestamp reflects the most recent sensor emission and may lag behind the
 * widget display.
 */
object WidgetState {
    @Volatile
    var statuses: Map<SensorType, SensorStatus> = SensorType.entries.associateWith {
        SensorStatus.Unavailable
    }
    @Volatile
    var lastRefreshTime: Long = 0L
}
