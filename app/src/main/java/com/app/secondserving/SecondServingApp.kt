package com.app.secondserving

import android.app.Application
import android.util.Log
import com.app.secondserving.data.AnalyticsRepository
import com.app.secondserving.data.ExpirationNotifier
import com.app.secondserving.data.InventoryRepository
import com.app.secondserving.data.NetworkMonitor
import com.app.secondserving.data.SavingsCache
import com.app.secondserving.data.SessionManager
import com.app.secondserving.data.local.AppDatabase
import com.app.secondserving.data.network.RetrofitClient
import com.app.secondserving.notifications.ExpirationCheckWorker
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

    lateinit var analyticsRepository: AnalyticsRepository
        private set

    // NetworkMonitor singleton: la UI lo observa (banner offline) y se
    // expone para que más adelante AnalyticsSyncWorker reaccione cuando
    // vuelva la red. Vive durante toda la vida del proceso.
    lateinit var networkMonitor: NetworkMonitor
        private set

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(this)
        database = AppDatabase.getDatabase(this)
        RetrofitClient.init(sessionManager)
        inventoryRepository = InventoryRepository(database, savingsCache = SavingsCache(this))
        expirationNotifier = ExpirationNotifier(this)
        expirationNotifier.createNotificationChannel()
        // analyticsRepository es singleton: lo comparten MainActivity (notification_opened),
        // ExpirationNotifier y SecondServingMessagingService (notification_received), y
        // los ViewModels que necesitan llamar al backend (UserSegmentViewModel).
        analyticsRepository = AnalyticsRepository()
        expirationNotifier.setAnalyticsRepository(analyticsRepository)
        networkMonitor = NetworkMonitor(this)
        ExpirationCheckWorker.enqueueDaily(this)
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