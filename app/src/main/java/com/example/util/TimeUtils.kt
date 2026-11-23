package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeUtils {
    fun formatMinutesAr(totalMinutes: Int, isEn: Boolean = false): String {
        val normalized = ((totalMinutes % 1440) + 1440) % 1440
        val hour24 = normalized / 60
        val minute = normalized % 60
        val period = if (hour24 >= 12) {
            if (isEn) "PM" else "م"
        } else {
            if (isEn) "AM" else "ص"
        }
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return String.format(Locale.US, "%d:%02d %s", hour12, minute, period)
    }

    fun formatTodayLabelAr(isEn: Boolean = false): String {
        val locale = if (isEn) Locale.US else Locale("ar", "EG")
        val pattern = if (isEn) "d MMM yyyy" else "d MMMM yyyy"
        val sdf = SimpleDateFormat(pattern, locale)
        return sdf.format(Date())
    }

    fun formatActualDateTimeLabel(
        millis: Long = System.currentTimeMillis(),
        isEn: Boolean = false,
    ): String {
        val dateSdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
        val hour24 = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = cal.get(java.util.Calendar.MINUTE)
        val period = if (hour24 >= 12) {
            if (isEn) "PM" else "م"
        } else {
            if (isEn) "AM" else "ص"
        }
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return String.format(Locale.US, "%s - %02d:%02d %s", dateSdf.format(Date(millis)), hour12, minute, period)
    }
}
