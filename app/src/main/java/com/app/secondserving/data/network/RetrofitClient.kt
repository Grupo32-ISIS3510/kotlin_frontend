package com.app.secondserving.data.network

import com.app.secondserving.data.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // Para celular físico usa: "http://192.168.100.117:8000/api/v1/"
    private const val BASE_URL = "http://3.16.198.192/api/v1/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Cliente sin autenticación — login y register
    val publicInstance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build()
            )
            .build()
            .create(ApiService::class.java)
    }

    // Cliente autenticado — todos los endpoints protegidos
    // Se inicializa al arrancar la app via SecondServingApp
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
