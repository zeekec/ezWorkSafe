package com.ezworksafe.ui.view

import android.Manifest
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

@Ignore("executeShellCommand crashes on API 36; revisit with proper Android 16 shell API")
class PermissionRefreshE2eTest {

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
