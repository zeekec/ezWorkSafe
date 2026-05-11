package com.ezworksafe.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
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

class SensorWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val statuses = WidgetState.statuses
        provideContent {
            WidgetContent(statuses = statuses)
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun WidgetContent(statuses: Map<SensorType, SensorStatus>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(8.dp)
            .background(ColorProvider(0xFF1a1a2e.toInt()))
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            statuses.entries.forEach { (type, status) ->
                SensorCell(
                    name = when (type) {
                        SensorType.WIFI -> "WiFi"
                        SensorType.BLUETOOTH -> "BT"
                        SensorType.MICROPHONE -> "Mic"
                        SensorType.CAMERA -> "Cam"
                    },
                    status = status,
                    modifier = GlanceModifier.defaultWeight()
                )
            }
        }
    }
}

@SuppressLint("RestrictedApi")
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
                .background(ColorProvider(status.color.toInt()))
        ) {
        }
        Text(
            text = name,
            style = TextStyle(
                color = ColorProvider(0xFFAAAAAA.toInt()),
                fontSize = 9.sp
            )
        )
        Text(
            text = status.label,
            style = TextStyle(
                color = ColorProvider(status.color.toInt()),
                fontSize = 10.sp
            )
        )
    }
}
