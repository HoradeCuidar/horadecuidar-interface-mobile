package com.example.hdcfuncap.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.hdcfuncap.MainActivity
import com.example.hdcfuncap.R

class PresetReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runReminderWork { deliverReminder(context.applicationContext, intent) }
    }

    private fun deliverReminder(context: Context, intent: Intent) {
        val scope = intent.getStringExtra(EXTRA_SCOPE).orEmpty()
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, scope.hashCode())

        if (!PresetReminderScheduler.isScopeEnabled(context, scope)) return

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Hora de cuidar"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Que tal conferir seu cuidado de hoje?"

        createChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.hdcmainlogo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Sem permissão de notificações no Android 13+, a entrega é ignorada com segurança.
        }

        PresetReminderScheduler.refreshPresetReminders(context)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Lembretes de cuidado",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Lembretes locais para hidratação e registros diários."
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "preset_care_reminders"
        const val EXTRA_SCOPE = "extra_scope"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
