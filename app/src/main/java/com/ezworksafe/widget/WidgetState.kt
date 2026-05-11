package com.ezworksafe.widget

import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType

object WidgetState {
    var statuses: Map<SensorType, SensorStatus> = SensorType.entries.associateWith {
        SensorStatus.Inactive
    }
}
