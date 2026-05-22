// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.ui.view

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
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

    private val permissionRule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        GrantPermissionRule.grant(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        GrantPermissionRule.grant(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(permissionRule)
        .around(composeRule)

    @Before
    fun setUp() {
        SensorType.entries.forEach { fakeRepo.setStatus(it, SensorStatus.Inactive) }
    }

    private fun waitForAssertion(timeoutMs: Long = 5_000, assertion: () -> Unit) {
        var lastError: AssertionError? = null
        composeRule.waitUntil(timeoutMs) {
            try {
                assertion()
                lastError = null
                true
            } catch (e: AssertionError) {
                lastError = e
                false
            }
        }
        lastError?.let { throw it }
    }

    @Test
    fun wifi_card_shows_initial_status() {
        composeRule.onNodeWithText("WiFi").assertIsDisplayed()
    }

    @Test
    fun wifi_toggles_between_active_and_blocked() {
        fakeRepo.setStatus(SensorType.WIFI, SensorStatus.Active)
        waitForAssertion {
            composeRule.onNodeWithText("Active").assertIsDisplayed()
        }

        fakeRepo.setStatus(SensorType.WIFI, SensorStatus.Blocked)
        waitForAssertion {
            composeRule.onNode(hasText("Blocked") and hasAnySibling(hasText("WiFi"))).assertIsDisplayed()
        }
    }

    @Test
    fun bluetooth_active_updates_ui() {
        fakeRepo.setStatus(SensorType.BLUETOOTH, SensorStatus.Active)
        waitForAssertion {
            composeRule.onNodeWithText("Active").assertIsDisplayed()
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
        waitForAssertion {
            composeRule.onNodeWithText("Denied").assertIsDisplayed()
        }
    }

    @Test
    fun bluetooth_blocked_updates_ui() {
        fakeRepo.setStatus(SensorType.BLUETOOTH, SensorStatus.Blocked)
        waitForAssertion {
            composeRule.onNode(hasText("Blocked") and hasAnySibling(hasText("Bluetooth"))).assertIsDisplayed()
        }
    }

    @Test
    fun microphone_shows_active() {
        fakeRepo.setStatus(SensorType.MICROPHONE, SensorStatus.Active)
        waitForAssertion {
            composeRule.onNode(hasText("Active") and hasAnySibling(hasText("Microphone"))).assertIsDisplayed()
        }
    }

    @Test
    fun microphone_shows_blocked() {
        fakeRepo.setStatus(SensorType.MICROPHONE, SensorStatus.Blocked)
        waitForAssertion {
            composeRule.onNode(hasText("Blocked") and hasAnySibling(hasText("Microphone"))).assertIsDisplayed()
        }
    }

    @Test
    fun microphone_shows_denied() {
        fakeRepo.setStatus(SensorType.MICROPHONE, SensorStatus.Denied)
        waitForAssertion {
            composeRule.onNode(hasText("Denied") and hasAnySibling(hasText("Microphone"))).assertIsDisplayed()
        }
    }

    @Test
    fun camera_shows_active() {
        fakeRepo.setStatus(SensorType.CAMERA, SensorStatus.Active)
        waitForAssertion {
            composeRule.onNode(hasText("Active") and hasAnySibling(hasText("Camera"))).assertIsDisplayed()
        }
    }

    @Test
    fun camera_shows_unavailable() {
        fakeRepo.setStatus(SensorType.CAMERA, SensorStatus.Unavailable)
        waitForAssertion {
            composeRule.onNode(hasText("Unavailable") and hasAnySibling(hasText("Camera"))).assertIsDisplayed()
        }
    }
}
