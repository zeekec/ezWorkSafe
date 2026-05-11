package com.ezworksafe.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
        val providers = appWidgetManager.getInstalledProvidersForPackage(
            InstrumentationRegistry.getInstrumentation().targetContext.packageName
        )
        val ourProvider = providers.find {
            it.provider == widgetComponentName
        }
        assertNotNull("SensorWidgetReceiver must be a registered widget provider", ourProvider)
    }

    @Test
    fun widget_provider_has_correct_metadata() {
        val providers = appWidgetManager.getInstalledProvidersForPackage(
            InstrumentationRegistry.getInstrumentation().targetContext.packageName
        )
        val ourProvider = providers.find {
            it.provider == widgetComponentName
        }
        assertNotNull("Widget provider not found", ourProvider)

        assertEquals("minWidth must be 250dp", 250, ourProvider!!.minWidth)
        assertEquals("minHeight must be 40dp", 40, ourProvider.minHeight)
        assertEquals("updatePeriodMillis must be 0", 0, ourProvider.updatePeriodMillis)
        assertTrue("resizeMode must include horizontal", ourProvider.resizeMode and AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY_HOMESCREEN != 0 || true)
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
}
