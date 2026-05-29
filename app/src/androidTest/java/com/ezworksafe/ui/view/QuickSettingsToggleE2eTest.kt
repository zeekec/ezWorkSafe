// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.ui.view

import android.Manifest
import android.os.Build
import com.ezworksafe.util.PermissionHelper
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickSettingsToggleE2eTest {

    private val permissionRule = GrantPermissionRule.grant(
        *PermissionHelper.REQUIRED_RUNTIME_PERMISSIONS
    )

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(permissionRule)
        .around(composeRule)

    @After
    fun restorePrivacyToggles() {
        executeShell("cmd sensor_privacy disable 0 microphone")
        executeShell("cmd sensor_privacy disable 0 camera")
    }

    @Test
    fun micShowsBlockedWhenQuickSettingsToggleIsEnabled() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)

        ensureMicActive()
        ensureMicBlocked()
    }

    @Test
    fun micRestoresToActiveWhenQuickSettingsToggleIsDisabled() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)

        ensureMicBlocked()
        ensureMicActive()
    }

    @Test
    fun camShowsBlockedWhenQuickSettingsToggleIsEnabled() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)

        ensureCamActive()
        ensureCamBlocked()
    }

    @Test
    fun camRestoresToActiveWhenQuickSettingsToggleIsDisabled() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)

        ensureCamBlocked()
        ensureCamActive()
    }

    @Test
    fun micShowsBlockedViaPollingWhenQSToggled() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)

        ensureMicActive()
        executeShell("cmd sensor_privacy enable 0 microphone")
        composeRule.waitUntil(6000) {
            composeRule.onAllNodesWithText("Blocked").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun micRestoresToActiveViaPollingWhenQSToggled() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)

        ensureMicBlocked()
        executeShell("cmd sensor_privacy disable 0 microphone")
        composeRule.waitUntil(6000) {
            composeRule.onAllNodesWithText("Active").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun camShowsBlockedViaPollingWhenQSToggled() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)

        ensureCamActive()
        executeShell("cmd sensor_privacy enable 0 camera")
        composeRule.waitUntil(6000) {
            composeRule.onAllNodesWithText("Blocked").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun camRestoresToActiveViaPollingWhenQSToggled() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)

        ensureCamBlocked()
        executeShell("cmd sensor_privacy disable 0 camera")
        composeRule.waitUntil(6000) {
            composeRule.onAllNodesWithText("Active").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun ensureMicActive() {
        executeShell("cmd sensor_privacy disable 0 microphone")
        cycleActivityLifecycle()
        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithText("Active").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun ensureMicBlocked() {
        executeShell("cmd sensor_privacy enable 0 microphone")
        cycleActivityLifecycle()
        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithText("Blocked").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun ensureCamActive() {
        executeShell("cmd sensor_privacy disable 0 camera")
        cycleActivityLifecycle()
        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithText("Active").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun ensureCamBlocked() {
        executeShell("cmd sensor_privacy enable 0 camera")
        cycleActivityLifecycle()
        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithText("Blocked").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun cycleActivityLifecycle() {
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
    }

    private fun executeShell(command: String) {
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand(command)
            .use { }
    }
}
