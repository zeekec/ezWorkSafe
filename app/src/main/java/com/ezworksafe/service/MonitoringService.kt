// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.ezworksafe.EzWorkSafeApp
import com.ezworksafe.R
import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import com.ezworksafe.data.repository.SensorRepository
import com.ezworksafe.ui.view.MainActivity
import com.ezworksafe.util.formatLastUpdated
import com.ezworksafe.widget.SensorWidgetReceiver
import com.ezworksafe.widget.WidgetState
import java.text.DateFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MonitoringService : Service() {

    companion object {
        const val CHANNEL_ID = "ezworksafe_monitoring"
        const val NOTIFICATION_ID = 1
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundNotification(createNotification("Starting..."))
        observeSensors()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun observeSensors() {
        val repository: SensorRepository = (application as EzWorkSafeApp).sensorRepository
        serviceScope.launch {
            combine(
                repository.observeSensor(SensorType.WIFI),
                repository.observeSensor(SensorType.BLUETOOTH),
                repository.observeSensor(SensorType.MICROPHONE),
                repository.observeSensor(SensorType.CAMERA)
            ) { wifi, bt, mic, cam ->
                WidgetState.statuses = mapOf(
                    SensorType.WIFI to wifi,
                    SensorType.BLUETOOTH to bt,
                    SensorType.MICROPHONE to mic,
                    SensorType.CAMERA to cam
                )
                WidgetState.lastRefreshTime = System.currentTimeMillis()
                pushWidgetUpdate(wifi, bt, mic, cam)
                "WiFi: ${wifi.label} | BT: ${bt.label} | Mic: ${mic.label} | Cam: ${cam.label}"
            }.collect { text ->
                val notification = createNotification(text)
                startForegroundNotification(notification)
            }
        }
    }

    private fun pushWidgetUpdate(
        wifi: SensorStatus, bt: SensorStatus,
        mic: SensorStatus, cam: SensorStatus
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val componentName = ComponentName(this, SensorWidgetReceiver::class.java)
        val ids = appWidgetManager.getAppWidgetIds(componentName)
        Log.d("MonitoringService", "pushWidgetUpdate: ids=${ids.contentToString()}, wifi=$wifi, bt=$bt, mic=$mic, cam=$cam")
        if (ids.isEmpty()) return

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val views = buildWidgetRemoteViews(
            packageName, wifi, bt, mic, cam,
            WidgetState.lastRefreshTime, android.text.format.DateFormat.getTimeFormat(this),
            openAppIntent = openAppIntent
        )
        for (widgetId in ids) {
            appWidgetManager.updateAppWidget(widgetId, views)
            Log.d("MonitoringService", "pushWidgetUpdate: updated widgetId=$widgetId")
        }
    }

    internal fun createNotification(text: String): Notification {
        val refreshIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val refreshPendingIntent = PendingIntent.getActivity(
            this, 0, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ezWorkSafe")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(0, "Refresh", refreshPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Monitoring",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "ezWorkSafe background sensor monitoring"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun startForegroundNotification(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID, notification)
        }
    }
}

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
