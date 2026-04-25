package com.app.secondserving.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * SSOT para métricas y registro de productos.
 * Maneja el conteo por categoría solicitado y el seguimiento de éxitos.
 */
object ProductRegistryManager {
    private val TAG = "ProductRegistryManager"

    private val _categoryCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val categoryCounts = _categoryCounts.asStateFlow()

    private val _totalSuccess = MutableStateFlow(0)
    val totalSuccess = _totalSuccess.asStateFlow()

    /**
     * Función suspendida que registra un producto y actualiza el conteo por categoría.
     * Verifica la red antes de proceder (SRP).
     */
    suspend fun registerProduct(category: String, networkMonitor: NetworkMonitor?) = withContext(Dispatchers.IO) {
        val isOnline = networkMonitor?.isOnline?.first() ?: true
        if (!isOnline) {
            Log.w(TAG, "DATABASE_LOG: Registro de producto omitido por falta de conexión.")
            return@withContext
        }

        _categoryCounts.update { currentMap ->
            val count = currentMap[category] ?: 0
            currentMap + (category to (count + 1))
        }
        Log.d(TAG, "DATABASE_LOG (IO): Producto registrado en categoría: $category. Total: ${_categoryCounts.value[category]}")
    }

    /**
     * Incrementa el contador global de éxitos.
     */
    fun markSuccess() {
        _totalSuccess.value += 1
        Log.d(TAG, "Éxito registrado en Manager. Total: ${_totalSuccess.value}")
    }

    fun resetSession() {
        _categoryCounts.value = emptyMap()
        Log.d(TAG, "Sesión de registro reseteada.")
    }
}
