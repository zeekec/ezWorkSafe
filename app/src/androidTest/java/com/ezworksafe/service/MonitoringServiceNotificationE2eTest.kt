// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.service

import android.Manifest
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.ezworksafe.ui.view.MainActivity
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class MonitoringServiceNotificationE2eTest {

    private val permissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(permissionRule)
        .around(composeRule)

    @Test
    fun service_is_foreground_with_notification() {
        composeRule.waitForIdle()

        composeRule.waitUntil(10_000) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "dumpsys activity services com.ezworksafe/.service.MonitoringService"
            ).use { pfd ->
                val text = FileInputStream(pfd.fileDescriptor).use { fis ->
                    BufferedReader(InputStreamReader(fis)).readText()
                }
                text.contains("isForeground=true") && text.contains("foregroundId=1")
            }
        }
    }
}
