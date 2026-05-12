package com.ezworksafe.util

import java.text.DateFormat
import java.util.Date

fun formatLastUpdated(time: Long, dateFormat: DateFormat): String {
    if (time == 0L) return ""
    return "Updated ${dateFormat.format(Date(time))}"
}
