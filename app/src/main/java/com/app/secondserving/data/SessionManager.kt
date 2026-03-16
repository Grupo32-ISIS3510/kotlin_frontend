package com.app.secondserving.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN       = "access_token"
        private const val KEY_FULL_NAME   = "full_name"
        private const val KEY_EMAIL       = "email"
        private const val KEY_USER_ID     = "user_id"
        private const val KEY_SAVED_AT    = "token_saved_at_ms"

        // El backend expira el JWT en 60 min; usamos 55 min para refrescar antes
        private const val SESSION_TTL_MS  = 55 * 60 * 1000L
    }

    fun saveSession(token: String, fullName: String, email: String, userId: String) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_FULL_NAME, fullName)
            putString(KEY_EMAIL, email)
            putString(KEY_USER_ID, userId)
            putLong(KEY_SAVED_AT, System.currentTimeMillis())
            apply()
        }
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getFullName(): String? = prefs.getString(KEY_FULL_NAME, null)
    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    /** Devuelve true si hay token guardado Y no ha expirado (< 55 min) */
    fun isSessionValid(): Boolean {
        val token = getToken() ?: return false
        if (token.isBlank()) return false
        val savedAt = prefs.getLong(KEY_SAVED_AT, 0L)
        return (System.currentTimeMillis() - savedAt) < SESSION_TTL_MS
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
