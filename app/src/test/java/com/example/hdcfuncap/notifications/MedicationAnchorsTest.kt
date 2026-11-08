package com.example.hdcfuncap.notifications

import org.junit.Assert.*
import org.junit.Test

class MedicationAnchorsTest {
    @Test fun firstConfirmedRecordCreatesAnchor() {
        assertEquals(MedicationAnchor(1, 1000), updatedMedicationAnchors(emptyMap(), "a", 1, "REALIZADO", 1000)["a"])
    }

    @Test fun laterRecordsAndRepeatedClicksDoNotMoveAnchor() {
        val initial = mapOf("a" to MedicationAnchor(1, 1000))
        assertEquals(initial, updatedMedicationAnchors(initial, "a", 2, "REALIZADO", 2000))
        assertEquals(initial, updatedMedicationAnchors(initial, "a", 1, "REALIZADO", 3000))
    }

    @Test fun eachMedicationHasItsOwnClock() {
        val initial = mapOf("a" to MedicationAnchor(1, 1000))
        val actual = updatedMedicationAnchors(initial, "b", 2, "REALIZADO", 2000)
        assertEquals(1000L, actual["a"]!!.at)
        assertEquals(2000L, actual["b"]!!.at)
    }

    @Test fun notTakenDoesNotCreateAnchor() {
        assertTrue(updatedMedicationAnchors(emptyMap(), "a", 1, "NAO_REALIZADO", 1000).isEmpty())
    }

    @Test fun undoInitialRecordRemovesAnchorOnlyForThatMedication() {
        val initial = mapOf("a" to MedicationAnchor(1, 1000), "b" to MedicationAnchor(2, 2000))
        assertEquals(setOf("b"), updatedMedicationAnchors(initial, "a", 1, "NAO_REALIZADO", 3000).keys)
        assertEquals(initial, updatedMedicationAnchors(initial, "a", 3, "NAO_REALIZADO", 3000))
    }

    @Test fun invalidInputsDoNotRemoveAnExistingAnchor() {
        val initial = mapOf("a" to MedicationAnchor(1, 1000))
        assertEquals(initial, updatedMedicationAnchors(initial, "a", 1, "REALIZADO", 0))
        assertEquals(initial, updatedMedicationAnchors(initial, "a", 1, "UNKNOWN", 3000))
    }
}
