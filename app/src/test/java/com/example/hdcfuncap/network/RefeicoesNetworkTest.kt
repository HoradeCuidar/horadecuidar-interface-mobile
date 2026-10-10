package com.example.hdcfuncap.network

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class RefeicoesNetworkTest {
    private val gson = Gson()

    @Test
    fun parsesMealOptionsFoodsAndRecordedChoice() {
        val response = parse("""
            {"data":"2026-10-10","ocorrencias":[{
                "id":12,"prescricaoId":3,"status":"REALIZADO","ordemNoDia":1,
                "refeicao":{"id":4,"nome":"Almoco","opcoes":[{
                    "id":5,"ordem":1,"descricao":"Opcao principal",
                    "alimentos":[{"descricao":"Arroz","quantidade":100.50,"unidade":"G"}]
                }]},
                "opcao":{"id":5,"descricao":"Opcao principal"},
                "dataPrevista":"2026-10-10","dataHoraRegistro":"2026-10-10T12:00:00"
            }]}
        """)
        val occurrence = response.refeicoesDisponiveis().single()
        assertEquals(StatusRefeicao.REALIZADO, occurrence.statusRefeicao)
        assertEquals(5L, occurrence.opcao?.id)
        assertEquals(BigDecimal("100.50"), occurrence.refeicao?.opcoes?.single()?.alimentos?.single()?.quantidade)
    }

    @Test
    fun excludesCancelledAndMissingMealsAndSortsByOrder() {
        val response = parse("""
            {"ocorrencias":[
                {"id":1,"status":"PENDENTE","refeicao":{"ordem":2}},
                {"id":2,"status":"CANCELADO","refeicao":{"ordem":1}},
                {"id":3,"status":"REALIZADO","refeicao":null},
                {"id":4,"status":"NAO_REALIZADO","ordemNoDia":1,"refeicao":{"ordem":3}}
            ]}
        """)
        assertEquals(listOf(4L, 1L), response.refeicoesDisponiveis().map { it.id })
    }

    @Test
    fun handlesAbsentOptionalFieldsAndUnknownStatusesWithoutInventingCompletion() {
        val response = parse("""
            {"ocorrencias":[
                {"status":"PENDENTE","refeicao":{"nome":"Jantar"}},
                {"status":"NOVO_STATUS","refeicao":{"nome":"Lanche"}},
                {"refeicao":{"nome":"Ceia"}}
            ]}
        """)
        val occurrences = response.refeicoesDisponiveis()
        assertEquals(listOf(StatusRefeicao.PENDENTE, StatusRefeicao.DESCONHECIDO, StatusRefeicao.DESCONHECIDO),
            occurrences.map { it.statusRefeicao })
        assertNull(occurrences.first().opcao)
        assertNull(occurrences.first().refeicao?.opcoes)
    }

    @Test
    fun handlesEmptyOrMissingOccurrences() {
        assertTrue(parse("{}").refeicoesDisponiveis().isEmpty())
        assertTrue(parse("""{"ocorrencias":[]}""").refeicoesDisponiveis().isEmpty())
    }

    @Test
    fun allowsRegistrationAndEditingOnlyForValidTodayOccurrences() {
        val occurrence = registrableOccurrence()
        assertTrue(occurrence.podeRegistrar("2026-10-10"))
        assertTrue(occurrence.copy(status = "REALIZADO").podeRegistrar("2026-10-10"))
        assertTrue(occurrence.copy(status = "NAO_REALIZADO").podeRegistrar("2026-10-10"))
        assertFalse(occurrence.copy(status = "CANCELADO").podeRegistrar("2026-10-10"))
        assertFalse(occurrence.copy(status = "DESCONHECIDO").podeRegistrar("2026-10-10"))
        assertFalse(occurrence.podeRegistrar("2026-10-09"))
        assertFalse(occurrence.podeRegistrar("2026-10-11"))
    }

    @Test
    fun blocksMissingOccurrenceIdsDatesAndOptions() {
        val occurrence = registrableOccurrence()
        assertFalse(occurrence.copy(id = null).podeRegistrar("2026-10-10"))
        assertFalse(occurrence.copy(id = 0L).podeRegistrar("2026-10-10"))
        assertFalse(occurrence.copy(dataPrevista = null).podeRegistrar("2026-10-10"))
        assertFalse(occurrence.copy(refeicao = occurrence.refeicao?.copy(opcoes = emptyList()))
            .podeRegistrar("2026-10-10"))
        val invalidOptions = occurrence.refeicao?.opcoes?.map { it.copy(id = null) }
        assertFalse(occurrence.copy(refeicao = occurrence.refeicao?.copy(opcoes = invalidOptions))
            .podeRegistrar("2026-10-10"))
    }

    @Test
    fun serializesRegistrationUsingBackendFieldNames() {
        val body = gson.toJsonTree(RegistroRefeicaoRequest(12L, 5L, "Teste")).asJsonObject
        assertEquals(12L, body.get("ocorrenciaId").asLong)
        assertEquals(5L, body.get("opcaoId").asLong)
        assertEquals("Teste", body.get("observacao").asString)
        assertFalse(body.has("status"))
    }

    @Test
    fun appliesConfirmedResponseWithoutLosingMealOptions() {
        val occurrence = registrableOccurrence()
        val response = gson.fromJson("""
            {"id":12,"opcaoId":5,"status":"REALIZADO",
             "observacao":"Teste","dataHoraRegistro":"2026-10-10T12:00:00"}
        """, RegistroRefeicaoResponse::class.java)
        val updated = occurrence.comRegistro(response, 5L)
        assertEquals(StatusRefeicao.REALIZADO, updated.statusRefeicao)
        assertEquals(5L, updated.opcao?.id)
        assertEquals("Teste", updated.observacao)
        assertEquals(occurrence.refeicao, updated.refeicao)
        assertEquals("2026-10-10T12:00:00", updated.dataHoraRegistro)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsConfirmationForAnotherOccurrence() {
        registrableOccurrence().comRegistro(RegistroRefeicaoResponse(99L, 5L, "REALIZADO", null, null), 5L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsConfirmationForAnotherOption() {
        registrableOccurrence().comRegistro(RegistroRefeicaoResponse(12L, 99L, "REALIZADO", null, null), 5L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsIncompleteConfirmation() {
        registrableOccurrence().comRegistro(RegistroRefeicaoResponse(12L, 5L, null, null, null), 5L)
    }

    @Test
    fun formatsFoodQuantitiesConsistentlyAcrossScreens() {
        val food = AlimentoPrescritoResponse(null, "Arroz", BigDecimal("100.50"), "G", null)
        assertEquals("Arroz · 100,5 g", food.resumoAlimento())
        assertEquals("Arroz", food.copy(quantidade = null, unidade = null).resumoAlimento())
    }

    private fun registrableOccurrence() = parse("""
        {"ocorrencias":[{"id":12,"status":"PENDENTE","dataPrevista":"2026-10-10",
         "refeicao":{"nome":"Almoco","opcoes":[{"id":5,"ordem":1}]}}]}
    """).ocorrencias.orEmpty().single()

    private fun parse(json: String): RefeicoesDiaResponse =
        gson.fromJson(json, RefeicoesDiaResponse::class.java)
}
