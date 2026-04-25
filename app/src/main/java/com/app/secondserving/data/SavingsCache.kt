package com.app.secondserving.data

import android.content.Context
import android.content.SharedPreferences
import com.app.secondserving.data.network.SavingsAnalyticsResponse

/**
 * Caché de analíticas de ahorro basado en SharedPreferences.
 *
 * Política:
 *  - TTL de 24 horas desde la última carga exitosa.
 *  - Se invalida inmediatamente al consumir o descartar un alimento
 *    (deleteInventoryItem / updateInventoryItem en el repositorio).
 *  - La clave de invalidación es poner el timestamp a 0.
 *
 * Se usan String para los valores Double para no perder precisión al
 * pasar por Float (valores COP pueden superar 9 dígitos).
 */
class SavingsCache(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "savings_analytics_cache"
        private const val TTL_MS = 24L * 60 * 60 * 1000   // 24 horas

        private const val KEY_SAVED_COP   = "saved_cop"
        private const val KEY_WASTED_COP  = "wasted_cop"
        private const val KEY_PERIOD      = "period"
        private const val KEY_MONTH       = "cached_month"
        private const val KEY_YEAR        = "cached_year"
        private const val KEY_CACHED_AT   = "cached_at_ms"
    }

    /**
     * Devuelve los datos en caché si son válidos para el mes/año solicitado.
     * Retorna null en caso contrario (caché expirada, invalidada o de otro período).
     *
     * Es síncrono: las SharedPreferences ya están cargadas en memoria tras
     * la primera inicialización, por lo que es seguro llamar desde el hilo principal.
     */
    fun get(month: Int, year: Int): SavingsAnalyticsResponse? {
        val cachedMonth = prefs.getInt(KEY_MONTH, -1)
        val cachedYear  = prefs.getInt(KEY_YEAR, -1)
        val cachedAt    = prefs.getLong(KEY_CACHED_AT, 0L)

        if (cachedMonth != month || cachedYear != year) return null
        if (System.currentTimeMillis() - cachedAt > TTL_MS) return null

        val savedCop  = prefs.getString(KEY_SAVED_COP, null)?.toDoubleOrNull()  ?: return null
        val wastedCop = prefs.getString(KEY_WASTED_COP, null)?.toDoubleOrNull() ?: return null
        val period    = prefs.getString(KEY_PERIOD, null) ?: return null

        return SavingsAnalyticsResponse(
            saved_cop  = savedCop,
            wasted_cop = wastedCop,
            period     = period
        )
    }

    /**
     * Almacena los datos obtenidos de la red junto con su timestamp.
     */
    fun put(month: Int, year: Int, response: SavingsAnalyticsResponse) {
        prefs.edit()
            .putString(KEY_SAVED_COP,  response.saved_cop.toString())
            .putString(KEY_WASTED_COP, response.wasted_cop.toString())
            .putString(KEY_PERIOD,     response.period)
            .putInt(KEY_MONTH,         month)
            .putInt(KEY_YEAR,          year)
            .putLong(KEY_CACHED_AT,    System.currentTimeMillis())
            .apply()
    }

    /**
     * Invalida la caché forzando que el próximo acceso vaya a la red.
     * Se llama cuando el usuario consume o descarta un alimento.
     */
    fun invalidate() {
        prefs.edit().putLong(KEY_CACHED_AT, 0L).apply()
    }

    /**
     * Borra completamente la caché. Se llama al cerrar sesión para que el
     * siguiente usuario no vea datos del anterior.
     */
    fun clear() {
        prefs.edit().clear().apply()
    }

    /** Indica si la caché está vigente para el período dado. */
    fun isValid(month: Int, year: Int): Boolean = get(month, year) != null
}
