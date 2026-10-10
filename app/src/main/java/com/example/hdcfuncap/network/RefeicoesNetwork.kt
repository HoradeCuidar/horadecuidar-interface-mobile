package com.example.hdcfuncap.network

import java.math.BigDecimal
import java.util.Locale

data class RefeicoesDiaResponse(
    val data: String?,
    val ocorrencias: List<OcorrenciaRefeicaoResponse>?
)

data class OcorrenciaRefeicaoResponse(
    val id: Long?,
    val prescricaoId: Long?,
    val status: String?,
    val refeicao: RefeicaoResponse?,
    val opcao: OpcaoRefeicaoResponse?,
    val dataPrevista: String?,
    val ordemNoDia: Int?,
    val dataHoraRegistro: String?,
    val observacao: String?
)

data class RefeicaoResponse(
    val id: Long?,
    val nome: String?,
    val ordem: Int?,
    val observacoes: String?,
    val opcoes: List<OpcaoRefeicaoResponse>?
)

data class OpcaoRefeicaoResponse(
    val id: Long?,
    val ordem: Int?,
    val descricao: String?,
    val alimentos: List<AlimentoPrescritoResponse>?
)

data class AlimentoPrescritoResponse(
    val id: Long?,
    val descricao: String?,
    val quantidade: BigDecimal?,
    val unidade: String?,
    val observacao: String?
)

data class RegistroRefeicaoRequest(
    val ocorrenciaId: Long,
    val opcaoId: Long,
    val observacao: String?
)

fun AlimentoPrescritoResponse.resumoAlimento(): String {
    val valor = quantidade?.stripTrailingZeros()?.toPlainString()?.replace('.', ',')
    val medida = when (unidade?.trim()?.uppercase(Locale.ROOT)) {
        "G" -> "g"
        "KG" -> "kg"
        "ML" -> "ml"
        "L" -> "L"
        "UNIDADE" -> "unidade"
        "FATIA" -> "fatia"
        "COLHER" -> "colher"
        "XICARA" -> "xícara"
        else -> unidade?.takeIf { it.isNotBlank() }
    }
    val quantidadeFormatada = listOfNotNull(valor, medida).joinToString(" ")
    return listOfNotNull(
        descricao?.takeIf { it.isNotBlank() },
        quantidadeFormatada.takeIf { it.isNotBlank() }
    ).joinToString(" · ")
}

data class RegistroRefeicaoResponse(
    val id: Long?,
    val opcaoId: Long?,
    val status: String?,
    val observacao: String?,
    val dataHoraRegistro: String?
)

fun OcorrenciaRefeicaoResponse.opcoesRegistraveis(): List<OpcaoRefeicaoResponse> =
    refeicao?.opcoes.orEmpty().filter { (it.id ?: 0L) > 0L }.sortedBy { it.ordem ?: Int.MAX_VALUE }

fun OcorrenciaRefeicaoResponse.podeRegistrar(hoje: String): Boolean =
    (id ?: 0L) > 0L && dataPrevista == hoje && opcoesRegistraveis().isNotEmpty() &&
        statusRefeicao in setOf(StatusRefeicao.PENDENTE, StatusRefeicao.NAO_REALIZADO, StatusRefeicao.REALIZADO)

fun OcorrenciaRefeicaoResponse.comRegistro(
    resposta: RegistroRefeicaoResponse,
    opcaoId: Long
): OcorrenciaRefeicaoResponse {
    require(resposta.id == id && resposta.opcaoId == opcaoId &&
        resposta.status?.trim()?.uppercase(Locale.ROOT) == "REALIZADO")
    val opcaoRegistrada = opcoesRegistraveis().first { it.id == opcaoId }
    return copy(
        status = resposta.status,
        opcao = opcaoRegistrada,
        observacao = resposta.observacao,
        dataHoraRegistro = resposta.dataHoraRegistro
    )
}

enum class StatusRefeicao {
    PENDENTE, REALIZADO, NAO_REALIZADO, CANCELADO, DESCONHECIDO
}

val OcorrenciaRefeicaoResponse.statusRefeicao: StatusRefeicao
    get() = when (status.orEmpty().trim().uppercase(Locale.ROOT)) {
        "PENDENTE" -> StatusRefeicao.PENDENTE
        "REALIZADO" -> StatusRefeicao.REALIZADO
        "NAO_REALIZADO" -> StatusRefeicao.NAO_REALIZADO
        "CANCELADO" -> StatusRefeicao.CANCELADO
        else -> StatusRefeicao.DESCONHECIDO
    }

fun RefeicoesDiaResponse.refeicoesDisponiveis(): List<OcorrenciaRefeicaoResponse> =
    ocorrencias.orEmpty()
        .filter { it.statusRefeicao != StatusRefeicao.CANCELADO && it.refeicao != null }
        .sortedBy { it.ordemNoDia ?: it.refeicao?.ordem ?: Int.MAX_VALUE }
