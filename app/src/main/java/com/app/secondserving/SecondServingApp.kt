package com.app.secondserving

import android.app.Application
import android.util.Log
import com.app.secondserving.data.SessionManager
import com.app.secondserving.data.network.RetrofitClient
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SecondServingApp : Application() {

    lateinit var sessionManager: SessionManager
        private set

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(this)
        RetrofitClient.init(sessionManager)
        registerFcmTokenIfLoggedIn()
    }

    fun registerFcmToken(token: String) {
        val savedToken = sessionManager.getToken() ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RetrofitClient.authInstance.registerFcmToken(
                    mapOf("token" to token, "device_type" to "android")
                )
                Log.d("FCM", "Token registrado en backend")
            } catch (e: Exception) {
                Log.e("FCM", "Error registrando token", e)
            }
        }
    }

    private fun registerFcmTokenIfLoggedIn() {
        if (sessionManager.getToken() == null) return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            registerFcmToken(token)
        }
    }
}