package com.example.hdcfuncap.notifications

import android.content.BroadcastReceiver
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal fun BroadcastReceiver.runReminderWork(work: () -> Unit) {
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            work()
        } catch (exception: Exception) {
            Log.e("ReminderReceiver", "Failed to process local reminders", exception)
        } finally {
            pendingResult.finish()
        }
    }
}
