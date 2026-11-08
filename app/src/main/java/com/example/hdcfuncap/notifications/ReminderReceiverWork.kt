package com.example.hdcfuncap.notifications

import android.content.BroadcastReceiver
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout

internal fun BroadcastReceiver.runReminderWork(work: suspend () -> Unit) {
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            withTimeout(8_000) { work() }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.e("ReminderReceiver", "Failed to process local reminders", exception)
        } finally {
            pendingResult.finish()
        }
    }
}
