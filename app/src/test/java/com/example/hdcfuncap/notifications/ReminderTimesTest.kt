package com.example.hdcfuncap.notifications

import java.text.SimpleDateFormat
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class ReminderTimesTest {
    private val zone = TimeZone.getTimeZone("UTC")
    private fun at(value: String, timeZone: TimeZone = zone): Long =
        SimpleDateFormat("yyyy-MM-dd HH:mm").apply { this.timeZone = timeZone }.parse(value)!!.time

    @Test fun hourlyStartsAfterFirstRecord() {
        val anchor = at("2026-10-11 09:00")
        assertEquals(listOf(at("2026-10-11 10:00"), at("2026-10-11 11:00")),
            ReminderTimes.medication(anchor, anchor, at("2026-10-11 12:00"), 1, "HORA", zone))
    }

    @Test fun tenHoursNeverRestartsAtEightInTheMorning() {
        val anchor = at("2026-10-11 09:00")
        assertEquals(listOf(at("2026-10-11 19:00"), at("2026-10-12 05:00"), at("2026-10-12 15:00")),
            ReminderTimes.medication(anchor, anchor, at("2026-10-12 20:00"), 10, "HORA", zone))
    }

    @Test fun renewalSkipsPastAlarmsWithoutMovingAnchor() {
        val anchor = at("2026-01-01 09:00")
        assertEquals(listOf(at("2026-10-11 17:00")), ReminderTimes.medication(
            anchor, at("2026-10-11 12:00"), at("2026-10-12 00:00"), 10, "HORA", zone))
    }

    @Test fun dailyIntervalUsesOriginalTime() {
        val anchor = at("2026-10-11 09:00")
        assertEquals(listOf(at("2026-10-13 09:00"), at("2026-10-15 09:00")),
            ReminderTimes.medication(anchor, anchor, at("2026-10-16 00:00"), 2, "DIA", zone))
    }

    @Test fun weeklyInterval() {
        val anchor = at("2026-10-11 09:00")
        assertEquals(listOf(at("2026-10-18 09:00")),
            ReminderTimes.medication(anchor, anchor, at("2026-10-20 00:00"), 1, "SEMANA", zone))
    }

    @Test fun monthlyIntervalSkipsMissingDaysLikeTheBackend() {
        val anchor = at("2026-01-31 09:00")
        assertEquals(listOf(at("2026-03-31 09:00")),
            ReminderTimes.medication(anchor, anchor, at("2026-04-01 00:00"), 1, "MES", zone))
    }

    @Test fun unsupportedAndInvalidIntervalsAreIgnored() {
        val anchor = at("2026-10-11 09:00")
        for ((value, type) in listOf(0 to "HORA", -1 to "DIA", 1 to "INVALID")) {
            assertTrue(ReminderTimes.medication(anchor, anchor, anchor + REMINDER_WINDOW_MILLIS, value, type, zone).isEmpty())
        }
        assertTrue(ReminderTimes.medication(0, anchor, anchor + REMINDER_WINDOW_MILLIS, 1, "HORA", zone).isEmpty())
        assertTrue(ReminderTimes.medication(anchor + 1, anchor, anchor + REMINDER_WINDOW_MILLIS, 1, "HORA", zone).isEmpty())
    }

    @Test fun largeIntervalDoesNotOverflowOrLoopIndefinitely() {
        val anchor = at("2026-10-11 09:00")
        assertTrue(ReminderTimes.medication(anchor, anchor, anchor + REMINDER_WINDOW_MILLIS, Int.MAX_VALUE, "HORA", zone).isEmpty())
    }

    @Test fun scheduleIsBoundedToThreeDays() {
        val anchor = at("2026-10-11 09:00")
        val results = ReminderTimes.medication(anchor, anchor, anchor + REMINDER_WINDOW_MILLIS, 1, "HORA", zone)
        assertEquals(71, results.size)
        assertTrue(results.all { it > anchor && it < anchor + REMINDER_WINDOW_MILLIS })
    }

    @Test fun prescriptionEndIsInclusiveButFollowingDayIsExcluded() {
        assertEquals(at("2026-10-12 00:00"), ReminderTimes.dateBoundary("2026-10-11", zone, true))
        assertNull(ReminderTimes.dateBoundary("2026-02-30", zone))
        assertNull(ReminderTimes.dateBoundary("garbage", zone))
    }

    @Test fun noonRemindersHaveIndependentIdentities() {
        assertNotEquals(PresetReminderSlot("water", 12, "", "").key, PresetReminderSlot("check_in", 12, "", "").key)
        assertEquals(at("2026-10-12 12:00"), ReminderTimes.nextDaily(12, at("2026-10-11 12:00"), zone))
    }

    @Test fun dailyReminderPreservesLocalHourAcrossDaylightSaving() {
        val ny = TimeZone.getTimeZone("America/New_York")
        val anchor = at("2026-03-07 09:00", ny)
        assertEquals(at("2026-03-08 09:00", ny),
            ReminderTimes.medication(anchor, anchor, anchor + 86_400_000L, 1, "DIA", ny).first())
    }
}
