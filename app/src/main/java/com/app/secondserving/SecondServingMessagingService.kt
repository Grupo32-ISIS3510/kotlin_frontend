package com.app.secondserving

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SecondServingMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "expiry_alerts"
        const val CHANNEL_NAME = "Alertas de vencimiento"
        const val EXTRA_NAVIGATE_TO = "navigate_to"
        const val NAV_EXPIRING = "expiring"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Token renovado: $token")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = applicationContext as SecondServingApp
                app.registerFcmToken(token)
            } catch (e: Exception) {
                Log.e("FCM", "Error registrando token renovado", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("FCM", "Mensaje recibido: ${message.notification?.title}")

        val title = message.notification?.title ?: "Second Serving"
        val body = message.notification?.body ?: "Tienes alimentos próximos a vencer"

        showNotification(title, body)

        // Registramos notification_received en el backend para alimentar la
        // BQ T4.1 (open_rate = opened / received).
        runCatching {
            (applicationContext as? SecondServingApp)?.analyticsRepository?.logNotificationReceived(
                mapOf("source" to "fcm", "title" to title)
            )
        }
    }

    private fun showNotification(title: String, body: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO, NAV_EXPIRING)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}