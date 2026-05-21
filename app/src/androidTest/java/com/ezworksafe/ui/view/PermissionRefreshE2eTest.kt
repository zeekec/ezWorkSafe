// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.ui.view

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

@Ignore("pm revoke kills the app process by design — Android enforces permission revocation by killing the running process. The instrumentation dies before any assertions can run. This is fundamental Android security behavior, not a testing or API-level issue.")
class PermissionRefreshE2eTest {

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

    @Test
    fun revokingCamera_showsDenied_afterResume() {
        composeRule.waitForIdle()

        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "pm revoke com.ezworksafe android.permission.CAMERA"
        ).close()

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        composeRule.waitUntil(10_000) {
            try {
                composeRule.onNodeWithText("Denied").assertExists()
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }
}
