package com.jsworld.android.autolog.core.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

object WeekTimeUtils {

    fun getStartOfWeekMillis(): Long {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val monday = today.with(DayOfWeek.MONDAY)

        return monday.atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }
}