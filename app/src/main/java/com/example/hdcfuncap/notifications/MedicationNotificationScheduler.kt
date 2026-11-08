package com.example.hdcfuncap.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.hdcfuncap.network.AuthApi
import com.example.hdcfuncap.network.MedicamentoHojeResponse
import com.example.hdcfuncap.network.PrescricaoMedicamentoResponse
import com.example.hdcfuncap.storage.UserPreferences
import com.google.gson.Gson
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray

object MedicationNotificationScheduler {
    private const val PREFS_NAME = "hdc_medication_notifications"
    private const val STATE_KEY = "reminder_state_v2"
    private const val ENABLED_KEY = "enabled"
    private val lock = Any()
    private val refreshMutex = Mutex()
    private val gson = Gson()

    fun isEnabled(context: Context): Boolean = try {
        preferences(context).getBoolean(ENABLED_KEY, true)
    } catch (e: ClassCastException) {
        Log.e("MedicationReminders", "Invalid reminder preference", e)
        false
    }

    fun setEnabled(context: Context, enabled: Boolean) = synchronized(lock) {
        check(preferences(context).edit().putBoolean(ENABLED_KEY, enabled).commit())
        val state = read(context)
        if (!enabled) cancelDisplayed(context, "${state.patientId}/")
        rebuild(context, state)
    }

    suspend fun activatePatient(context: Context, patientId: Long) {
        if (UserPreferences(context).pacienteId.firstOrNull() != patientId) return
        synchronized(lock) {
            val state = read(context)
            if (state.patientId != patientId) {
                state.events.forEach { cancel(context, it) }
                cancelDisplayed(context, "${state.patientId}/")
                write(context, MedicationReminderState(patientId = patientId))
            }
            migrateLegacyAlarms(context)
        }
    }

    suspend fun refreshMedicationNotifications(context: Context, authApi: AuthApi, pacienteId: Long) = refreshMutex.withLock {
        val prescriptions = authApi.getPrescricoesMedicamentos(pacienteId)
        val currentPatient = UserPreferences(context).pacienteId.firstOrNull()
        if (currentPatient != pacienteId) return@withLock
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val state = read(context)
                if (state.patientId != pacienteId) return@synchronized
                val plans = prescriptions.toReminderPlans()
                val keys = plans.map { it.key }.toSet()
                state.plans.filter { it.key !in keys }.forEach {
                    cancelDisplayed(context, "$pacienteId/${it.key}")
                }
                rebuild(context, state.copy(
                    validatedAt = System.currentTimeMillis(),
                    plans = plans,
                    anchors = state.anchors.filterKeys { it in keys }
                ))
            }
        }
    }

    fun onMedicationRecorded(
        context: Context,
        patientId: Long,
        medication: MedicamentoHojeResponse,
        status: String,
        recordedAt: Long
    ) = synchronized(lock) {
        val state = read(context)
        if (state.patientId != patientId) return@synchronized
        val key = medicationKey(medication.prescricaoId, medication.itemId)
        val occurrenceId = medication.ocorrenciaId ?: return@synchronized
        val anchors = updatedMedicationAnchors(state.anchors, key, occurrenceId, status, recordedAt)
        cancelDisplayed(context, "$patientId/$key")
        val obsolete = state.events.filter { it.itemKey == key && it.at <= recordedAt }
        obsolete.forEach { cancel(context, it) }
        rebuild(context, state.copy(anchors = anchors, events = state.events - obsolete.toSet()))
    }

    fun rescheduleSavedNotifications(context: Context) = synchronized(lock) {
        migrateLegacyAlarms(context)
        rebuild(context, read(context))
    }

    internal suspend fun consume(context: Context, eventKey: String, deliver: (MedicationReminderEvent) -> Unit) {
        val currentPatient = UserPreferences(context).pacienteId.firstOrNull()
        synchronized(lock) {
            val state = read(context)
            if (currentPatient != null && currentPatient != state.patientId) return@synchronized
            val event = state.events.firstOrNull { it.key == eventKey } ?: return@synchronized
            val now = System.currentTimeMillis()
            if (!isEnabled(context) || now < event.at) return@synchronized
            val remaining = state.events.filterNot { it.key == eventKey || (it.itemKey == event.itemKey && it.at <= now) }
            write(context, state.copy(events = remaining))
            remaining.filter { it.itemKey == event.itemKey && it.at > now }.minByOrNull { it.at }?.let {
                LocalReminderAlarm.schedule(context, intent(context, it.key), it.at, exact = true)
            }
            // Serialize delivery with records and toggles, so cancellation cannot race a late post.
            if (now - event.at <= 3_600_000L && now < state.validatedAt + REMINDER_WINDOW_MILLIS) deliver(event)
        }
    }

    private fun rebuild(context: Context, state: MedicationReminderState) {
        val now = System.currentTimeMillis()
        val zone = TimeZone.getDefault()
        val horizon = state.validatedAt + REMINDER_WINDOW_MILLIS
        val futureEvents = if (!isEnabled(context) || horizon <= now) emptyList() else state.plans.flatMap { plan ->
            val anchor = state.anchors[plan.key] ?: return@flatMap emptyList()
            val start = ReminderTimes.dateBoundary(plan.startDate, zone) ?: return@flatMap emptyList()
            val end = ReminderTimes.dateBoundary(plan.endDate, zone, true) ?: return@flatMap emptyList()
            ReminderTimes.medication(anchor.at, now, minOf(end, horizon), plan.intervalValue, plan.intervalType, zone)
                .filter { it >= start }
                .map { at -> MedicationReminderEvent(
                    key = "${state.patientId}/${plan.key}/$at",
                    itemKey = plan.key,
                    at = at,
                    name = plan.name
                ) }
        }
        val activeKeys = state.plans.map { it.key }.toSet()
        val dueEvents = if (!isEnabled(context) || horizon <= now) emptyList() else state.events.filter {
            it.itemKey in activeKeys && it.itemKey in state.anchors && it.at <= now && now - it.at <= 3_600_000L
        }
        val events = (dueEvents + futureEvents).distinctBy { it.key }
        val eventKeys = events.map { it.key }.toSet()
        state.events.filter { it.key !in eventKeys }.forEach { cancel(context, it) }
        // Persist first so receivers can reject cancelled or obsolete PendingIntents.
        write(context, state.copy(events = events))
        // Only the next alarm per item is handed to Android; the bounded queue stays on disk.
        events.groupBy { it.itemKey }.values.mapNotNull { it.minByOrNull { event -> event.at } }.forEach { event ->
            LocalReminderAlarm.schedule(context, intent(context, event.key), event.at, exact = true)
        }
    }

    private fun intent(context: Context, key: String) = Intent(context, MedicationNotificationReceiver::class.java).apply {
        data = Uri.Builder().scheme("hdc").authority("medication-reminder").appendPath(key).build()
        putExtra(MedicationNotificationReceiver.EXTRA_EVENT_KEY, key)
    }

    private fun cancel(context: Context, event: MedicationReminderEvent) {
        LocalReminderAlarm.cancel(context, intent(context, event.key))
        NotificationManagerCompat.from(context).cancel(event.key.substringBeforeLast('/'), 0)
    }

    private fun cancelDisplayed(context: Context, prefix: String) {
        context.getSystemService(android.app.NotificationManager::class.java).activeNotifications
            .filter { it.tag == prefix || it.tag?.startsWith("$prefix/") == true ||
                (prefix.endsWith('/') && it.tag?.startsWith(prefix) == true) }
            .forEach { NotificationManagerCompat.from(context).cancel(it.tag, it.id) }
    }

    private fun preferences(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun read(context: Context): MedicationReminderState = try {
        preferences(context).getString(STATE_KEY, null)?.let {
            gson.fromJson(it, MedicationReminderState::class.java)?.also { state ->
                require(state.plans.size <= 1000 && state.events.size <= 128000)
                require(state.validatedAt in 0..(Long.MAX_VALUE - REMINDER_WINDOW_MILLIS))
                require(state.anchors.values.all { anchor -> anchor.at > 0 })
                require(state.plans.all { plan -> plan.key.isNotBlank() && plan.name.isNotBlank() && plan.intervalValue > 0 })
                require(state.events.all { event -> event.key.isNotBlank() && event.name.isNotBlank() && event.at > 0 })
            }
        } ?: MedicationReminderState()
    } catch (e: RuntimeException) {
        Log.e("MedicationReminders", "Invalid saved reminder state", e)
        MedicationReminderState()
    }

    private fun write(context: Context, state: MedicationReminderState) {
        check(preferences(context).edit().putString(STATE_KEY, gson.toJson(state)).commit()) {
            "Unable to persist medication reminders"
        }
    }

    private fun migrateLegacyAlarms(context: Context) {
        val prefs = preferences(context)
        val raw = prefs.getString("scheduled_notifications", null) ?: return
        try {
            val array = JSONArray(raw)
            for (index in 0 until array.length()) {
                val code = array.optJSONObject(index)?.optInt("requestCode") ?: continue
                PendingIntent.getBroadcast(context, code, Intent(context, MedicationNotificationReceiver::class.java),
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
                    context.getSystemService(AlarmManager::class.java).cancel(it)
                    it.cancel()
                }
            }
        } catch (e: Exception) {
            Log.w("MedicationReminders", "Unable to migrate old alarms", e)
        } finally {
            prefs.edit().remove("scheduled_notifications").apply()
        }
    }
}

private fun medicationKey(prescriptionId: String, itemId: Long) = "${prescriptionId.trim().lowercase(java.util.Locale.ROOT)}:$itemId"

private fun List<PrescricaoMedicamentoResponse>.toReminderPlans(): List<MedicationReminderPlan> =
    filter { it.ativo != false }.flatMap { prescription ->
        (prescription.itens ?: prescription.medicacoes).orEmpty().mapNotNull { item ->
            val id = item.itemId ?: item.id ?: return@mapNotNull null
            val prescriptionId = prescription.id?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val name = item.nomeMedicamento?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val interval = item.intervaloValor?.takeIf { it > 0 } ?: return@mapNotNull null
            if (item.ativo == false) return@mapNotNull null
            MedicationReminderPlan(medicationKey(prescriptionId, id), name, interval,
                item.intervaloTipo.orEmpty(), prescription.dataInicio.orEmpty(), prescription.dataFim.orEmpty())
        }
    }.distinctBy { it.key }

private data class MedicationReminderState(
    val patientId: Long? = null,
    val validatedAt: Long = 0,
    val plans: List<MedicationReminderPlan> = emptyList(),
    val anchors: Map<String, MedicationAnchor> = emptyMap(),
    val events: List<MedicationReminderEvent> = emptyList()
)

private data class MedicationReminderPlan(
    val key: String, val name: String, val intervalValue: Int, val intervalType: String,
    val startDate: String, val endDate: String
)

internal data class MedicationReminderEvent(val key: String, val itemKey: String, val at: Long, val name: String)
