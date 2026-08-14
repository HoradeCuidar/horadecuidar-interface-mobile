package com.example.hdcfuncap.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

data class LoginRequest(
    val username: String,
    val senha: String
)
data class LoginResponse(
    val id: Long?,
    val token: String?,
    val nome: String?,
    val role: String?
)
data class AtualizarAdesaoRequest(
    val ocorrenciaId: Long,
    val itemMedicacaoId: Long,
    val ordemNoDia: Int?,
    val dataPrevista: String?,
    val quantidadeDiaria: Int?,
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
    val message: String?
)

data class PacienteProfileResponse(
    val id: Long?,
    val nome: String?,
    val email: String?,
    val username: String?,
    val dataDeNascimento: String?,
    val role: String?,
    val status: String?,
    val telefone: String?,
    val rua: String?,
    val bairro: String?,
    val estado: String?,
    val cidade: String?,
    val numeroDaCasa: String?,
    val genero: String?,
    val doencas: List<DoencaProfileResponse>?,
    val observacoes: String?,
    val fotoDePerfil: String?
)

data class PacienteProfileUpdateRequest(
    val nome: String,
    val email: String,
    val telefone: String,
    val genero: String,
    val dataDeNascimento: String,
    val rua: String?,
    val bairro: String?,
    val estado: String?,
    val cidade: String?,
    val numeroDaCasa: String?
)

data class DoencaProfileResponse(
    val id: Long?,
    val nome: String?
)

data class PrescricaoMedicamentoResponse(
    val id: String?,
    val nomeProfissional: String?,
    val dataInicio: String?,
    val dataFim: String?,
    val itens: List<ItemMedicacaoResponse>?,
    val medicacoes: List<ItemMedicacaoResponse>?
)

data class ItemMedicacaoResponse(
    val itemId: Long?,
    val id: Long?,
    val nomeMedicamento: String?,
    val dosagemFormatada: String?,
    val frequencia: String?,
    val viaAdministracao: String?,
    val dosagemValor: Double?,
    val dosagemUnidade: String?,
    val quantidadeDoses: Int?,
    val intervaloValor: Int?,
    val intervaloTipo: String?
)

data class MedicamentosDiaResponse(
    val data: String?,
    val ocorrencias: List<OcorrenciaMedicamentoResponse>?
)

data class OcorrenciaMedicamentoResponse(
    val id: Long?,
    val prescricaoId: String?,
    val itemMedicacao: ItemMedicacaoOcorrenciaResponse?,
    val dataPrevista: String?,
    val ordemNoDia: Int?,
    val dataHoraRegistro: String?,
    val observacao: String?
)

data class ItemMedicacaoOcorrenciaResponse(
    val id: Long?,
    val nomeMedicamento: String?,
    val dosagemValor: Double?,
    val dosagemUnidade: String?,
    val quantidadeDoses: Int?,
    val intervaloValor: Int?,
    val intervaloTipo: String?,
    val viaAdministracao: String?,
    val ativo: Boolean?,
    val observacao: String?
)

data class OrientacoesFuncionaisPageResponse(
    val content: List<OrientacaoFuncionalResponse>?
)

data class OrientacaoFuncionalResponse(
    val id: Long?,
    val responsavel: ProfissionalSaudeResumoResponse?,
    val nome: String?,
    val descricao: String?,
    val finalidade: String?,
    val urlImagem: String?,
    val ativo: Boolean?,
    val tags: List<TagFuncionalResponse>?,
    val dataCriacao: String?,
    val dataAtualizacao: String?
)

data class ProfissionalSaudeResumoResponse(
    val id: Long?,
    val nome: String?,
    val email: String?
)

data class TagFuncionalResponse(
    val id: Long?,
    val nome: String?,
    val descricao: String?
)

data class RegistroRealizacaoFuncionalRequest(
    val id: Long,
    val status: String,
    val duracaoRealizadaMinutos: Int?,
    val sensacaoFinal: String?,
    val observacao: String?
)

data class RegistroRealizacaoFuncionalResponse(
    val id: Long?,
    val orientacaoFuncionalId: Long?,
    val nomeOrientacao: String?,
    val status: String?,
    val duracaoRealizadaMinutos: Int?,
    val sensacaoFinal: String?,
    val observacao: String?,
    val dataRegistro: String?
)

data class HistoricoRealizacaoFuncionalPageResponse(
    val content: List<RegistroRealizacaoFuncionalResponse>?
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

    @GET("paciente/profile")
    suspend fun getPacienteProfile(): PacienteProfileResponse

    @PUT("paciente/perfil")
    suspend fun atualizarPacientePerfil(
        @Body request: PacienteProfileUpdateRequest
    ): PacienteProfileResponse

    @GET("pacientes/{id}/prescricoes/medicamentos/paciente/ocorrencias-medicamentos")
    suspend fun getMedicamentosHoje(
        @Path("id") pacienteId: Long,
        @Query("data") data: String
    ): MedicamentosDiaResponse

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

    @GET("orientacoes-funcionais/paciente")
    suspend fun getOrientacoesFuncionaisPaciente(
        @Query("number-page") numberPage: Int = 0,
        @Query("page-size") pageSize: Int = 50
    ): OrientacoesFuncionaisPageResponse

    @GET("paciente/{pacienteId}/realizacao-funcional/historico")
    suspend fun getHistoricoRealizacaoFuncional(
        @Path("pacienteId") pacienteId: Long,
        @Query("number-page") numberPage: Int = 0,
        @Query("page-size") pageSize: Int = 100
    ): HistoricoRealizacaoFuncionalPageResponse

    @POST("paciente/{pacienteId}/realizacao-funcional")
    suspend fun registrarRealizacaoFuncional(
        @Path("pacienteId") pacienteId: Long,
        @Body request: RegistroRealizacaoFuncionalRequest
    ): RegistroRealizacaoFuncionalResponse

    @PUT("paciente/{pacienteId}/realizacao-funcional/{registroId}")
    suspend fun alterarRealizacaoFuncional(
        @Path("pacienteId") pacienteId: Long,
        @Path("registroId") registroId: Long,
        @Body request: RegistroRealizacaoFuncionalRequest
    ): RegistroRealizacaoFuncionalResponse
}
