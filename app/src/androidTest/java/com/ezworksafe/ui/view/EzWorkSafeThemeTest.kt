// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.ui.view

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class EzWorkSafeThemeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun theme_rendersContent() {
        composeTestRule.setContent {
            EzWorkSafeTheme {
                androidx.compose.material3.Text("hello")
            }
        }
        composeTestRule.onNodeWithText("hello").assertExists()
    }

    @Test
    fun lightAndDarkThemes_produceDifferentColorSchemes() {
        val lightBg = mutableStateOf(Color.Unspecified)
        val darkBg = mutableStateOf(Color.Unspecified)

        composeTestRule.setContent {
            androidx.compose.foundation.layout.Column {
                EzWorkSafeTheme(darkTheme = false) {
                    lightBg.value = MaterialTheme.colorScheme.background
                }
                EzWorkSafeTheme(darkTheme = true) {
                    darkBg.value = MaterialTheme.colorScheme.background
                }
            }
        }

        assertTrue("light and dark backgrounds should differ", lightBg.value != darkBg.value)
    }
}
