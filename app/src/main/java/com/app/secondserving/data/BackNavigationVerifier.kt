package com.app.secondserving.data

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SSOT para el seguimiento exclusivo de la navegación hacia atrás.
 * Cumple con SRP al no manejar lógica de negocio o registros.
 */
object BackNavigationVerifier {
    private val TAG = "BackNavigationVerifier"

    private val _isAddItemScreen = MutableStateFlow(false)
    val isAddItemScreen = _isAddItemScreen.asStateFlow()

    private val _isScanReceiptScreen = MutableStateFlow(false)
    val isScanReceiptScreen = _isScanReceiptScreen.asStateFlow()

    private val _isReviewScanScreen = MutableStateFlow(false)
    val isReviewScanScreen = _isReviewScanScreen.asStateFlow()

    private var firstBackScreen: String? = null

    private val job = SupervisorJob()

    suspend fun trackBackFromScanReceipt() = withContext(Dispatchers.IO) {
        _isScanReceiptScreen.value = true
        registerFirstBack("ScanReceiptScreen")
    }

    suspend fun trackBackFromReviewScan() = withContext(Dispatchers.IO) {
        _isReviewScanScreen.value = true
        registerFirstBack("ReviewScanScreen")
    }

    suspend fun trackBackFromAddItem() = withContext(Dispatchers.IO) {
        _isAddItemScreen.value = true
        registerFirstBack("AddItemScreen")
        
        val origin = firstBackScreen ?: "AddItemScreen"
        Log.i(TAG, "DATABASE_LOG: Usuario volvió desde AddItemScreen. Origen inicial: $origin")
    }

    @Synchronized
    private fun registerFirstBack(screenName: String) {
        if (firstBackScreen == null) {
            firstBackScreen = screenName
            Log.d(TAG, "Primera pantalla de retroceso registrada: $firstBackScreen")
        }
    }

    /**
     * Limpia procesos de tracking de navegación.
     */
    fun cancelAllTracking() {
        Log.d(TAG, "Cancelando tareas de tracking de navegación.")
        job.cancelChildren()
    }

    fun reset() {
        _isAddItemScreen.value = false
        _isScanReceiptScreen.value = false
        _isReviewScanScreen.value = false
        firstBackScreen = null
    }
}
