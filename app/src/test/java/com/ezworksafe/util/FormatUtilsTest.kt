// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FormatUtilsTest {

    @Test
    fun `formatLastUpdated returns empty for zero time`() {
        val df = SimpleDateFormat("HH:mm", Locale.US)
        assertEquals("", formatLastUpdated(0L, df))
    }

    @Test
    fun `formatLastUpdated returns Updated prefix with formatted time`() {
        val df = SimpleDateFormat("HH:mm", Locale.US)
        val time = 1700000000000L
        assertEquals("Updated ${df.format(Date(time))}", formatLastUpdated(time, df))
    }
}
