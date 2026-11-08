package com.example.hdcfuncap.notifications

internal data class MedicationAnchor(val occurrenceId: Long, val at: Long)

internal fun updatedMedicationAnchors(
    anchors: Map<String, MedicationAnchor>,
    key: String,
    occurrenceId: Long,
    status: String,
    recordedAt: Long
): Map<String, MedicationAnchor> = anchors.toMutableMap().apply {
    when (status) {
        "REALIZADO" -> if (recordedAt > 0) putIfAbsent(key, MedicationAnchor(occurrenceId, recordedAt))
        "NAO_REALIZADO", "PENDENTE" -> if (get(key)?.occurrenceId == occurrenceId) remove(key)
    }
}
