package com.app.secondserving.data

import com.app.secondserving.data.model.LoggedInUser
import com.app.secondserving.data.network.LoginRequest
import com.app.secondserving.data.network.RegisterRequest
import com.app.secondserving.data.network.RetrofitClient
import java.io.IOException

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
                Result.Error(IOException("Error logging in: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(IOException("Error logging in", e))
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
                Result.Error(IOException("Error registering: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(IOException("Error registering", e))
        }
    }

    fun logout() {
        // TODO: revoke authentication
    }
}