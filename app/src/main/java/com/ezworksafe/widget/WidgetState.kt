// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.widget

import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType

object WidgetState {
    @Volatile
    var statuses: Map<SensorType, SensorStatus> = SensorType.entries.associateWith {
        SensorStatus.Inactive
    }
    @Volatile
    var lastRefreshTime: Long = 0L
}
