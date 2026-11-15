package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeUtils {
    fun formatMinutesAr(totalMinutes: Int): String {
        val normalized = ((totalMinutes % 1440) + 1440) % 1440
        val hour24 = normalized / 60
        val minute = normalized % 60
        val period = if (hour24 >= 12) "م" else "ص"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return String.format(Locale.US, "%d:%02d %s", hour12, minute, period)
    }

    fun formatTodayLabelAr(): String {
        val sdf = SimpleDateFormat("d MMMM yyyy", Locale("ar", "EG"))
        return sdf.format(Date())
    }
}
