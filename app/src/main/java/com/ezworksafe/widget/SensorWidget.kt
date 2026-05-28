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
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import com.ezworksafe.ui.view.MainActivity

/**
 * Full-size Glance widget with left/right section layout.
 *
 * Left section: WiFi + BT (real-time updates via broadcasts).
 * Right section: Mic + Cam (state from last foreground refresh).
 *
 * **Note:** Glance provides the initial render. Real-time updates bypass Glance
 * via [MonitoringService.pushWidgetUpdate] using [RemoteViews].
 */
private val wifiBtSensors = setOf(SensorType.WIFI, SensorType.BLUETOOTH)
private val micCamSensors = setOf(SensorType.MICROPHONE, SensorType.CAMERA)

class SensorWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val statuses = WidgetState.statuses
        provideContent {
            WidgetContent(statuses = statuses)
        }
    }
}

@Composable
private fun WidgetContent(statuses: Map<SensorType, SensorStatus>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(8.dp)
            .background(ColorProvider(widgetBgDarkColor))
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Row(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            ) {
                val wifiBtList = statuses.filterKeys { it in wifiBtSensors }.entries.toList()
                wifiBtList.forEachIndexed { index, (type, status) ->
                    SensorCell(
                        name = type.shortName,
                        status = status,
                        modifier = GlanceModifier.defaultWeight()
                    )
                    if (index < wifiBtList.lastIndex) {
                        Box(
                            modifier = GlanceModifier
                                .width(1.dp)
                                .size(1.dp, 30.dp)
                                .background(ColorProvider(widgetBorderColor))
                        ) { }
                    }
                }
            }

            Box(
                modifier = GlanceModifier
                    .width(2.dp)
                    .size(2.dp, 40.dp)
                    .background(ColorProvider(widgetAccentColor))
            ) { }

            Box(
                modifier = GlanceModifier.defaultWeight(),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                ) {
                    val micCamList = statuses.filterKeys { it in micCamSensors }.entries.toList()
                    micCamList.forEachIndexed { index, (type, status) ->
                        SensorCell(
                            name = type.shortName,
                            status = status,
                            modifier = GlanceModifier.defaultWeight()
                        )
                        if (index < micCamList.lastIndex) {
                            Box(
                                modifier = GlanceModifier
                                    .width(1.dp)
                                    .size(1.dp, 30.dp)
                                .background(ColorProvider(widgetBorderColor))
                            ) { }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SensorCell(name: String, status: SensorStatus, modifier: GlanceModifier) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = GlanceModifier
                .padding(bottom = 4.dp)
                .size(10.dp)
                .background(ColorProvider(ComposeColor(status.color)))
        ) {
        }
        Text(
            text = name,
            style = TextStyle(
                color = ColorProvider(widgetLabelTextColor),
                fontSize = 9.sp
            )
        )
        Text(
            text = status.label,
            style = TextStyle(
                color = ColorProvider(ComposeColor(status.color)),
                fontSize = 10.sp
            )
        )
    }
}
