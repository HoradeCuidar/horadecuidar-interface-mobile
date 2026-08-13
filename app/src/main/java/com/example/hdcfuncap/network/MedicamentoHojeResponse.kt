package com.example.hdcfuncap.network

data class MedicamentoHojeResponse(
    val itemId: Long,
    val prescricaoId: String,
    val nomeMedicamento: String,
    val dosagemFormatada: String,
    val frequencia: String,
    val viaAdministracao: String?,
    val observacao: String?,
    val statusAdesaoHoje: String?,
    val adesaoId: Long?,
    val dosesEsperadasHoje: Int,
    val dosesRegistradasHoje: Int,
    val ocorrenciaId: Long? = null,
    val ordemNoDia: Int? = null,
    val dataPrevista: String? = null,
    val quantidadeDiaria: Int? = null
)

fun MedicamentosDiaResponse.toMedicamentosHoje(): List<MedicamentoHojeResponse> {
    return ocorrencias.orEmpty().mapNotNull { ocorrencia ->
        val item = ocorrencia.itemMedicacao ?: return@mapNotNull null
        val itemId = item.id ?: return@mapNotNull null
        val ocorrenciaId = ocorrencia.id ?: return@mapNotNull null
        val quantidadeDoses = item.quantidadeDoses ?: 1
        val foiRegistrado = ocorrencia.dataHoraRegistro != null

        MedicamentoHojeResponse(
            itemId = itemId,
            prescricaoId = ocorrencia.prescricaoId.orEmpty(),
            nomeMedicamento = item.nomeMedicamento.orEmpty(),
            dosagemFormatada = formatarDosagem(item),
            frequencia = formatarFrequencia(item),
            viaAdministracao = formatarViaAdministracao(item.viaAdministracao),
            observacao = ocorrencia.observacao ?: item.observacao,
            statusAdesaoHoje = if (foiRegistrado) "REALIZADO" else null,
            adesaoId = ocorrenciaId,
            dosesEsperadasHoje = quantidadeDoses,
            dosesRegistradasHoje = if (foiRegistrado) 1 else 0,
            ocorrenciaId = ocorrenciaId,
            ordemNoDia = ocorrencia.ordemNoDia,
            dataPrevista = ocorrencia.dataPrevista,
            quantidadeDiaria = quantidadeDoses
        )
    }
}

private fun formatarDosagem(item: ItemMedicacaoOcorrenciaResponse): String {
    val valor = item.dosagemValor ?: return ""
    val unidade = item.dosagemUnidade?.lowercase().orEmpty()
    val quantidade = item.quantidadeDoses?.let { " - $it comprimido${if (it > 1) "s" else ""}" }.orEmpty()
    return "${formatarNumero(valor)}$unidade$quantidade"
}

private fun formatarFrequencia(item: ItemMedicacaoOcorrenciaResponse): String {
    val intervaloValor = item.intervaloValor ?: return ""
    val intervaloTipo = item.intervaloTipo?.lowercase().orEmpty()
    val periodo = when (intervaloTipo) {
        "hora", "horas" -> "hora${if (intervaloValor > 1) "s" else ""}"
        "dia", "dias" -> "dia${if (intervaloValor > 1) "s" else ""}"
        "semana", "semanas" -> "semana${if (intervaloValor > 1) "s" else ""}"
        else -> intervaloTipo
    }
    return if (periodo.isBlank()) "" else "A cada $intervaloValor $periodo"
}

private fun formatarViaAdministracao(via: String?): String? {
    return when (via) {
        null -> null
        "ORAL" -> "Via oral"
        "SUBLINGUAL" -> "Via sublingual"
        "INJETAVEL" -> "Via injetavel"
        "TOPICA" -> "Via topica"
        "INALATORIA" -> "Via inalatoria"
        else -> via.lowercase().replaceFirstChar { it.uppercase() }
    }
}

private fun formatarNumero(valor: Double): String {
    return if (valor % 1.0 == 0.0) valor.toInt().toString() else valor.toString()
}
