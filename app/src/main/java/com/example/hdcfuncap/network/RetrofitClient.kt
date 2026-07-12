package com.example.hdcfuncap.network

import android.content.Context
import com.example.hdcfuncap.storage.UserPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8080/api/"
    @Volatile
    private var sessionExpiredHandler: (() -> Unit)? = null

    fun setSessionExpiredHandler(handler: (() -> Unit)?) {
        sessionExpiredHandler = handler
    }

    fun getAuthApi(context: Context): AuthApi {
        val userPreferences = UserPreferences(context)

        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val token = runBlocking { userPreferences.getAuthToken() }

            val newRequest = if (!token.isNullOrEmpty()) {
                originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                originalRequest
            }

            val response = chain.proceed(newRequest)
            val statusCode = response.code()
            if (!token.isNullOrEmpty() && (statusCode == 401 || statusCode == 403)) {
                sessionExpiredHandler?.invoke()
            }
            response
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }
}
