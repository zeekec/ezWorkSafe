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

/**
 * Builds [RemoteViews] for the full-size SensorWidget.
 *
 * This is the real-time update path (bypasses Glance). Layout is defined in
 * `res/layout/widget_sensor_status.xml`. The widget is split into a left section
 * (WiFi, BT) and a right section (Mic, Cam + timestamp), visually separated by
 * a divider.
 */
internal fun buildWidgetRemoteViews(
    packageName: String,
    wifi: SensorStatus, bt: SensorStatus,
    mic: SensorStatus, cam: SensorStatus,
    lastRefreshTime: Long,
    timeFormat: DateFormat,
    openAppIntent: PendingIntent? = null
): RemoteViews {
    val statuses = mapOf(SensorType.WIFI to wifi, SensorType.BLUETOOTH to bt,
        SensorType.MICROPHONE to mic, SensorType.CAMERA to cam)
    val cellMap = mapOf(
        SensorType.WIFI to Pair(R.id.dot_wifi, R.id.status_wifi),
        SensorType.BLUETOOTH to Pair(R.id.dot_bt, R.id.status_bt),
        SensorType.MICROPHONE to Pair(R.id.dot_mic, R.id.status_mic),
        SensorType.CAMERA to Pair(R.id.dot_cam, R.id.status_cam),
    )

    val views = RemoteViews(packageName, R.layout.widget_sensor_status)
    views.setInt(R.id.widget_root, "setBackgroundColor", widgetBgDark)
    views.setInt(R.id.left_section, "setBackgroundColor", widgetBgDark)
    views.setInt(R.id.right_section, "setBackgroundColor", widgetBgRight)
    statuses.forEach { (type, status) ->
        val (dotId, statusId) = cellMap[type] ?: return@forEach
        views.setInt(dotId, "setBackgroundColor", status.color)
        views.setTextColor(statusId, status.color)
        views.setTextViewText(statusId, status.label)
    }
    views.setTextColor(R.id.label_wifi, widgetLabelText)
    views.setTextColor(R.id.label_bt, widgetLabelText)
    views.setTextColor(R.id.label_mic, widgetLabelText)
    views.setTextColor(R.id.label_cam, widgetLabelText)
    views.setTextViewText(R.id.last_updated, formatLastUpdated(lastRefreshTime, timeFormat))
    views.setTextColor(R.id.last_updated, widgetLabelText)
    if (openAppIntent != null) {
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent)
    }
    return views
}
