package com.app.secondserving

import android.app.Application
import android.util.Log
import com.app.secondserving.data.ExpirationNotifier
import com.app.secondserving.data.InventoryRepository
import com.app.secondserving.data.SessionManager
import com.app.secondserving.data.local.AppDatabase
import com.app.secondserving.data.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SecondServingApp : Application() {

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

        // 1. LO PRIMERO: Configurar la red. Sin esto nada funciona.
        sessionManager = SessionManager(this)
        RetrofitClient.init(sessionManager)

        // 2. LO SEGUNDO: Base de datos y Repositorio.
        database = AppDatabase.getDatabase(this)
        inventoryRepository = InventoryRepository(database)

        // 3. LO TERCERO: Servicios secundarios.
        expirationNotifier = ExpirationNotifier(this, inventoryRepository)
        expirationNotifier.startObserving()
    }

    fun registerFcmToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.authInstance.registerFcmToken(mapOf("token" to token))
                if (response.isSuccessful) {
                    Log.d("FCM", "Token registrado correctamente")
                } else {
                    Log.e("FCM", "Error registrando token: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("FCM", "Error registrando token", e)
            }
        }
    }
}
