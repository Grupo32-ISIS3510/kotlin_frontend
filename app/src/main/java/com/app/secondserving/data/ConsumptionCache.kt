package com.app.secondserving.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Caché local de frecuencia de consumo por categoría — BQ T4.2.
 *
 * Guarda cuántas veces el usuario agregó y consumió/descartó alimentos
 * por categoría. Se usa como fuente local para la pantalla ConsumptionAnalyticsScreen.
 * Los mismos eventos se envían al backend vía AnalyticsRepository.logEvent()
 * para el registro servidor.
 *
 * Patrón idéntico a SavingsCache: SharedPreferences con métodos síncronos
 * (los datos ya están en memoria tras la primera apertura).
 */
class ConsumptionCache(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "consumption_analytics_cache"
        private const val PREFIX_CONSUMED = "consumed_"
        private const val PREFIX_ADDED    = "added_"
    }

    data class CategoryStats(
        val category: String,
        val consumedCount: Int,
        val addedCount: Int
    )

    /** Incrementa el contador de consumos para una categoría. */
    fun incrementConsumed(category: String) {
        val key = PREFIX_CONSUMED + category.lowercase()
        val current = prefs.getInt(key, 0)
        prefs.edit().putInt(key, current + 1).apply()
    }

    /** Incrementa el contador de agregados para una categoría. */
    fun incrementAdded(category: String) {
        val key = PREFIX_ADDED + category.lowercase()
        val current = prefs.getInt(key, 0)
        prefs.edit().putInt(key, current + 1).apply()
    }

    /**
     * Devuelve las estadísticas de todas las categorías registradas,
     * ordenadas por consumedCount descendente.
     */
    fun getStats(): List<CategoryStats> {
        val all = prefs.all
        val categories = all.keys
            .filter { it.startsWith(PREFIX_CONSUMED) || it.startsWith(PREFIX_ADDED) }
            .map { key ->
                when {
                    key.startsWith(PREFIX_CONSUMED) -> key.removePrefix(PREFIX_CONSUMED)
                    else -> key.removePrefix(PREFIX_ADDED)
                }
            }
            .distinct()

        return categories.map { cat ->
            CategoryStats(
                category     = cat,
                consumedCount = prefs.getInt(PREFIX_CONSUMED + cat, 0),
                addedCount    = prefs.getInt(PREFIX_ADDED    + cat, 0)
            )
        }.sortedByDescending { it.consumedCount }
    }

    /** Borra todos los datos (se llama al cerrar sesión). */
    fun clear() {
        prefs.edit().clear().apply()
    }
}
