package com.example.hdcfuncap.features

import com.example.hdcfuncap.network.MedicamentoHojeResponse
import com.example.hdcfuncap.network.OcorrenciaRefeicaoResponse
import com.example.hdcfuncap.network.RefeicoesDiaResponse
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressoDiarioTest {
    @Test
    fun combinesMedicationAndMealTasks() {
        val resumo = calcularProgressoDiario(
            listOf(medicamento("REALIZADO"), medicamento("NAO_REALIZADO")),
            refeicoes("""
                {"status":"REALIZADO","refeicao":{"nome":"Cafe"}},
                {"status":"PENDENTE","refeicao":{"nome":"Almoco"}},
                {"status":"NAO_REALIZADO","refeicao":{"nome":"Jantar"}}
            """)
        )
        assertEquals(5, resumo.totalTarefas)
        assertEquals(2, resumo.tarefasFeitas)
        assertEquals(0.4f, resumo.fracao, 0.0001f)
    }

    @Test
    fun countsMealOnceRegardlessOfOptionsOrFoods() {
        val resumo = calcularProgressoDiario(emptyList(), refeicoes("""
            {"status":"REALIZADO","refeicao":{"opcoes":[
                {"id":1,"alimentos":[{"descricao":"Pao"},{"descricao":"Fruta"}]},
                {"id":2,"alimentos":[{"descricao":"Iogurte"}]}
            ]},"opcao":{"id":1}}
        """))
        assertEquals(ProgressoDiario(1, 1), resumo)
        assertEquals(1f, resumo.fracao, 0f)
    }

    @Test
    fun excludesCancelledTasksAndMealsWithoutDetails() {
        val resumo = calcularProgressoDiario(
            listOf(medicamento("CANCELADO"), medicamento("REALIZADO")),
            refeicoes("""
                {"status":"CANCELADO","refeicao":{"nome":"Cafe"}},
                {"status":"REALIZADO","refeicao":null},
                {"status":"PENDENTE","refeicao":{"nome":"Almoco"}}
            """)
        )
        assertEquals(ProgressoDiario(2, 1), resumo)
    }

    @Test
    fun absentAndUnknownStatusesDoNotCountAsCompleted() {
        val resumo = calcularProgressoDiario(
            listOf(medicamento(null)),
            refeicoes("""
                {"refeicao":{"nome":"Cafe"}},
                {"status":"NOVO_STATUS","refeicao":{"nome":"Almoco"}}
            """)
        )
        assertEquals(ProgressoDiario(3, 0), resumo)
        assertEquals(0f, resumo.fracao, 0f)
    }

    @Test
    fun handlesEmptyDayWithoutDivisionByZero() {
        val resumo = calcularProgressoDiario(emptyList(), emptyList())
        assertEquals(ProgressoDiario(0, 0), resumo)
        assertEquals(0f, resumo.fracao, 0f)
    }

    @Test
    fun preservesMedicationOnlyProgressAndNormalizesStatuses() {
        val resumo = calcularProgressoDiario(
            listOf(medicamento(" realizado "), medicamento("PENDENTE")),
            emptyList()
        )
        assertEquals(ProgressoDiario(2, 1), resumo)
        assertEquals(0.5f, resumo.fracao, 0f)
        val refeicoesRealizadas = calcularProgressoDiario(emptyList(), refeicoes("""
            {"status":" realizado ","refeicao":{"nome":"Cafe"}}
        """))
        assertEquals(ProgressoDiario(1, 1), refeicoesRealizadas)
    }

    private fun refeicoes(json: String): List<OcorrenciaRefeicaoResponse> =
        Gson().fromJson("""{"ocorrencias":[$json]}""", RefeicoesDiaResponse::class.java)
            .ocorrencias.orEmpty()

    private fun medicamento(status: String?) = MedicamentoHojeResponse(
        itemId = 1L,
        prescricaoId = "1",
        nomeMedicamento = "Medicamento de teste",
        dosagemFormatada = "1 comprimido",
        frequencia = "1 vez ao dia",
        viaAdministracao = null,
        observacao = null,
        statusAdesaoHoje = status,
        adesaoId = null,
        dosesEsperadasHoje = 1,
        dosesRegistradasHoje = if (status == "REALIZADO") 1 else 0
    )
}
