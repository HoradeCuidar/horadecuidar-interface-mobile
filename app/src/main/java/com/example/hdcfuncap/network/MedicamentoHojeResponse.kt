package com.example.hdcfuncap.network

import java.util.Locale

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
        if (item.ativo == false) return@mapNotNull null

        val quantidadeDoses = item.quantidadeDoses ?: 1
        val status = ocorrencia.status?.normalizedApiValue() ?: if (ocorrencia.dataHoraRegistro != null) {
            "REALIZADO"
        } else {
            "PENDENTE"
        }
        if (status == "CANCELADO") return@mapNotNull null

        val foiRealizado = status == "REALIZADO"

        MedicamentoHojeResponse(
            itemId = itemId,
            prescricaoId = ocorrencia.prescricaoId.orEmpty(),
            nomeMedicamento = item.nomeMedicamento.orEmpty(),
            dosagemFormatada = formatarDosagem(item),
            frequencia = formatarFrequencia(item),
            viaAdministracao = formatarViaAdministracao(item.viaAdministracao),
            observacao = ocorrencia.observacao ?: item.observacao,
            statusAdesaoHoje = status,
            adesaoId = ocorrenciaId,
            dosesEsperadasHoje = quantidadeDoses,
            dosesRegistradasHoje = if (foiRealizado) 1 else 0,
            ocorrenciaId = ocorrenciaId,
            ordemNoDia = ocorrencia.ordemNoDia,
            dataPrevista = ocorrencia.dataPrevista,
            quantidadeDiaria = quantidadeDoses
        )
    }
}

fun List<PrescricaoMedicamentoResponse>.toMedicamentosHojeFallback(data: String): List<MedicamentoHojeResponse> {
    return asSequence()
        .filter { prescricao -> prescricao.estaDisponivelNaData(data) }
        .flatMap { prescricao ->
            prescricao.itensAtivos().asSequence().mapNotNull { item ->
                val itemId = item.itemId ?: item.id ?: return@mapNotNull null
                val nome = item.nomeMedicamento.orEmpty().trim().ifBlank { return@mapNotNull null }
                val quantidadeDoses = item.quantidadeDoses?.takeIf { it > 0 } ?: 1

                MedicamentoHojeResponse(
                    itemId = itemId,
                    prescricaoId = prescricao.id.orEmpty(),
                    nomeMedicamento = nome,
                    dosagemFormatada = item.dosagemFormatada
                        .orEmpty()
                        .ifBlank { formatarDosagem(item) },
                    frequencia = item.frequencia
                        .orEmpty()
                        .ifBlank { formatarFrequencia(item) },
                    viaAdministracao = formatarViaAdministracao(item.viaAdministracao),
                    observacao = null,
                    statusAdesaoHoje = "PENDENTE",
                    adesaoId = null,
                    dosesEsperadasHoje = quantidadeDoses,
                    dosesRegistradasHoje = 0,
                    ocorrenciaId = null,
                    ordemNoDia = null,
                    dataPrevista = data,
                    quantidadeDiaria = quantidadeDoses
                )
            }
        }
        .distinctBy { "${it.prescricaoId}-${it.itemId}" }
        .toList()
}

fun List<PrescricaoMedicamentoResponse>.apenasPrescricoesMedicamentosAtivas(
    data: String? = null
): List<PrescricaoMedicamentoResponse> {
    return asSequence()
        .filter { prescricao ->
            prescricao.ativo != false && (data == null || prescricao.estaDisponivelNaData(data))
        }
        .map { prescricao ->
            val itensAtivos = prescricao.itensAtivos()
            prescricao.copy(
                itens = if (prescricao.itens != null) itensAtivos else null,
                medicacoes = if (prescricao.itens == null) itensAtivos else prescricao.medicacoes?.filter { it.ativo != false }
            )
        }
        .filter { prescricao -> prescricao.itensPrescricao().isNotEmpty() }
        .toList()
}

fun List<MedicamentoHojeResponse>.apenasDasPrescricoes(
    prescricoes: List<PrescricaoMedicamentoResponse>
): List<MedicamentoHojeResponse> {
    val chavesAtivas = prescricoes.asSequence()
        .filter { it.ativo != false }
        .flatMap { prescricao ->
            val prescricaoId = prescricao.id.normalizedId()
            prescricao.itensAtivos().asSequence().mapNotNull { item ->
                val itemId = item.itemId ?: item.id ?: return@mapNotNull null
                "$prescricaoId-$itemId"
            }
        }
        .toSet()

    if (chavesAtivas.isEmpty()) return emptyList()

    return filter { medicamento ->
        "${medicamento.prescricaoId.normalizedId()}-${medicamento.itemId}" in chavesAtivas
    }
}

private fun formatarDosagem(item: ItemMedicacaoOcorrenciaResponse): String {
    val valor = item.dosagemValor ?: return ""
    val unidade = item.dosagemUnidade?.lowercase().orEmpty()
    val quantidade = item.quantidadeDoses?.let { " - $it comprimido${if (it > 1) "s" else ""}" }.orEmpty()
    return "${formatarNumero(valor)}$unidade$quantidade"
}

private fun formatarDosagem(item: ItemMedicacaoResponse): String {
    val valor = item.dosagemValor ?: return ""
    val unidade = item.dosagemUnidade?.lowercase(Locale.getDefault()).orEmpty()
    val quantidade = item.quantidadeDoses?.let { " - $it dose${if (it > 1) "s" else ""}" }.orEmpty()
    return "${formatarNumero(valor)} $unidade$quantidade".trim()
}

private fun formatarFrequencia(item: ItemMedicacaoOcorrenciaResponse): String {
    val intervaloValor = item.intervaloValor ?: return ""
    val intervaloTipo = item.intervaloTipo?.normalizedApiValue()?.lowercase(Locale.getDefault()).orEmpty()
    val periodo = when (intervaloTipo) {
        "hora", "horas" -> "hora${if (intervaloValor > 1) "s" else ""}"
        "dia", "dias" -> "dia${if (intervaloValor > 1) "s" else ""}"
        "semana", "semanas" -> "semana${if (intervaloValor > 1) "s" else ""}"
        "mes", "meses" -> "mês${if (intervaloValor > 1) "es" else ""}"
        else -> intervaloTipo
    }
    return if (periodo.isBlank()) "" else "A cada $intervaloValor $periodo"
}

private fun formatarFrequencia(item: ItemMedicacaoResponse): String {
    val doses = item.quantidadeDoses ?: return ""
    val intervalo = item.intervaloValor ?: 1
    val tipo = item.intervaloTipo?.normalizedApiValue() ?: return ""
    val vez = if (doses == 1) "vez" else "vezes"

    val unidadeTempo = when (tipo) {
        "HORA" -> if (intervalo == 1) "hora" else "horas"
        "DIA" -> if (intervalo == 1) "dia" else "dias"
        "SEMANA" -> if (intervalo == 1) "semana" else "semanas"
        "MES" -> if (intervalo == 1) "mês" else "meses"
        else -> tipo.lowercase(Locale.getDefault())
    }

    return if (intervalo == 1 && tipo != "HORA") {
        val preposicao = if (tipo == "DIA") "ao" else "por"
        "$doses $vez $preposicao $unidadeTempo"
    } else {
        "$doses $vez a cada $intervalo $unidadeTempo"
    }
}

private fun formatarViaAdministracao(via: String?): String? {
    return when (via?.normalizedApiValue()) {
        null -> null
        "ORAL" -> "Via oral"
        "SUBLINGUAL" -> "Via sublingual"
        "INJETAVEL" -> "Via injetável"
        "TOPICA" -> "Via tópica"
        "INALATORIA" -> "Via inalatória"
        else -> via.trim().lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase() }
    }
}

private fun formatarNumero(valor: Double): String {
    return if (valor % 1.0 == 0.0) valor.toInt().toString() else valor.toString()
}

private fun String.normalizedApiValue(): String {
    return trim().uppercase(Locale.US)
}

private fun PrescricaoMedicamentoResponse.itensPrescricao(): List<ItemMedicacaoResponse> {
    return itens ?: medicacoes ?: emptyList()
}

private fun PrescricaoMedicamentoResponse.itensAtivos(): List<ItemMedicacaoResponse> {
    return itensPrescricao().filter { item -> item.ativo != false }
}

private fun PrescricaoMedicamentoResponse.estaDisponivelNaData(data: String): Boolean {
    if (ativo == false) return false

    val dataReferencia = data.substringBefore("T").substringBefore(" ")
    val inicio = dataInicio?.substringBefore("T")?.substringBefore(" ")
    val fim = dataFim?.substringBefore("T")?.substringBefore(" ")

    if (dataReferencia.length != 10) return true
    if (!inicio.isNullOrBlank() && inicio.length == 10 && dataReferencia < inicio) return false
    if (!fim.isNullOrBlank() && fim.length == 10 && dataReferencia > fim) return false

    return true
}

private fun String?.normalizedId(): String {
    return orEmpty().trim().lowercase(Locale.US)
}
