package com.example.hdcfuncap.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import com.example.hdcfuncap.storage.UserPreferences

object PresetReminderScheduler {
    private const val DAYS_TO_SCHEDULE = 7
    private const val WATER_START_HOUR = 8
    private const val WATER_END_HOUR = 22

    const val SCOPE_WATER = "water"
    const val SCOPE_CHECK_IN = "check_in"

    private val checkInReminders = listOf(
        FixedReminder(
            hour = 9,
            minute = 0,
            title = "Bom dia!",
            message = "Que tal vermos o que temos planejado sobre a nossa saúde para hoje?"
        ),
        FixedReminder(
            hour = 12,
            minute = 0,
            title = "Pausa do almoço",
            message = "Que tal aproveitar a pausa do almoço para ver o que ainda temos que fazer daqui para frente?"
        ),
        FixedReminder(
            hour = 21,
            minute = 0,
            title = "Fechamento do dia",
            message = "Que tal recapitular para garantir que não esquecemos de nada?"
        )
    )

    fun refreshPresetReminders(context: Context) {
        val appContext = context.applicationContext
        val preferences = UserPreferences(appContext)
        val waterEnabled = runBlocking {
            preferences.waterRemindersEnabled.firstOrNull() ?: true
        }
        val checkInEnabled = runBlocking {
            preferences.checkInRemindersEnabled.firstOrNull() ?: true
        }

        if (waterEnabled) {
            scheduleWaterReminders(appContext)
        } else {
            cancelScope(appContext, SCOPE_WATER)
        }

        if (checkInEnabled) {
            scheduleCheckInReminders(appContext)
        } else {
            cancelScope(appContext, SCOPE_CHECK_IN)
        }
    }

    fun setWaterRemindersEnabled(context: Context, enabled: Boolean) {
        if (enabled) {
            scheduleWaterReminders(context.applicationContext)
        } else {
            cancelScope(context.applicationContext, SCOPE_WATER)
        }
    }

    fun setCheckInRemindersEnabled(context: Context, enabled: Boolean) {
        if (enabled) {
            scheduleCheckInReminders(context.applicationContext)
        } else {
            cancelScope(context.applicationContext, SCOPE_CHECK_IN)
        }
    }

    fun isScopeEnabled(context: Context, scope: String): Boolean {
        val preferences = UserPreferences(context.applicationContext)
        return runBlocking {
            when (scope) {
                SCOPE_WATER -> preferences.waterRemindersEnabled.firstOrNull() ?: true
                SCOPE_CHECK_IN -> preferences.checkInRemindersEnabled.firstOrNull() ?: true
                else -> false
            }
        }
    }

    private fun scheduleWaterReminders(context: Context) {
        cancelScope(context, SCOPE_WATER)
        val reminders = buildList {
            forEachScheduledDay { day ->
                var hour = WATER_START_HOUR
                while (hour <= WATER_END_HOUR) {
                    add(
                        ScheduledPresetReminder(
                            scope = SCOPE_WATER,
                            requestCode = requestCode(SCOPE_WATER, day, hour, 0),
                            triggerAtMillis = triggerAt(day, hour, 0),
                            title = "Hora da água",
                            message = "Que tal fazer uma pausa para tomar água?"
                        )
                    )
                    hour += 2
                }
            }
        }

        reminders
            .filter { it.triggerAtMillis > System.currentTimeMillis() }
            .forEach { schedule(context, it) }
    }

    private fun scheduleCheckInReminders(context: Context) {
        cancelScope(context, SCOPE_CHECK_IN)
        val reminders = buildList {
            forEachScheduledDay { day ->
                checkInReminders.forEach { reminder ->
                    add(
                        ScheduledPresetReminder(
                            scope = SCOPE_CHECK_IN,
                            requestCode = requestCode(SCOPE_CHECK_IN, day, reminder.hour, reminder.minute),
                            triggerAtMillis = triggerAt(day, reminder.hour, reminder.minute),
                            title = reminder.title,
                            message = reminder.message
                        )
                    )
                }
            }
        }

        reminders
            .filter { it.triggerAtMillis > System.currentTimeMillis() }
            .forEach { schedule(context, it) }
    }

    private fun cancelScope(context: Context, scope: String) {
        forEachScheduledDay { day ->
            if (scope == SCOPE_WATER) {
                var hour = WATER_START_HOUR
                while (hour <= WATER_END_HOUR) {
                    cancel(context, requestCode(scope, day, hour, 0))
                    hour += 2
                }
            }

            if (scope == SCOPE_CHECK_IN) {
                checkInReminders.forEach { reminder ->
                    cancel(context, requestCode(scope, day, reminder.hour, reminder.minute))
                }
            }
        }
    }

    private fun schedule(context: Context, reminder: ScheduledPresetReminder) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = pendingIntentFor(context, reminder)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: Exception) {
            // Restrições do aparelho não devem afetar o uso normal do app.
        }
    }

    private fun cancel(context: Context, requestCode: Int) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            pendingIntentForCancellation(context, requestCode)?.let { pendingIntent ->
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (_: Exception) {
            // Cancelar é uma rotina defensiva; falhas aqui não podem quebrar a UI.
        }
    }

    private fun pendingIntentFor(
        context: Context,
        reminder: ScheduledPresetReminder
    ): PendingIntent {
        val intent = Intent(context, PresetReminderReceiver::class.java).apply {
            putExtra(PresetReminderReceiver.EXTRA_SCOPE, reminder.scope)
            putExtra(PresetReminderReceiver.EXTRA_TITLE, reminder.title)
            putExtra(PresetReminderReceiver.EXTRA_MESSAGE, reminder.message)
            putExtra(PresetReminderReceiver.EXTRA_NOTIFICATION_ID, reminder.requestCode)
        }

        return PendingIntent.getBroadcast(
            context,
            reminder.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun pendingIntentForCancellation(context: Context, requestCode: Int): PendingIntent? {
        val intent = Intent(context, PresetReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun forEachScheduledDay(block: (String) -> Unit) {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calendar = Calendar.getInstance()
        repeat(DAYS_TO_SCHEDULE) { offset ->
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_MONTH, offset)
            block(formatter.format(calendar.time))
        }
    }

    private fun triggerAt(date: String, hour: Int, minute: Int): Long {
        val parts = date.split("-")
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, parts.getOrNull(0)?.toIntOrNull() ?: get(Calendar.YEAR))
            set(Calendar.MONTH, (parts.getOrNull(1)?.toIntOrNull() ?: (get(Calendar.MONTH) + 1)) - 1)
            set(Calendar.DAY_OF_MONTH, parts.getOrNull(2)?.toIntOrNull() ?: get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun requestCode(scope: String, date: String, hour: Int, minute: Int): Int {
        return "preset-$scope-$date-$hour-$minute".hashCode()
    }
}

private data class FixedReminder(
    val hour: Int,
    val minute: Int,
    val title: String,
    val message: String
)

private data class ScheduledPresetReminder(
    val scope: String,
    val requestCode: Int,
    val triggerAtMillis: Long,
    val title: String,
    val message: String
)
