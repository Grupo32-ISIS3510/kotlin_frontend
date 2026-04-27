package com.app.secondserving.data.network

import com.app.secondserving.data.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "http://3.16.198.192/api/v1/"

    // Callback inyectado por SecondServingApp. Se llama desde el interceptor
    // de respuesta cuando el backend devuelve 401. No puede llamar a la UI
    // directamente (hilo de red), por eso usa un flow que la UI colecta.
    private var onUnauthorized: (() -> Unit)? = null

    // EDAMAM_BASE_URL ya no se usa: las recetas vienen del backend del equipo.
    // private const val EDAMAM_BASE_URL = "https://api.edamam.com/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val baseClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    // El cliente con el header Edamam-Account-User dejó de usarse cuando
    // migramos a /recipes/suggestions del backend (ver RecipeRepository).
    // private val edamamClient = OkHttpClient.Builder()
    //     .addInterceptor(logging)
    //     .addInterceptor { chain ->
    //         val request = chain.request().newBuilder()
    //             .addHeader("Edamam-Account-User", "arnulfo_user")
    //             .build()
    //         chain.proceed(request)
    //     }
    //     .build()

    // Cliente sin autenticación — login y register
    val publicInstance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(baseClient)
            .build()
            .create(ApiService::class.java)
    }

    // Instancia Retrofit para Edamam — desactivada porque ahora las
    // recetas vienen del backend del equipo a través de authInstance.
    // val edamamInstance: EdamamService by lazy {
    //     Retrofit.Builder()
    //         .baseUrl(EDAMAM_BASE_URL)
    //         .addConverterFactory(GsonConverterFactory.create())
    //         .client(edamamClient)
    //         .build()
    //         .create(EdamamService::class.java)
    // }

    // Cliente autenticado — todos los endpoints protegidos
    lateinit var authInstance: ApiService
        private set

    fun init(sessionManager: SessionManager, onUnauthorized: (() -> Unit)? = null) {
        RetrofitClient.onUnauthorized = onUnauthorized
        val authInterceptor = AuthInterceptor { sessionManager.getToken() }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor { chain ->
                val response = chain.proceed(chain.request())
                // 401 = token expirado o inválido. Señalamos a la UI para que
                // haga logout completo. No lanzamos excepción: devolvemos la
                // respuesta para que el caller (Repository) la maneje también.
                if (response.code == 401) {
                    RetrofitClient.onUnauthorized?.invoke()
                }
                response
            }
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
