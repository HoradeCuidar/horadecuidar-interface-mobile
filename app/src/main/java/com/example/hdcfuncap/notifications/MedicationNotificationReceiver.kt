package com.example.hdcfuncap.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MedicationNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.getStringExtra(EXTRA_EVENT_KEY) ?: return
        runReminderWork {
            MedicationNotificationScheduler.consume(context, key) { event ->
                ReminderNotifications.show(context, event.key.substringBeforeLast('/'), "Lembrete de medicamento",
                    "Confira sua prescrição de ${event.name} e registre no aplicativo.", medication = true)
            }
        }
    }

    companion object {
        const val EXTRA_EVENT_KEY = "medication_event_key"
    }
}
