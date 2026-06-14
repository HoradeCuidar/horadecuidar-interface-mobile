package com.example.hdcfuncap.network

data class MedicamentoHojeResponse(
    val itemId: Long,
    val prescricaoId: String,
    val nomeMedicamento: String,
    val dosagemFormatada: String,
    val frequencia: String,
    val viaAdministracao: String,
    val observacao: String,
    val statusAdesaoHoje: String?,
    val adesaoId: Long?,
    val dosesEsperadasHoje: Int,
    val dosesRegistradasHoje: Int
)