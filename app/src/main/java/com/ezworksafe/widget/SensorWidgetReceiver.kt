package com.ezworksafe.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.ezworksafe.service.MonitoringService

class SensorWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SensorWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        try {
            val intent = Intent(context, MonitoringService::class.java)
            context.startForegroundService(intent)
        } catch (e: Exception) {
            Log.w("SensorWidgetReceiver", "startForegroundService blocked by FGS restrictions", e)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }
}
