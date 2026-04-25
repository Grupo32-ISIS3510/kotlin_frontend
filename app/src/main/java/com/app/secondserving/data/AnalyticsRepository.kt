package com.app.secondserving.data

import android.util.Log
import com.app.secondserving.data.network.AnalyticsEventBatch
import com.app.secondserving.data.network.AnalyticsEventCreate
import com.app.secondserving.data.network.ApiService
import com.app.secondserving.data.network.RetrofitClient
import com.app.secondserving.data.network.UserSegmentResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Repository de analytics. Por ahora hace fire-and-forget contra
 * POST /analytics/events; cualquier fallo de red queda en logcat.
 *
 * Trade-off consciente: la versión con cola offline (Room +
 * AnalyticsSyncWorker + Result.retry()) está planificada como siguiente
 * iteración — patrón del curso 15.EventualConnectivity.pdf y
 * 18.Multi_threading.pdf. Para esta entrega priorizamos que la pantalla
 * de T4.1 muestre datos reales del backend; los logs llegan cuando hay
 * red y se pierden si no hay (asumible para el demo, no para producción).
 *
 * Patrón curso: SupervisorJob para que un fallo en un log no cancele el
 * scope; Dispatchers.IO porque es una llamada de red (curso 18).
 */
class AnalyticsRepository(
    private val apiService: ApiService = RetrofitClient.authInstance,
    private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    fun logNotificationReceived(properties: Map<String, Any>? = null) {
        logEvent("notification_received", properties)
    }

    fun logNotificationOpened(properties: Map<String, Any>? = null) {
        logEvent("notification_opened", properties)
    }

    private fun logEvent(eventName: String, properties: Map<String, Any>?) {
        externalScope.launch {
            try {
                val event = AnalyticsEventCreate(
                    event_name = eventName,
                    properties = properties,
                    occurred_at = nowIso()
                )
                val response = apiService.postAnalyticsEvents(AnalyticsEventBatch(listOf(event)))
                if (!response.isSuccessful) {
                    Log.w(TAG, "Backend rejected $eventName: HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                // Sin cola offline (todavía): solo logueamos para diagnóstico.
                Log.w(TAG, "No pude enviar $eventName: ${e.message}")
            }
        }
    }

    suspend fun getUserSegment(): Result<UserSegmentResponse> {
        return try {
            val response = apiService.getUserSegment()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Result.Success(body)
                else Result.Error(IOException("Respuesta vacía del backend"))
            } else {
                Result.Error(IOException("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Devuelve el waste agrupado por categoría: por cada categoría, una lista
     * con las observaciones mensuales (value_lost_cop). Sirve directo a la
     * BQ T3.2: las observaciones alimentan el box plot.
     *
     * Si una categoría tiene una sola observación se grafica como punto;
     * con dos o más se calculan min/q1/mediana/q3/max (la pantalla decide
     * la representación visual).
     */
    suspend fun getWasteByCategory(months: Int = 3): Result<Map<String, List<Double>>> {
        return try {
            val response = apiService.getWasteAnalytics(months)
            if (response.isSuccessful) {
                val items = response.body() ?: emptyList()
                val grouped = items.groupBy { it.category.ifBlank { "Sin categoría" } }
                    .mapValues { (_, list) -> list.map { it.value_lost_cop } }
                Result.Success(grouped)
            } else {
                Result.Error(IOException("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun nowIso(): String =
        DateTimeFormatter.ISO_INSTANT.format(Instant.now())

    companion object {
        private const val TAG = "AnalyticsRepository"
    }
}
