package com.example.hdcfuncap.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

data class LoginRequest(
    val username: String,
    val senha: String
)
data class LoginResponse(
    val id: Long,
    val token: String,
    val nome: String

)
data class AtualizarAdesaoRequest(
    val itemMedicacaoId: Long,
    val status: String,
    val observacao: String
)

data class SolicitarRecuperacaoSenhaRequest(
    val email: String
)

data class ResetarSenhaRequest(
    val token: String,
    val novaSenha: String,
    val confirmacao: String
)

data class MessageResponse(
    val message: String
)

data class PrescricaoMedicamentoResponse(
    val id: String,
    val nomeProfissional: String?,
    val dataInicio: String?,
    val dataFim: String?,
    val itens: List<ItemMedicacaoResponse>?
)

data class ItemMedicacaoResponse(
    val itemId: Long,
    val nomeMedicamento: String,
    val dosagemFormatada: String?,
    val frequencia: String?,
    val viaAdministracao: String?
)

interface AuthApi {
    @POST("auth/logar")
    suspend fun logar(@Body request: LoginRequest): LoginResponse

    @POST("auth/recuperacao-senha")
    suspend fun solicitarRecuperacaoSenha(
        @Body request: SolicitarRecuperacaoSenhaRequest
    ): MessageResponse

    @POST("auth/resetar-senha")
    suspend fun resetarSenha(
        @Body request: ResetarSenhaRequest
    ): MessageResponse

    @GET("pacientes/{id}/prescricoes/medicamentos/paciente/hoje")
    suspend fun getMedicamentosHoje(
        @Path("id") pacienteId: Long
    ): List<MedicamentoHojeResponse>

    @GET("pacientes/{id}/prescricoes/medicamentos/paciente/ativas")
    suspend fun getPrescricoesMedicamentos(
        @Path("id") pacienteId: Long
    ): List<PrescricaoMedicamentoResponse>

    @PUT("pacientes/{id}/prescricoes/medicamentos/paciente/adesao/{adesaoId}")
    suspend fun registrarAdesao(
        @Path("id") pacienteId: Long,
        @Path("adesaoId") adesaoId: Long,
        @Body request: AtualizarAdesaoRequest
    )

    @POST("pacientes/{id}/prescricoes/medicamentos/paciente/adesao")
    suspend fun criarAdesao(
        @Path("id") pacienteId: Long,
        @Body request: AtualizarAdesaoRequest
    )
}

