package com.example.hdcfuncap.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.hdcfuncap.storage.UserPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object PresetReminderScheduler {
    const val SCOPE_WATER = "water"
    const val SCOPE_CHECK_IN = "check_in"
    private const val PREFS_NAME = "hdc_preset_reminders"
    private val mutex = Mutex()
    private val slots = (8..22 step 2).map { hour ->
        PresetReminderSlot(SCOPE_WATER, hour, "Hora da água", "Que tal fazer uma pausa para tomar água?")
    } + listOf(
        PresetReminderSlot(SCOPE_CHECK_IN, 9, "Bom dia!", "Que tal vermos o que temos planejado sobre a nossa saúde para hoje?"),
        PresetReminderSlot(SCOPE_CHECK_IN, 12, "Pausa do almoço", "Que tal aproveitar a pausa do almoço para ver o que ainda temos que fazer daqui para frente?"),
        PresetReminderSlot(SCOPE_CHECK_IN, 21, "Fechamento do dia", "Que tal recapitular para garantir que não esquecemos de nada?")
    )

    suspend fun refreshPresetReminders(context: Context) = mutex.withLock {
        migrateLegacyAlarms(context)
        val preferences = UserPreferences(context)
        val waterEnabled = preferences.waterRemindersEnabled.first()
        val checkInEnabled = preferences.checkInRemindersEnabled.first()
        slots.forEach { slot ->
            if (if (slot.scope == SCOPE_WATER) waterEnabled else checkInEnabled) {
                scheduleNext(context, slot, preserveDue = true)
            } else {
                cancel(context, slot)
            }
        }
    }

    suspend fun setWaterRemindersEnabled(context: Context, enabled: Boolean) = setScopeEnabled(context, SCOPE_WATER, enabled)

    suspend fun setCheckInRemindersEnabled(context: Context, enabled: Boolean) = setScopeEnabled(context, SCOPE_CHECK_IN, enabled)

    private suspend fun setScopeEnabled(context: Context, scope: String, enabled: Boolean) = mutex.withLock {
        slots.filter { it.scope == scope }.forEach {
            if (enabled) scheduleNext(context, it, preserveDue = true) else cancel(context, it)
        }
    }

    internal suspend fun consume(context: Context, key: String, at: Long, deliver: (PresetReminderSlot) -> Unit) = mutex.withLock {
        val slot = slots.firstOrNull { it.key == key } ?: return@withLock
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (preferences.getLong(slot.key, 0) != at) return@withLock
        val userPreferences = UserPreferences(context)
        val enabled = if (slot.scope == SCOPE_WATER) userPreferences.waterRemindersEnabled.first()
            else userPreferences.checkInRemindersEnabled.first()
        if (!enabled) {
            cancel(context, slot)
            return@withLock
        }
        val now = System.currentTimeMillis()
        if (now < at) return@withLock
        // Renew only this slot. Water and check-in at noon remain independent.
        scheduleNext(context, slot, preserveDue = false)
        if (now - at <= 3_600_000L) deliver(slot)
    }

    private fun scheduleNext(context: Context, slot: PresetReminderSlot, preserveDue: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val previous = prefs.getLong(slot.key, 0)
        val at = if (preserveDue && previous <= now && now - previous in 0..3_600_000L) previous
            else ReminderTimes.nextDaily(slot.hour, now)
        check(prefs.edit().putLong(slot.key, at).commit())
        LocalReminderAlarm.schedule(context, intent(context, slot, at), at, exact = false)
    }

    private fun cancel(context: Context, slot: PresetReminderSlot) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().remove(slot.key).apply()
        LocalReminderAlarm.cancel(context, intent(context, slot, 0))
        androidx.core.app.NotificationManagerCompat.from(context).cancel(slot.key, 0)
    }

    private fun intent(context: Context, slot: PresetReminderSlot, at: Long) = Intent(context, PresetReminderReceiver::class.java).apply {
        data = Uri.Builder().scheme("hdc").authority("preset-reminder").appendPath(slot.key).build()
        putExtra(PresetReminderReceiver.EXTRA_SLOT_KEY, slot.key)
        putExtra(PresetReminderReceiver.EXTRA_AT, at)
    }

    private fun migrateLegacyAlarms(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean("legacy_migrated", false)) return
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        repeat(9) {
            val day = formatter.format(calendar.time)
            slots.forEach { slot ->
                val code = "preset-${slot.scope}-$day-${slot.hour}-0".hashCode()
                PendingIntent.getBroadcast(context, code, Intent(context, PresetReminderReceiver::class.java),
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let { pending ->
                    context.getSystemService(AlarmManager::class.java).cancel(pending)
                    pending.cancel()
                }
            }
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        prefs.edit().putBoolean("legacy_migrated", true).apply()
    }
}

internal data class PresetReminderSlot(val scope: String, val hour: Int, val title: String, val message: String) {
    val key: String get() = "$scope/$hour"
}
