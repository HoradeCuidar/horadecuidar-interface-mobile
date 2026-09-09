package com.example.hdcfuncap.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MedicationNotificationBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            try {
                MedicationNotificationScheduler.rescheduleSavedNotifications(context)
            } catch (_: Exception) {
                // Reinicialização do aparelho não pode quebrar a abertura futura do app.
            }
        }
    }
}
