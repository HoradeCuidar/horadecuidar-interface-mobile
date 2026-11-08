package com.example.hdcfuncap.notifications

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.hdcfuncap.network.RetrofitClient
import com.example.hdcfuncap.storage.UserPreferences
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class ReminderRefreshWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            PresetReminderScheduler.refreshPresetReminders(applicationContext)
            val prefs = UserPreferences(applicationContext)
            val session = prefs.getPatientSession()
            if (session != null) MedicationNotificationScheduler.activatePatient(applicationContext, session.pacienteId)
            MedicationNotificationScheduler.rescheduleSavedNotifications(applicationContext)
            if (session != null) {
                MedicationNotificationScheduler.refreshMedicationNotifications(applicationContext,
                    RetrofitClient.getReminderApi(applicationContext, session.token), session.pacienteId)
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.w("ReminderRefresh", "Network unavailable; keeping saved reminders", e)
            retryTransient()
        } catch (e: HttpException) {
            Log.w("ReminderRefresh", "Reminder refresh rejected: HTTP ${e.code()}")
            if (e.code() == 429 || e.code() >= 500) retryTransient() else Result.success()
        } catch (e: Exception) {
            Log.e("ReminderRefresh", "Unable to refresh reminders", e)
            Result.failure()
        }
    }

    private fun retryTransient() = if (runAttemptCount < 3) Result.retry() else Result.failure()
}

object ReminderRefreshWork {
    fun enqueue(context: Context): Boolean {
        return try {
            val manager = WorkManager.getInstance(context.applicationContext)
            manager.enqueueUniquePeriodicWork("hdc-reminders-periodic", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ReminderRefreshWorker>(12, TimeUnit.HOURS)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES).build())
            // Durable and independent of Compose navigation; serializes concurrent refresh requests.
            manager.enqueueUniqueWork("hdc-reminders-refresh", ExistingWorkPolicy.APPEND_OR_REPLACE,
                OneTimeWorkRequestBuilder<ReminderRefreshWorker>()
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES).build())
            true
        } catch (e: RuntimeException) {
            Log.e("ReminderRefresh", "Unable to enqueue reminder refresh", e)
            false
        }
    }
}
