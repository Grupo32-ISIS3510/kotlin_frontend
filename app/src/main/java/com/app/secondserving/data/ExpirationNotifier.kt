package com.app.secondserving.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.app.secondserving.MainActivity
import com.app.secondserving.data.local.FoodItemEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Notificador de expiración de alimentos.
 * - Stateless respecto a Flows: no observa la DB.
 * - Deduplica notificaciones por (itemId, fecha) usando SharedPreferences,
 *   así un item sólo genera una notificación por día aunque se dispare el
 *   chequeo varias veces.
 */
class ExpirationNotifier(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "expiry_alerts"
        private const val CHANNEL_NAME = "Alertas de vencimiento"
        private const val PREFS_NAME = "expiration_notifier_prefs"
        private const val KEY_NOTIFIED_SET = "notified_keys"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones cuando los alimentos están por expirar"
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Chequeo único: notifica una vez al día por item que venza en ≤3 días.
     * Lo llama el worker diario o el flujo de "item recién agregado".
     */
    fun checkAndNotify(items: List<FoodItemEntity>) {
        createNotificationChannel()
        pruneOldKeys()

        val today = LocalDate.now().toString()
        val notified = prefs.getStringSet(KEY_NOTIFIED_SET, emptySet())!!.toMutableSet()

        items.forEach { item ->
            val key = "${item.id}:$today"
            if (key in notified) return@forEach

            val daysRemaining = calculateDaysRemaining(item.expiryDate)
            if (daysRemaining in 0..3) {
                if (sendExpirationNotification(item, daysRemaining)) {
                    notified += key
                }
            }
        }

        prefs.edit().putStringSet(KEY_NOTIFIED_SET, notified).apply()
    }

    fun notifyImmediateExpiration(item: FoodItemEntity) {
        checkAndNotify(listOf(item))
    }

    private fun sendExpirationNotification(item: FoodItemEntity, daysRemaining: Long): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }

        val title = when (daysRemaining) {
            0L -> "¡${item.name} vence hoy!"
            1L -> "¡${item.name} vence mañana!"
            else -> "${item.name} vence en $daysRemaining días"
        }
        val message = "Categoría: ${item.category} | Cantidad: ${item.quantity}"

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

        val color = when {
            daysRemaining <= 1L -> 0xFFFF0000.toInt()
            daysRemaining <= 3L -> 0xFFFFA500.toInt()
            else -> 0xFFFFFF00.toInt()
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

        NotificationManagerCompat.from(context).notify(item.id.hashCode(), notification)
        return true
    }

    private fun calculateDaysRemaining(expiryDate: String): Long {
        return try {
            ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(expiryDate))
        } catch (e: Exception) {
            Log.w("ExpirationNotifier", "Fecha inválida: $expiryDate", e)
            Long.MAX_VALUE
        }
    }

    private fun pruneOldKeys() {
        val today = LocalDate.now().toString()
        val existing = prefs.getStringSet(KEY_NOTIFIED_SET, emptySet())!!
        val pruned = existing.filter { it.endsWith(":$today") }.toSet()
        if (pruned.size != existing.size) {
            prefs.edit().putStringSet(KEY_NOTIFIED_SET, pruned).apply()
        }
    }
}
