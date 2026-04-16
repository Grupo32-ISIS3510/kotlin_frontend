package com.app.secondserving

import android.app.Application
import android.util.Log
import com.app.secondserving.data.ExpirationNotifier
import com.app.secondserving.data.InventoryRepository
import com.app.secondserving.data.SessionManager
import com.app.secondserving.data.local.AppDatabase
import com.app.secondserving.data.network.RetrofitClient
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SecondServingApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var sessionManager: SessionManager
        private set

    lateinit var database: AppDatabase
        private set

    lateinit var inventoryRepository: InventoryRepository
        private set

    lateinit var expirationNotifier: ExpirationNotifier
        private set

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(this)
        database = AppDatabase.getDatabase(this)
        inventoryRepository = InventoryRepository(database)
        expirationNotifier = ExpirationNotifier(this, inventoryRepository)
        expirationNotifier.startObserving()
        RetrofitClient.init(sessionManager)
        registerFcmTokenIfLoggedIn()
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationScope.cancel()
    }

    fun registerFcmToken(token: String) {
        val savedToken = sessionManager.getToken() ?: return
        applicationScope.launch {
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