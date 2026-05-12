package com.ezworksafe.ui.view

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.ezworksafe.EzWorkSafeApp
import com.ezworksafe.data.model.SensorStatus
import com.ezworksafe.data.model.SensorType
import com.ezworksafe.data.repository.FakeSensorRepository
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class StatusDashboardE2eTest {

    companion object {
        private lateinit var fakeRepo: FakeSensorRepository

        @BeforeClass @JvmStatic
        fun setUpClass() {
            fakeRepo = FakeSensorRepository()
            val app = InstrumentationRegistry.getInstrumentation()
                .targetContext.applicationContext as EzWorkSafeApp
            app.sensorRepository = fakeRepo
        }
    }

    private val permissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(permissionRule)
        .around(composeRule)

    @Before
    fun setUp() {
        SensorType.entries.forEach { fakeRepo.setStatus(it, SensorStatus.Inactive) }
    }

    @Test
    fun wifi_card_shows_initial_status() {
        composeRule.onNodeWithText("WiFi").assertIsDisplayed()
    }

    @Test
    fun wifi_toggles_between_active_and_blocked() {
        fakeRepo.setStatus(SensorType.WIFI, SensorStatus.Active)
        composeRule.waitUntil(5_000) {
            try {
                composeRule.onNodeWithText("Active").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }

        fakeRepo.setStatus(SensorType.WIFI, SensorStatus.Blocked)
        composeRule.waitUntil(5_000) {
            try {
                composeRule.onNode(hasText("Blocked") and hasAnySibling(hasText("WiFi"))).assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    @Test
    fun bluetooth_active_updates_ui() {
        fakeRepo.setStatus(SensorType.BLUETOOTH, SensorStatus.Active)
        composeRule.waitUntil(5000) {
            try {
                composeRule.onNodeWithText("Active").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    @Test
    fun all_four_sensor_labels_displayed() {
        composeRule.onNodeWithText("WiFi").assertIsDisplayed()
        composeRule.onNodeWithText("Bluetooth").assertIsDisplayed()
        composeRule.onNodeWithText("Microphone").assertIsDisplayed()
        composeRule.onNodeWithText("Camera").assertIsDisplayed()
    }

    @Test
    fun sensor_shows_denied_status() {
        fakeRepo.setStatus(SensorType.CAMERA, SensorStatus.Denied)
        composeRule.waitUntil(5000) {
            try {
                composeRule.onNodeWithText("Denied").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    @Test
    fun bluetooth_blocked_updates_ui() {
        fakeRepo.setStatus(SensorType.BLUETOOTH, SensorStatus.Blocked)
        composeRule.waitUntil(5000) {
            try {
                composeRule.onNode(hasText("Blocked") and hasAnySibling(hasText("Bluetooth"))).assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    @Test
    fun microphone_shows_active() {
        fakeRepo.setStatus(SensorType.MICROPHONE, SensorStatus.Active)
        composeRule.waitUntil(5000) {
            try {
                composeRule.onNode(hasText("Active") and hasAnySibling(hasText("Microphone"))).assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    @Test
    fun microphone_shows_blocked() {
        fakeRepo.setStatus(SensorType.MICROPHONE, SensorStatus.Blocked)
        composeRule.waitUntil(5000) {
            try {
                composeRule.onNode(hasText("Blocked") and hasAnySibling(hasText("Microphone"))).assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    @Test
    fun microphone_shows_denied() {
        fakeRepo.setStatus(SensorType.MICROPHONE, SensorStatus.Denied)
        composeRule.waitUntil(5000) {
            try {
                composeRule.onNode(hasText("Denied") and hasAnySibling(hasText("Microphone"))).assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    @Test
    fun camera_shows_active() {
        fakeRepo.setStatus(SensorType.CAMERA, SensorStatus.Active)
        composeRule.waitUntil(5000) {
            try {
                composeRule.onNode(hasText("Active") and hasAnySibling(hasText("Camera"))).assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    @Test
    fun camera_shows_unavailable() {
        fakeRepo.setStatus(SensorType.CAMERA, SensorStatus.Unavailable)
        composeRule.waitUntil(5000) {
            try {
                composeRule.onNode(hasText("Unavailable") and hasAnySibling(hasText("Camera"))).assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }
}
