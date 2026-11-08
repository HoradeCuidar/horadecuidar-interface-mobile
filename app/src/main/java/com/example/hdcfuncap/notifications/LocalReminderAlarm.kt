package com.example.hdcfuncap.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

internal object LocalReminderAlarm {
    fun schedule(context: Context, intent: Intent, at: Long, exact: Boolean): Boolean {
        return try {
            val manager = context.getSystemService(AlarmManager::class.java)
            val pending = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (exact && (Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms())) {
                try {
                    manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
                } catch (_: SecurityException) {
                    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
                }
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            }
            true
        } catch (e: RuntimeException) {
            Log.e("LocalReminderAlarm", "Unable to schedule reminder", e)
            false
        }
    }

    fun cancel(context: Context, intent: Intent) {
        try {
            PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.let {
                context.getSystemService(AlarmManager::class.java).cancel(it)
                it.cancel()
            }
        } catch (e: RuntimeException) {
            Log.w("LocalReminderAlarm", "Unable to cancel reminder", e)
        }
    }
}
