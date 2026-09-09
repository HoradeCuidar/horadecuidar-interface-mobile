package com.example.hdcfuncap.network

import android.content.Context
import com.example.hdcfuncap.storage.UserPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://horadecuidar-api-production.up.railway.app/api/"
    @Volatile
    private var sessionExpiredHandler: (() -> Unit)? = null
    @Volatile
    private var authApi: AuthApi? = null

    fun setSessionExpiredHandler(handler: (() -> Unit)?) {
        sessionExpiredHandler = handler
    }

    fun getAuthApi(context: Context): AuthApi {
        authApi?.let { return it }

        return synchronized(this) {
            authApi ?: createAuthApi(context.applicationContext).also { authApi = it }
        }
    }

    private fun createAuthApi(context: Context): AuthApi {
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
            val statusCode = response.code
            if (!token.isNullOrEmpty() && (statusCode == 401 || statusCode == 403)) {
                sessionExpiredHandler?.invoke()
            }
            response
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
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
