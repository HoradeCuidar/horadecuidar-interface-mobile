package com.example.hdcfuncap.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(
    val username: String,
    val senha: String
)
data class LoginResponse(
    val token: String
)
interface AuthApi {
    @POST("auth/logar")
    suspend fun logar(@Body request: LoginRequest): LoginResponse
}
object RetrofitClient {
    private const val BASE_URL = "https://horadecuidar-api-production.up.railway.app/api/"
    val authApi: AuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }
}