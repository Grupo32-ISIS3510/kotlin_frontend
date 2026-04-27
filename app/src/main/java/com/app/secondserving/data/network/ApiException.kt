package com.app.secondserving.data.network

import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response

/**
 * Error HTTP del backend (la respuesta llegó pero con código != 2xx).
 *
 * Se distingue intencionalmente de IOException para que las capas superiores
 * (InventoryRepository, etc.) NO traten errores 4xx/5xx como "estoy offline" y
 * encolen el item como pending operation. Validaciones del backend, 401, 404,
 * etc. no se recuperan reintentando: deben propagarse a la UI tal cual.
 */
class ApiException(
    val code: Int,
    val userMessage: String,
    cause: Throwable? = null
) : Exception(userMessage, cause)

/**
 * Convierte una Response<*> sin éxito en una ApiException, parseando el
 * cuerpo del error para extraer un mensaje amigable.
 *
 * Soporta el formato estándar del backend:
 *   {"status":422,"code":"VALIDATION_ERROR","message":"...",
 *    "details":[{"field":"","message":"La fecha de vencimiento debe ser..."}]}
 */
internal fun Response<*>.toApiException(fallback: String): ApiException {
    val body = errorBody()?.string().orEmpty()
    val parsed = parseBackendErrorMessage(body)
    return ApiException(
        code = code(),
        userMessage = parsed ?: "$fallback (${code()})"
    )
}

private fun parseBackendErrorMessage(body: String): String? {
    if (body.isBlank()) return null
    return try {
        val json = JSONObject(body)

        val details = json.optJSONArray("details")
        if (details != null && details.length() > 0) {
            val firstDetailMsg = details.optJSONObject(0)?.optString("message")?.ifBlank { null }
            if (firstDetailMsg != null) return firstDetailMsg
        }

        val message = json.optString("message").ifBlank { null }
        if (message != null) return message

        when (val detail = json.opt("detail")) {
            is String -> detail.ifBlank { null }
            is JSONObject -> detail.optString("message").ifBlank { null }
            is JSONArray -> detail.optJSONObject(0)?.optString("msg")?.ifBlank { null }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}
