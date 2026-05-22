// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import android.widget.RemoteViews
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class CompactWidgetReceiverTest {

    private val ctx: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `onUpdate calls updateAppWidget with RemoteViews`() {
        val appWidgetManager = mock(AppWidgetManager::class.java)
        val receiver = CompactWidgetReceiver()
        val appWidgetIds = intArrayOf(42)

        receiver.onUpdate(ctx, appWidgetManager, appWidgetIds)

        val captor: KArgumentCaptor<RemoteViews> = argumentCaptor()
        verify(appWidgetManager).updateAppWidget(org.mockito.Mockito.eq(42), captor.capture())
        assertNotNull("RemoteViews should not be null", captor.firstValue)
    }

    @Test
    fun `onUpdate handles multiple widget ids`() {
        val appWidgetManager = mock(AppWidgetManager::class.java)
        val receiver = CompactWidgetReceiver()
        val appWidgetIds = intArrayOf(1, 2, 3)

        receiver.onUpdate(ctx, appWidgetManager, appWidgetIds)

        val captor: KArgumentCaptor<RemoteViews> = argumentCaptor()
        verify(appWidgetManager, times(3)).updateAppWidget(org.mockito.Mockito.anyInt(), captor.capture())
        assertNotNull("RemoteViews should not be null", captor.firstValue)
    }
}
