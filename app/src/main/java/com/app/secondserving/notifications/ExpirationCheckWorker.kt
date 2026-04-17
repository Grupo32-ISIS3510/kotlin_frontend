package com.app.secondserving.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.app.secondserving.SecondServingApp
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Worker que revisa una vez al día si hay items próximos a vencer y dispara
 * las notificaciones correspondientes. Reemplaza al observer permanente que
 * reaccionaba a cada escritura en la DB.
 */
class ExpirationCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as SecondServingApp
            val items = app.inventoryRepository.getExpiringSoonItemsOnce()
            app.expirationNotifier.checkAndNotify(items)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error ejecutando chequeo de expiración", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "ExpirationCheckWorker"
        private const val WORK_NAME = "expiration_check_daily"
        private val NOTIFICATION_HOUR = LocalTime.of(9, 0)

        fun enqueueDaily(context: Context) {
            val request = PeriodicWorkRequestBuilder<ExpirationCheckWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(computeInitialDelay().toMinutes(), TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        private fun computeInitialDelay(): Duration {
            val now = LocalDateTime.now()
            val nextRun = now.toLocalDate().atTime(NOTIFICATION_HOUR).let {
                if (it.isAfter(now)) it else it.plusDays(1)
            }
            return Duration.between(now, nextRun)
        }
    }
}
