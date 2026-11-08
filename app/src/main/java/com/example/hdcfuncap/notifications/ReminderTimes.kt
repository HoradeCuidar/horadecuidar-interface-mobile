package com.example.hdcfuncap.notifications

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

internal const val REMINDER_WINDOW_MILLIS = 3L * 24 * 60 * 60 * 1000

internal object ReminderTimes {
    fun dateBoundary(date: String, zone: TimeZone, endExclusive: Boolean = false): Long? {
        if (!date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return null
        return try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = zone
                isLenient = false
            }.parse(date) ?: return null
            Calendar.getInstance(zone).apply {
                time = parsed
                if (endExclusive) add(Calendar.DAY_OF_MONTH, 1)
            }.timeInMillis
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: java.text.ParseException) {
            null
        }
    }

    fun medication(
        anchor: Long,
        now: Long,
        untilExclusive: Long,
        intervalValue: Int,
        intervalType: String,
        zone: TimeZone = TimeZone.getDefault()
    ): List<Long> {
        if (anchor <= 0 || anchor > now || intervalValue <= 0 || untilExclusive <= now) return emptyList()
        val type = intervalType.trim().uppercase(Locale.ROOT)
        val field = when (type) {
            "HORA", "HORAS" -> Calendar.HOUR_OF_DAY
            "DIA", "DIAS" -> Calendar.DAY_OF_MONTH
            "SEMANA", "SEMANAS" -> Calendar.WEEK_OF_YEAR
            "MES", "MESES" -> Calendar.MONTH
            else -> return emptyList()
        }
        val origin = Calendar.getInstance(zone).apply { timeInMillis = anchor }
        val current = Calendar.getInstance(zone).apply { timeInMillis = now }
        val elapsed = now - anchor
        val approximateCycles = when (field) {
            Calendar.HOUR_OF_DAY -> elapsed / (intervalValue.toLong() * 3_600_000L)
            Calendar.MONTH -> ((current.get(Calendar.YEAR) - origin.get(Calendar.YEAR)).toLong() * 12 +
                current.get(Calendar.MONTH) - origin.get(Calendar.MONTH)) / intervalValue
            else -> elapsed / (intervalValue.toLong() * 86_400_000L *
                if (field == Calendar.WEEK_OF_YEAR) 7 else 1)
        }
        var cycle = (approximateCycles - 2).coerceAtLeast(1)
        return buildList {
            // Always calculate from the original anchor, never from a shortened month.
            repeat(128) {
                val amount = cycle * intervalValue.toLong()
                if (amount > Int.MAX_VALUE) return@buildList
                val candidate = (origin.clone() as Calendar).apply { add(field, amount.toInt()) }.timeInMillis
                if (candidate >= untilExclusive || candidate <= anchor) return@buildList
                val sameMonthDay = field != Calendar.MONTH || Calendar.getInstance(zone).apply {
                    timeInMillis = candidate
                }.get(Calendar.DAY_OF_MONTH) == origin.get(Calendar.DAY_OF_MONTH)
                if (candidate > now && sameMonthDay) add(candidate)
                cycle++
            }
        }
    }

    fun nextDaily(hour: Int, now: Long, zone: TimeZone = TimeZone.getDefault()): Long {
        require(hour in 0..23)
        return Calendar.getInstance(zone).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
    }
}
