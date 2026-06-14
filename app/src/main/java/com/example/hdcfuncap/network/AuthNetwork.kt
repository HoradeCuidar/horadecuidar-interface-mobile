package com.example.hdcfuncap.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
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
interface AuthApi {
    @POST("auth/logar")
    suspend fun logar(@Body request: LoginRequest): LoginResponse

    @GET("pacientes/{id}/prescricoes/medicamentos/paciente/hoje")
    suspend fun getMedicamentosHoje(
        @Path("id") pacienteId: Long
    ): List<MedicamentoHojeResponse>
}
