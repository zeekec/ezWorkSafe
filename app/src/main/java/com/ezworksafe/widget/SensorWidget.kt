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
import androidx.glance.layout.height
import androidx.glance.layout.width
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
            .background(ColorProvider(ComposeColor(0xFF1a1a2e.toInt())))
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = GlanceModifier
                    .defaultWeight()
                    .background(ColorProvider(ComposeColor(0xFF1a1a2e.toInt()))),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val leftEntries = statuses.entries.toList().take(2)
                leftEntries.forEachIndexed { index, (type, status) ->
                    SensorCell(
                        name = when (type) {
                            SensorType.WIFI -> "WiFi"
                            SensorType.BLUETOOTH -> "BT"
                            else -> ""
                        },
                        status = status,
                        modifier = GlanceModifier.defaultWeight()
                    )
                    if (index < leftEntries.lastIndex) {
                        Box(
                            modifier = GlanceModifier
                                .size(1.dp, 30.dp)
                                .background(ColorProvider(ComposeColor(0xFF333355.toInt())))
                        ) { }
                    }
                }
            }

            Box(
                modifier = GlanceModifier
                    .width(2.dp)
                    .height(40.dp)
                    .background(ColorProvider(ComposeColor(0xFF5555AA.toInt())))
            ) { }

            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .background(ColorProvider(ComposeColor(0xFF1e1e35.toInt()))),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val rightEntries = statuses.entries.toList().drop(2)
                    rightEntries.forEachIndexed { index, (type, status) ->
                        SensorCell(
                            name = when (type) {
                                SensorType.MICROPHONE -> "Mic"
                                SensorType.CAMERA -> "Cam"
                                else -> ""
                            },
                            status = status,
                            modifier = GlanceModifier.defaultWeight()
                        )
                        if (index < rightEntries.lastIndex) {
                            Box(
                                modifier = GlanceModifier
                                    .size(1.dp, 30.dp)
                                    .background(ColorProvider(ComposeColor(0xFF333355.toInt())))
                            ) { }
                        }
                    }
                }
                Text(
                    text = formatLastUpdated(WidgetState.lastRefreshTime),
                    style = TextStyle(
                        color = ColorProvider(ComposeColor(0xFFAAAAAA.toInt())),
                        fontSize = 8.sp
                    ),
                    modifier = GlanceModifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun formatLastUpdated(time: Long): String {
    if (time == 0L) return ""
    val elapsed = System.currentTimeMillis() - time
    return when {
        elapsed < 60_000 -> "Updated just now"
        elapsed < 3_600_000 -> "Updated ${elapsed / 60_000}m ago"
        elapsed < 86_400_000 -> "Updated ${elapsed / 3_600_000}h ago"
        else -> "Updated >1d ago"
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
                .background(ColorProvider(ComposeColor(status.color.toInt())))
        ) {
        }
        Text(
            text = name,
            style = TextStyle(
                color = ColorProvider(ComposeColor(0xFFAAAAAA.toInt())),
                fontSize = 9.sp
            )
        )
        Text(
            text = status.label,
            style = TextStyle(
                color = ColorProvider(ComposeColor(status.color.toInt())),
                fontSize = 10.sp
            )
        )
    }
}
