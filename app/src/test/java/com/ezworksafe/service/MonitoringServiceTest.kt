// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.ezworksafe.EzWorkSafeApp
import com.ezworksafe.widget.buildWidgetRemoteViews
import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.ui.view.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.DateFormat

@RunWith(RobolectricTestRunner::class)
class MonitoringServiceTest {

    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val pkg: String = ctx.packageName

    @Test
    fun `buildWidgetRemoteViews constructs successfully`() {
        val views = buildWidgetRemoteViews(
            packageName = pkg,
            wifi = SensorStatus.Active,
            bt = SensorStatus.Inactive,
            mic = SensorStatus.Denied,
            cam = SensorStatus.Unavailable,
            lastRefreshTime = 0L,
            timeFormat = DateFormat.getTimeInstance()
        )
        assertNotNull(views)
    }

    @Test
    fun `buildWidgetRemoteViews handles all state combinations`() {
        val states = listOf(
            SensorStatus.Active, SensorStatus.Inactive,
            SensorStatus.Denied, SensorStatus.Blocked, SensorStatus.Unavailable
        )
        for (state in states) {
            val views = buildWidgetRemoteViews(
                packageName = pkg,
                wifi = state, bt = state,
                mic = state, cam = state,
                lastRefreshTime = 0L,
                timeFormat = DateFormat.getTimeInstance()
            )
            assertNotNull(views)
        }
    }

    @Test
    fun `buildWidgetRemoteViews with PendingIntent constructs successfully`() {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            ctx, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val views = buildWidgetRemoteViews(
            packageName = pkg,
            wifi = SensorStatus.Active,
            bt = SensorStatus.Inactive,
            mic = SensorStatus.Denied,
            cam = SensorStatus.Unavailable,
            lastRefreshTime = 0L,
            timeFormat = DateFormat.getTimeInstance(),
            openAppIntent = pendingIntent
        )
        assertNotNull(views)
    }

    @Test
    fun `buildWidgetRemoteViews with null PendingIntent still works`() {
        val views = buildWidgetRemoteViews(
            packageName = pkg,
            wifi = SensorStatus.Active,
            bt = SensorStatus.Inactive,
            mic = SensorStatus.Denied,
            cam = SensorStatus.Unavailable,
            lastRefreshTime = 0L,
            timeFormat = DateFormat.getTimeInstance(),
            openAppIntent = null
        )
        assertNotNull(views)
    }

    @Test
    @Config(application = EzWorkSafeApp::class, sdk = [Build.VERSION_CODES.O_MR1])
    fun `service notification is created with correct channel id`() {
        val service = Robolectric.buildService(MonitoringService::class.java).create().get()
        val notification = service.createNotification("Test status")
        assertEquals(MonitoringService.CHANNEL_ID, notification.channelId)
    }

    @Test
    @Config(application = EzWorkSafeApp::class, sdk = [Build.VERSION_CODES.O_MR1])
    fun `service notification is ongoing`() {
        val service = Robolectric.buildService(MonitoringService::class.java).create().get()
        val notification = service.createNotification("Test status")
        val flags = notification.flags and Notification.FLAG_ONGOING_EVENT
        assertTrue(flags != 0)
    }

    @Test
    @Config(application = EzWorkSafeApp::class, sdk = [Build.VERSION_CODES.O_MR1])
    fun `service notification has refresh action`() {
        val service = Robolectric.buildService(MonitoringService::class.java).create().get()
        val notification = service.createNotification("Test status")

        val refreshAction = notification.actions?.find { it.title == "Refresh" }
        assertNotNull("Notification should have a Refresh action", refreshAction)
    }

    @Test
    @Config(application = EzWorkSafeApp::class, sdk = [Build.VERSION_CODES.O_MR1])
    fun `service notification has BigTextStyle with summary text`() {
        val service = Robolectric.buildService(MonitoringService::class.java).create().get()
        val notification = service.createNotification("WiFi: Active | BT: Inactive | Mic: Denied | Cam: Unavailable")

        val bigText = notification.extras?.getString(android.app.Notification.EXTRA_TEXT)
        assertNotNull("Notification should have BigText content", bigText)
    }
}
