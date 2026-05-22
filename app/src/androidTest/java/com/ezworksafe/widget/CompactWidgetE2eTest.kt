// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.view.LayoutInflater
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ezworksafe.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompactWidgetE2eTest {

    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var widgetComponentName: ComponentName

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        appWidgetManager = AppWidgetManager.getInstance(context)
        widgetComponentName = ComponentName(context, CompactWidgetReceiver::class.java)
    }

    @Test
    fun widget_provider_is_registered() {
        val providers = appWidgetManager.getInstalledProviders()
        val ourProvider = providers.find {
            it.provider.className == widgetComponentName.className
        }
        assertNotNull("CompactWidgetReceiver must be a registered widget provider", ourProvider)
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
        val expectedMinWidth = (72f * density).toInt()
        val expectedMinHeight = (72f * density).toInt()

        assertEquals("minWidth must be 72dp ($expectedMinWidth px)",
            expectedMinWidth, ourProvider!!.minWidth)
        assertEquals("minHeight must be 72dp ($expectedMinHeight px)",
            expectedMinHeight, ourProvider.minHeight)
        assertEquals("updatePeriodMillis must be 0", 0, ourProvider.updatePeriodMillis)
    }

    @Test
    fun widget_compact_initial_layout_has_root_id() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.widget_compact_initial, null, false)
        val root = view.findViewById<android.view.View>(R.id.widget_root)
        assertNotNull("widget_compact_initial must have a view with R.id.widget_root", root)
    }
}
