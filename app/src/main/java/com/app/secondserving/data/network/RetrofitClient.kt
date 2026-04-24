package com.app.secondserving.data.network

import com.app.secondserving.data.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "http://3.16.198.192/api/v1/"
    private const val EDAMAM_BASE_URL = "https://api.edamam.com/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val baseClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    // Cliente específico para Edamam con el Header requerido
    private val edamamClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Edamam-Account-User", "arnulfo_user")
                .build()
            chain.proceed(request)
        }
        .build()

    // Cliente sin autenticación — login y register
    val publicInstance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(baseClient)
            .build()
            .create(ApiService::class.java)
    }

    // Cliente para Edamam (Recetas en Español)
    val edamamInstance: EdamamService by lazy {
        Retrofit.Builder()
            .baseUrl(EDAMAM_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(edamamClient) // Usamos el cliente con el header
            .build()
            .create(EdamamService::class.java)
    }

    // Cliente autenticado — todos los endpoints protegidos
    lateinit var authInstance: ApiService
        private set

    fun init(sessionManager: SessionManager) {
        val authInterceptor = AuthInterceptor { sessionManager.getToken() }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        authInstance = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiService::class.java)
    }
}
