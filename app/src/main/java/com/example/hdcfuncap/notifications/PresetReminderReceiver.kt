package com.example.hdcfuncap.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PresetReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.getStringExtra(EXTRA_SLOT_KEY) ?: return
        val at = intent.getLongExtra(EXTRA_AT, 0)
        runReminderWork {
            PresetReminderScheduler.consume(context, key, at) { slot ->
                ReminderNotifications.show(context, slot.key, slot.title, slot.message, medication = false)
            }
        }
    }

    companion object {
        const val EXTRA_SLOT_KEY = "preset_slot_key"
        const val EXTRA_AT = "preset_at"
    }
}
