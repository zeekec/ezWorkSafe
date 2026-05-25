// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import com.ezworksafe.ui.view.MainActivity

/**
 * Compact Glance widget (2x2 grid of colored dots).
 *
 * Shows all four sensors as colored dots in a 2x2 layout with short labels.
 * Real-time updates pushed from [MonitoringService.pushWidgetUpdate].
 */
class CompactWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val statuses = WidgetState.statuses
        provideContent {
            CompactContent(statuses = statuses)
        }
    }
}

@Composable
private fun CompactContent(statuses: Map<SensorType, SensorStatus>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(4.dp)
            .background(ColorProvider(widgetBgDarkColor))
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            CompactCell(
                name = SensorType.WIFI.shortName,
                status = statuses[SensorType.WIFI] ?: SensorStatus.Unavailable,
                modifier = GlanceModifier.defaultWeight()
            )
            CompactCell(
                name = SensorType.BLUETOOTH.shortName,
                status = statuses[SensorType.BLUETOOTH] ?: SensorStatus.Unavailable,
                modifier = GlanceModifier.defaultWeight()
            )
        }
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            CompactCell(
                name = SensorType.MICROPHONE.shortName,
                status = statuses[SensorType.MICROPHONE] ?: SensorStatus.Unavailable,
                modifier = GlanceModifier.defaultWeight()
            )
            CompactCell(
                name = SensorType.CAMERA.shortName,
                status = statuses[SensorType.CAMERA] ?: SensorStatus.Unavailable,
                modifier = GlanceModifier.defaultWeight()
            )
        }
    }
}

@Composable
private fun CompactCell(name: String, status: SensorStatus, modifier: GlanceModifier) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = GlanceModifier
                .padding(bottom = 2.dp)
                .size(8.dp)
                .background(ColorProvider(ComposeColor(status.color)))
        ) { }
        Text(
            text = name,
            style = TextStyle(
                color = ColorProvider(widgetLabelTextColor),
                fontSize = 9.sp
            )
        )
    }
}
