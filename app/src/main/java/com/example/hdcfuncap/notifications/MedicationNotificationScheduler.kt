package com.example.hdcfuncap.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.hdcfuncap.network.AuthApi
import com.example.hdcfuncap.network.OcorrenciaMedicamentoResponse
import com.example.hdcfuncap.network.PrescricaoMedicamentoResponse
import com.example.hdcfuncap.network.apenasPrescricoesMedicamentosAtivas
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

object MedicationNotificationScheduler {
    private const val PREFS_NAME = "hdc_medication_notifications"
    private const val KEY_SCHEDULED = "scheduled_notifications"
    private const val DAYS_TO_SCHEDULE = 3
    private const val START_HOUR = 8
    private const val END_HOUR = 20

    suspend fun refreshMedicationNotifications(
        context: Context,
        authApi: AuthApi,
        pacienteId: Long
    ) {
        val appContext = context.applicationContext
        val notifications = mutableListOf<ScheduledMedicationNotification>()
        val prescricoes = authApi.getPrescricoesMedicamentos(pacienteId)

        repeat(DAYS_TO_SCHEDULE) { offset ->
            val data = isoDateFromToday(offset)
            val prescricoesDoDia = prescricoes.apenasPrescricoesMedicamentosAtivas(data)
            val ocorrencias = authApi
                .getMedicamentosHoje(pacienteId = pacienteId, data = data)
                .ocorrencias
                .orEmpty()
                .filter { ocorrencia -> ocorrencia.pertenceAsPrescricoes(prescricoesDoDia) }

            notifications += ocorrencias.mapNotNull { ocorrencia ->
                ocorrencia.toScheduledNotification()
            }
        }

        val futureNotifications = notifications
            .filter { it.triggerAtMillis > System.currentTimeMillis() }
            .distinctBy { it.requestCode }

        replaceScheduledNotifications(appContext, futureNotifications)
    }

    fun rescheduleSavedNotifications(context: Context) {
        val appContext = context.applicationContext
        val notifications = readSavedNotifications(appContext)
            .filter { it.triggerAtMillis > System.currentTimeMillis() }

        notifications.forEach { schedule(appContext, it) }
        saveNotifications(appContext, notifications)
    }

    private fun replaceScheduledNotifications(
        context: Context,
        notifications: List<ScheduledMedicationNotification>
    ) {
        readSavedNotifications(context).forEach { cancel(context, it) }
        notifications.forEach { schedule(context, it) }
        saveNotifications(context, notifications)
    }

    private fun OcorrenciaMedicamentoResponse.toScheduledNotification(): ScheduledMedicationNotification? {
        val occurrenceId = id ?: return null
        val item = itemMedicacao ?: return null
        val name = item.nomeMedicamento.orEmpty().ifBlank { return null }
        val date = dataPrevista.orEmpty().ifBlank { return null }
        val statusAtual = status.normalizedApiValue()
        val isPending = statusAtual.isBlank() || statusAtual == "PENDENTE"

        if (!isPending || item.ativo == false) return null

        val triggerAtMillis = calculateTriggerMillis(
            date = date,
            doseOrder = ordemNoDia ?: 1,
            totalDoses = item.quantidadeDoses ?: 1,
            intervalValue = item.intervaloValor,
            intervalType = item.intervaloTipo
        ) ?: return null

        val dosage = formatDosage(item.dosagemValor, item.dosagemUnidade)
        val message = buildNotificationMessage(name = name, dosage = dosage)
        val requestCode = "medication-$occurrenceId-$date".hashCode()

        return ScheduledMedicationNotification(
            requestCode = requestCode,
            triggerAtMillis = triggerAtMillis,
            title = "Hora do medicamento",
            message = message
        )
    }

    private fun buildNotificationMessage(name: String, dosage: String): String {
        return buildString {
            append("Está na hora de tomar ")
            append(name)
            if (dosage.isNotBlank()) append(" ($dosage)")
            append(".")
        }
    }

    private fun OcorrenciaMedicamentoResponse.pertenceAsPrescricoes(
        prescricoes: List<PrescricaoMedicamentoResponse>
    ): Boolean {
        val prescricaoId = prescricaoId.normalizedId()
        val itemId = itemMedicacao?.id ?: return false

        return prescricoes.any { prescricao ->
            prescricao.id.normalizedId() == prescricaoId &&
                (prescricao.itens ?: prescricao.medicacoes).orEmpty().any { item ->
                    (item.itemId ?: item.id) == itemId && item.ativo != false
                }
        }
    }

    private fun calculateTriggerMillis(
        date: String,
        doseOrder: Int,
        totalDoses: Int,
        intervalValue: Int?,
        intervalType: String?
    ): Long? {
        val calendar = calendarFromIsoDate(date) ?: return null
        val safeOrder = doseOrder.coerceAtLeast(1)
        val safeTotalDoses = totalDoses.coerceAtLeast(safeOrder).coerceAtLeast(1)
        val intervalHours = intervalValue?.takeIf { it > 0 }

        val normalizedIntervalType = intervalType.normalizedApiValue()

        val minutesFromStart = if (
            (normalizedIntervalType == "HORA" || normalizedIntervalType == "HORAS") &&
            intervalHours != null
        ) {
            val calculated = (safeOrder - 1) * intervalHours * 60
            val maxSameDayMinutes = ((23 - START_HOUR) * 60) + 59
            if (calculated <= maxSameDayMinutes) {
                calculated
            } else {
                distributedMinutes(safeOrder, safeTotalDoses)
            }
        } else {
            distributedMinutes(safeOrder, safeTotalDoses)
        }

        calendar.set(Calendar.HOUR_OF_DAY, START_HOUR)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.MINUTE, minutesFromStart)

        return calendar.timeInMillis
    }

    private fun distributedMinutes(doseOrder: Int, totalDoses: Int): Int {
        if (totalDoses <= 1) return 0

        val availableMinutes = (END_HOUR - START_HOUR) * 60
        return ((doseOrder - 1) * availableMinutes) / (totalDoses - 1)
    }

    private fun schedule(context: Context, notification: ScheduledMedicationNotification) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = pendingIntentFor(context, notification)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    notification.triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    notification.triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: Exception) {
            // Alguns aparelhos restringem alarmes; falhar aqui não pode afetar a UI.
        }
    }

    private fun cancel(context: Context, notification: ScheduledMedicationNotification) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntentFor(context, notification))
        } catch (_: Exception) {
            // Cancelamento é uma limpeza defensiva; não deve quebrar o app.
        }
    }

    private fun pendingIntentFor(
        context: Context,
        notification: ScheduledMedicationNotification
    ): PendingIntent {
        val intent = Intent(context, MedicationNotificationReceiver::class.java).apply {
            putExtra(MedicationNotificationReceiver.EXTRA_TITLE, notification.title)
            putExtra(MedicationNotificationReceiver.EXTRA_MESSAGE, notification.message)
            putExtra(MedicationNotificationReceiver.EXTRA_NOTIFICATION_ID, notification.requestCode)
        }

        return PendingIntent.getBroadcast(
            context,
            notification.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun readSavedNotifications(context: Context): List<ScheduledMedicationNotification> {
        val raw = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SCHEDULED, null)
            .orEmpty()

        if (raw.isBlank()) return emptyList()

        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val requestCode = item.optInt("requestCode")
                    val triggerAtMillis = item.optLong("triggerAtMillis", 0L)
                    val title = item.optString("title", "Hora do medicamento")
                    val message = item.optString("message")

                    if (triggerAtMillis > 0L && message.isNotBlank()) {
                        add(
                            ScheduledMedicationNotification(
                                requestCode = requestCode,
                                triggerAtMillis = triggerAtMillis,
                                title = title,
                                message = message
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveNotifications(
        context: Context,
        notifications: List<ScheduledMedicationNotification>
    ) {
        val array = JSONArray()
        notifications.forEach { notification ->
            array.put(
                JSONObject()
                    .put("requestCode", notification.requestCode)
                    .put("triggerAtMillis", notification.triggerAtMillis)
                    .put("title", notification.title)
                    .put("message", notification.message)
            )
        }

        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SCHEDULED, array.toString())
            .apply()
    }

    private fun isoDateFromToday(offsetDays: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, offsetDays)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }

    private fun calendarFromIsoDate(date: String): Calendar? {
        val parts = date.split("-")
        if (parts.size != 3) return null

        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null

        return try {
            Calendar.getInstance().apply {
                isLenient = false
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                timeInMillis
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun String?.normalizedApiValue(): String {
        return orEmpty().trim().uppercase(Locale.US)
    }

    private fun String?.normalizedId(): String {
        return orEmpty().trim().lowercase(Locale.US)
    }

    private fun formatDosage(value: Double?, unit: String?): String {
        if (value == null || unit.isNullOrBlank()) return ""

        val formattedValue = if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            value.toString()
        }

        return "$formattedValue ${unit.lowercase(Locale.getDefault())}"
    }
}

private data class ScheduledMedicationNotification(
    val requestCode: Int,
    val triggerAtMillis: Long,
    val title: String,
    val message: String
)
