// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.widget

import android.app.PendingIntent
import android.widget.RemoteViews
import com.ezworksafe.R
import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import com.ezworksafe.util.formatLastUpdated
import java.text.DateFormat

internal fun buildWidgetRemoteViews(
    packageName: String,
    wifi: SensorStatus, bt: SensorStatus,
    mic: SensorStatus, cam: SensorStatus,
    lastRefreshTime: Long,
    timeFormat: DateFormat,
    openAppIntent: PendingIntent? = null
): RemoteViews {
    val labelColor = 0xFFAAAAAA.toInt()
    val statuses = mapOf(SensorType.WIFI to wifi, SensorType.BLUETOOTH to bt,
        SensorType.MICROPHONE to mic, SensorType.CAMERA to cam)
    val cellMap = mapOf(
        SensorType.WIFI to Pair(R.id.dot_wifi, R.id.status_wifi),
        SensorType.BLUETOOTH to Pair(R.id.dot_bt, R.id.status_bt),
        SensorType.MICROPHONE to Pair(R.id.dot_mic, R.id.status_mic),
        SensorType.CAMERA to Pair(R.id.dot_cam, R.id.status_cam),
    )

    val views = RemoteViews(packageName, R.layout.widget_sensor_status)
    views.setInt(R.id.widget_root, "setBackgroundColor", 0xFF1a1a2e.toInt())
    views.setInt(R.id.left_section, "setBackgroundColor", 0xFF1a1a2e.toInt())
    views.setInt(R.id.right_section, "setBackgroundColor", 0xFF1e1e35.toInt())
    for ((type, status) in statuses) {
        val (dotId, statusId) = cellMap[type] ?: continue
        views.setInt(dotId, "setBackgroundColor", status.color)
        views.setTextColor(statusId, status.color)
        views.setTextViewText(statusId, status.label)
    }
    views.setTextColor(R.id.label_wifi, labelColor)
    views.setTextColor(R.id.label_bt, labelColor)
    views.setTextColor(R.id.label_mic, labelColor)
    views.setTextColor(R.id.label_cam, labelColor)
    views.setTextViewText(R.id.last_updated, formatLastUpdated(lastRefreshTime, timeFormat))
    views.setTextColor(R.id.last_updated, labelColor)
    if (openAppIntent != null) {
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent)
    }
    return views
}
