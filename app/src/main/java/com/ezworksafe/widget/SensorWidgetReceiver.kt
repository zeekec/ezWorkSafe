// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.ezworksafe.R
import com.ezworksafe.service.MonitoringService
import com.ezworksafe.ui.view.MainActivity

/**
 * Glance widget receiver for the full-size sensor widget.
 *
 * **Initial render path:** [onUpdate] starts [MonitoringService] (which handles
 * real-time updates) then falls through to Glance for the initial layout. The
 * XML-based click handler is set here via [RemoteViews] as a fallback in case
 * Glance's Compose clickable doesn't apply immediately.
 */
class SensorWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SensorWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        try {
            val intent = Intent(context, MonitoringService::class.java)
            context.startForegroundService(intent)
        } catch (e: IllegalStateException) {
            Log.w("SensorWidgetReceiver", "startForegroundService blocked by FGS restrictions", e)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        for (appWidgetId in appWidgetIds) {
            val pendingIntent = PendingIntent.getActivity(
                context, appWidgetId, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val views = RemoteViews(context.packageName, R.layout.widget_initial_layout)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
