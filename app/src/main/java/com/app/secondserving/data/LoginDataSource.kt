package com.app.secondserving.data

import com.app.secondserving.data.model.LoggedInUser
import com.app.secondserving.data.network.LoginRequest
import com.app.secondserving.data.network.RegisterRequest
import com.app.secondserving.data.network.RetrofitClient
import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class LoginDataSource {

    suspend fun login(username: String, password: String): Result<LoggedInUser> {
        return try {
            val response = RetrofitClient.publicInstance.login(LoginRequest(username, password))
            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!
                val loggedInUser = LoggedInUser(
                    userId = tokenResponse.user.id,
                    displayName = tokenResponse.user.full_name,
                    email = tokenResponse.user.email,
                    token = tokenResponse.access_token
                )
                Result.Success(loggedInUser)
            } else {
                val backendMessage = response.errorBody()?.string().orEmpty().extractBackendErrorMessage()
                val fallback = "No fue posible iniciar sesión. Revisa tus credenciales."
                Result.Error(IOException(backendMessage ?: fallback))
            }
        } catch (e: Exception) {
            Result.Error(IOException(e.message ?: "Error de red al iniciar sesión", e))
        }
    }

    suspend fun register(email: String, fullName: String, password: String): Result<LoggedInUser> {
        return try {
            val response = RetrofitClient.publicInstance.register(RegisterRequest(email, fullName, password))
            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!
                val loggedInUser = LoggedInUser(
                    userId = tokenResponse.user.id,
                    displayName = tokenResponse.user.full_name,
                    email = tokenResponse.user.email,
                    token = tokenResponse.access_token
                )
                Result.Success(loggedInUser)
            } else {
                val backendMessage = response.errorBody()?.string().orEmpty().extractBackendErrorMessage()
                val fallback = "No fue posible crear la cuenta. Intenta de nuevo."
                Result.Error(IOException(backendMessage ?: fallback))
            }
        } catch (e: Exception) {
            Result.Error(IOException(e.message ?: "Error de red al registrar la cuenta", e))
        }
    }

    fun logout() {
        // TODO: revoke authentication
    }
}

private fun String.extractBackendErrorMessage(): String? {
    if (isBlank()) return null
    return try {
        val json = JSONObject(this)
        val message = json.optString("message").ifBlank { null }
        val detail = json.opt("detail")
        when {
            message != null -> message
            detail is String -> detail
            detail is JSONObject -> detail.optString("message").ifBlank { null }
            detail is JSONArray -> {
                val firstError = detail.optJSONObject(0)
                firstError?.optString("msg")?.ifBlank { null }
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}