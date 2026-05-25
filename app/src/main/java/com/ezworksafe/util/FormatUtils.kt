// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Erik Zeek

package com.ezworksafe.util

import java.text.DateFormat
import java.util.Date

/** Formats a timestamp for the widget's "Updated" label. Returns empty string for 0. */
fun formatLastUpdated(time: Long, dateFormat: DateFormat): String {
    if (time == 0L) return ""
    return "Updated ${dateFormat.format(Date(time))}"
}
