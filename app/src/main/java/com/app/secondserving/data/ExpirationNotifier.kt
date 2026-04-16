package com.app.secondserving.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.app.secondserving.MainActivity
import com.app.secondserving.R
import com.app.secondserving.data.local.FoodItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Observer Pattern - Notificador de expiración de alimentos.
 * Patrón Observer:
 * - Observa cambios en los items del inventario
 * - Notifica cuando un alimento está por expirar
 * - Usa notificaciones locales de Android
 */
class ExpirationNotifier(private val context: Context, private val repository: InventoryRepository) {

    companion object {
        private const val CHANNEL_ID = "expiration_alerts"
        private const val CHANNEL_NAME = "Alertas de Expiración"
        private const val NOTIFICATION_DELAY_MS = 5000L // 5 segundos entre notificaciones
    }

    private var observerJob: Job? = null

    /**
     * Crea el canal de notificaciones (requerido Android 8+).
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notificaciones cuando los alimentos están por expirar"
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Inicia el observador que monitorea los items que expiran pronto.
     * Idempotente: si ya está observando, no inicia otro.
     */
    fun startObserving() {
        // Evitar scopes huérfanos si se llama dos veces
        if (observerJob?.isActive == true) return

        stopObserving() // Cancela cualquier scope previo que haya fallado

        createNotificationChannel()

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        observerJob = scope.launch {
            repository.getExpiringSoonItems().collectLatest { items ->
                items.forEach { item ->
                    val daysRemaining = calculateDaysRemaining(item.expiryDate)
                    if (daysRemaining >= 0 && daysRemaining <= 3) {
                        sendExpirationNotification(item, daysRemaining)
                        delay(NOTIFICATION_DELAY_MS)
                    }
                }
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

        val pendingIntent = PendingIntent.getActivity(
            context,
            item.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Color según urgencia
        val color = when {
            daysRemaining <= 1L -> 0xFFFF0000.toInt() // Rojo
            daysRemaining <= 3L -> 0xFFFFA500.toInt() // Naranja
            else -> 0xFFFFFF00.toInt() // Amarillo
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColor(color)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Enviar notificación
        NotificationManagerCompat.from(context).notify(
            item.id.hashCode(),
            notification
        )
    }

    /**
     * Calcula días restantes hasta la expiración.
     */
    private fun calculateDaysRemaining(expiryDate: String): Long {
        return try {
            val expiry = LocalDate.parse(expiryDate)
            val today = LocalDate.now()
            ChronoUnit.DAYS.between(today, expiry)
        } catch (e: Exception) {
            Log.w("ExpirationNotifier", "Fecha de expiración inválida: $expiryDate", e)
            Long.MAX_VALUE // Tratamos fecha inválida como "nunca expira" para no notificar falsos positivos
        }
    }

    /**
     * Envía una notificación inmediata para un item específico.
     * Útil cuando el usuario agrega un item que ya está cerca de expirar.
     */
    fun notifyImmediateExpiration(item: FoodItemEntity) {
        val daysRemaining = calculateDaysRemaining(item.expiryDate)
        if (daysRemaining >= 0 && daysRemaining <= 3) {
            sendExpirationNotification(item, daysRemaining)
        }
    }
}
