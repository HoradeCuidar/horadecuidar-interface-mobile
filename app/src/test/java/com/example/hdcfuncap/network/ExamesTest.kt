package com.example.hdcfuncap.network

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class ExamesTest {
    private val now = Instant.parse("2026-10-13T12:00:00Z")

    @Test fun publicationIsDeterminedByServerStatus() {
        assertEquals(SituacaoExame.PUBLICADO, ExameResponse(status = " publicado ").situacao(now))
        for (status in listOf("RASCUNHO", "INATIVADO", "UNKNOWN", null)) {
            assertEquals(SituacaoExame.INDISPONIVEL, ExameResponse(status = status).situacao(now))
        }
    }

    @Test fun scheduledPdfIsNotAvailableBeforeRelease() {
        val exame = ExameResponse(status = "AGENDADO", disponibilizacaoEm = "2026-10-14T12:00:00Z")
        assertEquals(SituacaoExame.AGENDADO, exame.situacao(now))
        assertEquals(SituacaoExame.PUBLICADO, exame.situacao(now.plusSeconds(86400)))
    }

    @Test fun malformedReleaseDateDoesNotUnlockAnExam() {
        for (date in listOf(null, "", "invalid", "2026-10-14")) {
            assertEquals(SituacaoExame.AGENDADO, ExameResponse(status = "AGENDADO", disponibilizacaoEm = date).situacao(now))
        }
    }

    @Test fun onlyValidExamsForCurrentPatientAreListed() {
        val first = ExameResponse(id = 1, pacienteId = 9, status = "PUBLICADO", dataColeta = "2026-10-01")
        val second = first.copy(id = 2, dataColeta = "2026-10-02")
        val input = listOf(first, first, second, first.copy(id = 3, pacienteId = 10),
            first.copy(id = 4, status = "INATIVADO"), first.copy(id = null), first.copy(id = -1),
            first.copy(id = 5, pacienteId = null))
        assertEquals(listOf(2L, 1L), input.examesDoPaciente(9).map { it.id })
    }

    @Test fun filenameCannotEscapeTheChosenDirectory() {
        val exame = ExameResponse(id = 1, nomeArquivo = "C:\\pasta\\..\\Hemograma_completo.PdF")
        assertEquals("Hemograma_completo.pdf", exame.nomePdf())
        assertEquals("Hemograma completo", exame.titulo)
        assertEquals("exame-1.pdf", exame.copy(nomeArquivo = "../...").nomePdf())
        assertFalse(exame.copy(nomeArquivo = "nome\r\n?.pdf").nomePdf().contains('\n'))
        assertTrue(exame.copy(nomeArquivo = "a".repeat(300)).nomePdf().length <= 124)
    }

    @Test fun datesAreFormattedInDeviceTimezoneIncludingFractionalInstants() {
        val zone = ZoneId.of("America/Sao_Paulo")
        assertEquals("13/10/2026 às 09:00", formatarDisponibilidadeExame("2026-10-13T12:00:00.123456789Z", zone))
        assertEquals("12/10/2026 às 22:00", formatarDisponibilidadeExame("2026-10-13T01:00:00Z", zone))
        assertEquals("13/10/2026", formatarColetaExame("2026-10-13"))
        assertEquals("Data não informada", formatarColetaExame("2026-02-30"))
        assertNull(formatarDisponibilidadeExame("invalid", zone))
    }

    @Test fun missingFilenameHasStableFallback() {
        assertEquals("exame-42.pdf", ExameResponse(id = 42).nomePdf())
    }
}
