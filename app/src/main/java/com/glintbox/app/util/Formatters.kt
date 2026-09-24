package com.glintbox.app.util

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.concurrent.TimeUnit

@Composable
fun relativeTime(timestamp: Long): String = remember(timestamp) {
    if (timestamp <= 0L) "" else DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
}

@Composable
fun fileSize(bytes: Long): String {
    val context = LocalContext.current
    return remember(bytes) { Formatter.formatShortFileSize(context, bytes) }
}

/** Statuses disappear 24 h after posting; the file time is a close approximation. Returns hours left or null. */
fun hoursUntilExpiry(modified: Long, now: Long = System.currentTimeMillis()): Long? {
    if (modified <= 0L) return null
    val left = modified + TimeUnit.HOURS.toMillis(24) - now
    return if (left <= 0) null else TimeUnit.MILLISECONDS.toHours(left).coerceAtLeast(1)
}
