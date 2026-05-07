package com.ezworksafe.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ezworksafe.EzWorkSafeApp
import com.ezworksafe.data.model.SensorType
import com.ezworksafe.data.repository.SensorRepository
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
        startForeground(NOTIFICATION_ID, createNotification("Starting..."))
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
                "WiFi: ${wifi.label} | BT: ${bt.label} | Mic: ${mic.label} | Cam: ${cam.label}"
            }.collect { text ->
                val notification = createNotification(text)
                startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ezWorkSafe")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
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
}
