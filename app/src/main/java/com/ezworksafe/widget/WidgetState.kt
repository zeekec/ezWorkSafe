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
 * [lastRefreshTime] is set by [com.ezworksafe.data.repository.SensorRepository.refresh]
 * but **not** by [MonitoringService.pushWidgetUpdate] — the timestamp may lag behind
 * the widget display.
 */
object WidgetState {
    @Volatile
    var statuses: Map<SensorType, SensorStatus> = SensorType.entries.associateWith {
        SensorStatus.Inactive
    }
    @Volatile
    var lastRefreshTime: Long = 0L
}
