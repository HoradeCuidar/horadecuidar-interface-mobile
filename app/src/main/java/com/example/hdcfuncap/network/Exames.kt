package com.example.hdcfuncap.network

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

data class ExameResponse(
    val id: Long? = null,
    val pacienteId: Long? = null,
    val profissionalCadastroId: Long? = null,
    val dataColeta: String? = null,
    val laboratorio: String? = null,
    val observacao: String? = null,
    val status: String? = null,
    val disponibilizacaoEm: String? = null,
    val publicadoEm: String? = null,
    val criadoEm: String? = null,
    val atualizadoEm: String? = null,
    val nomeArquivo: String? = null,
    val tamanhoBytes: Long? = null
)

enum class SituacaoExame { AGENDADO, PUBLICADO, INDISPONIVEL }

fun ExameResponse.situacao(agora: Instant = Instant.now()): SituacaoExame = when (status?.trim()?.uppercase(Locale.ROOT)) {
    "PUBLICADO" -> SituacaoExame.PUBLICADO
    "AGENDADO" -> if (instanteExame(disponibilizacaoEm)?.let { !it.isAfter(agora) } == true) {
        SituacaoExame.PUBLICADO
    } else SituacaoExame.AGENDADO
    else -> SituacaoExame.INDISPONIVEL
}

fun List<ExameResponse>.examesDoPaciente(pacienteId: Long): List<ExameResponse> =
    filter { it.id != null && it.id > 0 && it.pacienteId == pacienteId && it.situacao() != SituacaoExame.INDISPONIVEL }
        .distinctBy { it.id }
        .sortedWith(compareByDescending<ExameResponse> { dataColetaExame(it.dataColeta) ?: LocalDate.MIN }.thenByDescending { it.id })

fun ExameResponse.nomePdf(): String {
    val original = nomeArquivo.orEmpty().replace('\\', '/').substringAfterLast('/')
        .filterNot { it.isISOControl() }.replace(Regex("[<>:\"|?*]"), "_").trim()
    val name = (if (original.endsWith(".pdf", ignoreCase = true)) original.dropLast(4) else original)
        .trim().take(120).trimEnd('.', ' ')
    return "${name.ifBlank { "exame-${id ?: "laudo"}" }}.pdf"
}

val ExameResponse.titulo: String
    get() = nomePdf().dropLast(4).replace('_', ' ').trim()

internal fun instanteExame(value: String?): Instant? = try {
    value?.trim()?.takeIf { it.isNotBlank() }?.let(Instant::parse)
} catch (_: DateTimeParseException) { null }

private fun dataColetaExame(value: String?): LocalDate? = try {
    value?.trim()?.let(LocalDate::parse)
} catch (_: DateTimeParseException) { null }

fun formatarColetaExame(value: String?): String = dataColetaExame(value)
    ?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))) ?: "Data não informada"

fun formatarDisponibilidadeExame(value: String?, zone: ZoneId = ZoneId.systemDefault()): String? =
    instanteExame(value)?.atZone(zone)?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR")))
