package com.app.secondserving.data.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Proxy pattern (sección 4.8 del documento de arquitectura).
 * Intercepta todas las peticiones salientes y adjunta el Bearer token.
 * El resto de la app nunca gestiona el token directamente.
 */
class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokenProvider()

        val request = if (token != null) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }

        return chain.proceed(request)
    }
}
