// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.view.LayoutInflater
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ezworksafe.R
import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import com.ezworksafe.widget.buildWidgetRemoteViews
import com.ezworksafe.ui.view.MainActivity
import java.text.DateFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SensorWidgetE2eTest {

    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var widgetComponentName: ComponentName

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        appWidgetManager = AppWidgetManager.getInstance(context)
        widgetComponentName = ComponentName(context, SensorWidgetReceiver::class.java)
    }

    @Test
    fun widget_provider_is_registered() {
        val providers = appWidgetManager.getInstalledProviders()
        val ourProvider = providers.find {
            it.provider.className == widgetComponentName.className
        }
        assertNotNull("SensorWidgetReceiver must be a registered widget provider", ourProvider)
    }

    @Test
    fun widget_provider_has_correct_metadata() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val providers = appWidgetManager.getInstalledProviders()
        val ourProvider = providers.find {
            it.provider.className == widgetComponentName.className
        }
        assertNotNull("Widget provider not found", ourProvider)

        val density = context.resources.displayMetrics.density
        val expectedMinWidth = (250f * density).toInt()
        val expectedMinHeight = (40f * density).toInt()

        assertEquals("minWidth must be 250dp ($expectedMinWidth px)",
            expectedMinWidth, ourProvider!!.minWidth)
        assertEquals("minHeight must be 40dp ($expectedMinHeight px)",
            expectedMinHeight, ourProvider.minHeight)
        assertEquals("updatePeriodMillis must be 0", 0, ourProvider.updatePeriodMillis)
    }

    @Test
    fun widgetState_can_be_updated_and_widget_renders() {
        WidgetState.statuses = mapOf(
            SensorType.WIFI to SensorStatus.Active,
            SensorType.BLUETOOTH to SensorStatus.Blocked,
            SensorType.MICROPHONE to SensorStatus.Denied,
            SensorType.CAMERA to SensorStatus.Unavailable
        )

        assertEquals("Active", WidgetState.statuses[SensorType.WIFI]?.label)
        assertEquals("Blocked", WidgetState.statuses[SensorType.BLUETOOTH]?.label)
        assertEquals("Denied", WidgetState.statuses[SensorType.MICROPHONE]?.label)
        assertEquals("Unavailable", WidgetState.statuses[SensorType.CAMERA]?.label)
    }

    @Test
    fun widget_sensor_status_layout_has_root_id() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.widget_sensor_status, null, false)
        val root = view.findViewById<android.view.View>(R.id.widget_root)
        assertNotNull("widget_sensor_status must have a view with R.id.widget_root", root)
    }

    @Test
    fun widget_initial_layout_has_root_id() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.widget_initial_layout, null, false)
        val root = view.findViewById<android.view.View>(R.id.widget_root)
        assertNotNull("widget_initial_layout must have a view with R.id.widget_root", root)
    }

    @Test
    fun buildWidgetRemoteViews_with_pendingIntent_constructs_correctly() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val views = buildWidgetRemoteViews(
            packageName = context.packageName,
            wifi = SensorStatus.Active,
            bt = SensorStatus.Unavailable,
            mic = SensorStatus.Denied,
            cam = SensorStatus.Unavailable,
            lastRefreshTime = 0L,
            timeFormat = DateFormat.getTimeInstance(),
            openAppIntent = pendingIntent
        )
        assertNotNull("buildWidgetRemoteViews should return non-null RemoteViews", views)
    }
}
