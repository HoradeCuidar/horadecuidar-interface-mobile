package com.example.hdcfuncap.features

import com.example.hdcfuncap.network.MedicamentoHojeResponse
import com.example.hdcfuncap.network.OcorrenciaRefeicaoResponse
import com.example.hdcfuncap.network.StatusRefeicao
import com.example.hdcfuncap.network.statusRefeicao
import java.util.Locale

internal data class ProgressoDiario(val totalTarefas: Int, val tarefasFeitas: Int) {
    val fracao: Float
        get() = if (totalTarefas > 0) tarefasFeitas.toFloat() / totalTarefas else 0f
}

internal fun calcularProgressoDiario(
    medicamentos: List<MedicamentoHojeResponse>,
    refeicoes: List<OcorrenciaRefeicaoResponse>
): ProgressoDiario {
    var total = 0
    var feitas = 0
    medicamentos.forEach { medicamento ->
        val status = medicamento.statusAdesaoHoje?.trim()?.uppercase(Locale.ROOT)
        if (status != "CANCELADO") {
            total++
            if (status == "REALIZADO") feitas++
        }
    }
    refeicoes.forEach { ocorrencia ->
        val status = ocorrencia.statusRefeicao
        if (ocorrencia.refeicao != null && status != StatusRefeicao.CANCELADO) {
            total++
            if (status == StatusRefeicao.REALIZADO) feitas++
        }
    }
    return ProgressoDiario(totalTarefas = total, tarefasFeitas = feitas)
}
