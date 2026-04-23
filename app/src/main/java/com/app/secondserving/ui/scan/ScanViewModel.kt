package com.app.secondserving.ui.scan

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.secondserving.data.scan.ReceiptScanner
import com.app.secondserving.data.ShelfLifePredictor
import com.app.secondserving.data.InventoryRepository
import com.app.secondserving.data.network.InventoryItemRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Estado unificado para el flujo de escaneo y revisión.
 */
sealed class ScanUiState {
    object Idle : ScanUiState()
    object Processing : ScanUiState()
    data class Success(
        val items: List<EditableScannedItem>,
        val purchaseDate: String?
    ) : ScanUiState()
    data class Error(val message: String) : ScanUiState()
}

/**
 * Estado UI persistente para la lista de items durante la revisión.
 * Esto asegura el SSOT para la pantalla de revisión.
 */
data class ScanReviewState(
    val items: List<EditableScannedItem> = emptyList(),
    val detectedPurchaseDate: String? = null,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val saveSuccess: Boolean = false
)

data class EditableScannedItem(
    val name: String,
    val category: String,
    val quantity: Int,
    val price: Double?,
    val purchaseDate: String,
    val expiryDate: String
)

class ScanViewModel(
    private val scanner: ReceiptScanner,
    private val inventoryRepository: InventoryRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _reviewState = MutableStateFlow(ScanReviewState())
    val reviewState: StateFlow<ScanReviewState> = _reviewState.asStateFlow()

    fun scanReceipt(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ScanUiState.Processing
            try {
                val result = scanner.scanReceipt(uri)
                if (result.error != null) {
                    _uiState.value = ScanUiState.Error(result.error)
                } else if (result.items.isEmpty()) {
                    // Alerta cuando no se detecta nada correctamente
                    _uiState.value = ScanUiState.Error("No se detectaron productos en la factura. Intenta tomar la foto con mejor iluminación o más cerca.")
                } else {
                    val purchaseDate = result.purchaseDate ?: LocalDate.now().toString()
                    val editableItems = result.items.map { 
                        EditableScannedItem(
                            name = it.name,
                            category = it.category,
                            quantity = 1,
                            price = it.price,
                            purchaseDate = purchaseDate,
                            expiryDate = ShelfLifePredictor.predictExpiryDate(purchaseDate, it.category)
                        )
                    }
                    
                    // Actualizar el estado de revisión (SSOT)
                    _reviewState.update { it.copy(
                        items = editableItems,
                        detectedPurchaseDate = result.purchaseDate,
                        saveSuccess = false,
                        saveError = null
                    ) }
                    
                    _uiState.value = ScanUiState.Success(editableItems, result.purchaseDate)
                }
            } catch (e: Exception) {
                _uiState.value = ScanUiState.Error("Error procesando imagen: ${e.message}")
            }
        }
    }

    fun updateItem(index: Int, updatedItem: EditableScannedItem) {
        _reviewState.update { state ->
            val newList = state.items.toMutableList()
            if (index in newList.indices) {
                newList[index] = updatedItem
                state.copy(items = newList)
            } else state
        }
    }

    fun removeItem(index: Int) {
        _reviewState.update { state ->
            val newList = state.items.toMutableList()
            if (index in newList.indices) {
                newList.removeAt(index)
                state.copy(items = newList)
            } else state
        }
    }

    fun getInventoryRequests(): List<InventoryItemRequest> {
        return _reviewState.value.items.map { item ->
            InventoryItemRequest(
                name = item.name,
                category = item.category,
                quantity = item.quantity.toDouble(),
                purchase_date = item.purchaseDate,
                expiry_date = item.expiryDate
            )
        }
    }

    fun saveScannedItems() {
        val requests = getInventoryRequests()

        if (requests.isEmpty()) return

        viewModelScope.launch {
            _reviewState.update { it.copy(isSaving = true, saveError = null) }
            try {
                // Aquí iría la llamada al repositorio si no es nulo
                // inventoryRepository?.addItems(requests)
                _reviewState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _reviewState.update { it.copy(isSaving = false, saveError = e.message ?: "Error al guardar") }
            }
        }
    }

    fun resetState() {
        _uiState.value = ScanUiState.Idle
    }
    
    fun resetReviewState() {
        _reviewState.value = ScanReviewState()
    }

    override fun onCleared() {
        super.onCleared()
        scanner.close()
    }
}

class ScanViewModelFactory(
    private val scanner: ReceiptScanner,
    private val inventoryRepository: InventoryRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScanViewModel(scanner, inventoryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
