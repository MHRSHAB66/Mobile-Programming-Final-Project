package com.example.project.core.util

import java.util.concurrent.TimeUnit

/** Formats a millisecond duration/position as m:ss. */
fun Long.asTrackTime(): String {
    if (this <= 0L) return "0:00"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60
    return "%d:%02d".format(minutes, seconds)
}

/** Coarse "time ago" label for chat/conversation timestamps. */
fun Long.asTimeAgo(now: Long = System.currentTimeMillis()): String {
    val diff = (now - this).coerceAtLeast(0)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        days < 7 -> "${days}d"
        else -> "${days / 7}w"
    }
}

/** Clock time HH:mm for a message bubble, in the device's local timezone. */
fun Long.asClockTime(): String =
    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(this))

/** Compact follower/listener counts: 1.8M, 540K, 128. */
fun Int.asCompactCount(): String = when {
    this >= 1_000_000 -> "%.1fM".format(this / 1_000_000.0)
    this >= 1_000 -> "%.1fK".format(this / 1_000.0)
    else -> toString()
}
