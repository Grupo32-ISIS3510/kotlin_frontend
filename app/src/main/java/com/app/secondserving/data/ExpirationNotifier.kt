package com.app.secondserving.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.app.secondserving.MainActivity
import com.app.secondserving.R
import com.app.secondserving.data.local.FoodItemEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class ExpirationNotifier(
    private val context: Context,
    private val repository: InventoryRepository
) {
    private val notificationManager = NotificationManagerCompat.from(context)
    private val CHANNEL_ID = "expiration_notifications"
    private var observerJob: Job? = null
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notificaciones de Expiración"
            val descriptionText = "Avisos sobre productos próximos a vencer"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Inicia un observador que revisa diariamente los productos.
     */
    fun startObserving() {
        if (observerJob != null) return

        observerJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                checkExpirations()
                // Esperar 24 horas antes de la siguiente revisión
                delay(24 * 60 * 60 * 1000)
            }
        }
    }

    /**
     * Revisa la base de datos y envía notificaciones para productos por vencer.
     */
    suspend fun checkExpirations() {
        val items = repository.getInventoryFlow().first()
        val today = LocalDate.now()

        items.forEach { item ->
            try {
                if (item.expiryDate.isNotBlank()) {
                    val expirationDate = LocalDate.parse(item.expiryDate, dateFormatter)
                    val daysRemaining = ChronoUnit.DAYS.between(today, expirationDate)

                    // Notificar si vence hoy, mañana o en 3 días
                    if (daysRemaining in 0L..3L) {
                        sendExpirationNotification(item, daysRemaining)
                    }
                }
            } catch (e: Exception) {
                // Ignorar items con fecha inválida
            }
        }
    }

    /**
     * Detiene el observador.
     */
    fun stopObserving() {
        observerJob?.cancel()
        observerJob = null
    }

    /**
     * Envía una notificación de expiración para un item.
     */
    private fun sendExpirationNotification(item: FoodItemEntity, daysRemaining: Long) {
        // Verificar permiso para notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return // No tenemos permiso para notificar
            }
        }

        val title = when {
            daysRemaining == 0L -> "¡${item.name} vence hoy!"
            daysRemaining == 1L -> "¡${item.name} vence mañana!"
            else -> "${item.name} vence en $daysRemaining días"
        }

        val message = "Categoría: ${item.category} | Cantidad: ${item.quantity}"

        // Intent para abrir la app al tocar la notificación
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("expiring_item_id", item.id)
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            item.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Asegúrate de tener un icono
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            notificationManager.notify(item.id.hashCode(), builder.build())
        } catch (e: SecurityException) {
            // Manejar excepción de falta de permisos si ocurre
        }
    }
}
