package com.app.secondserving.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.secondserving.data.InventoryRepository
import com.app.secondserving.data.Result
import com.app.secondserving.data.ScannedItem
import com.app.secondserving.data.ShelfLifePredictor
import com.app.secondserving.data.network.InventoryItemRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

enum class Urgency { RED, YELLOW, GREEN }

data class InventoryItemUi(
    val id: String,
    val name: String,
    val category: String,
    val quantity: Int,
    val daysRemaining: Long,
    val urgency: Urgency
)

sealed class InventoryUiState {
    object Loading : InventoryUiState()
    data class Success(val items: List<InventoryItemUi>) : InventoryUiState()
    data class Error(val message: String) : InventoryUiState()
}

sealed class AddItemUiState {
    object Idle : AddItemUiState()
    object Loading : AddItemUiState()
    object Success : AddItemUiState()
    data class Error(val message: String) : AddItemUiState()
}

class InventoryViewModel(private val repository: InventoryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<InventoryUiState>(InventoryUiState.Loading)
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private var allItems: List<InventoryItemUi> = emptyList()
    private val _addItemState = MutableStateFlow<AddItemUiState>(AddItemUiState.Idle)
    val addItemState: StateFlow<AddItemUiState> = _addItemState.asStateFlow()

    fun loadInventory() {
        viewModelScope.launch {
            _uiState.value = InventoryUiState.Loading
            when (val result = repository.getInventory()) {
                is Result.Success -> {
                    val today = LocalDate.now()
                    allItems = result.data.map { item ->
                        val daysRemaining = try {
                            val expiry = LocalDate.parse(item.expiryDate)
                            ChronoUnit.DAYS.between(today, expiry)
                        } catch (e: DateTimeParseException) {
                            0L
                        }
                        val urgency = when {
                            daysRemaining < 3 -> Urgency.RED
                            daysRemaining <= 7 -> Urgency.YELLOW
                            else -> Urgency.GREEN
                        }
                        InventoryItemUi(
                            id = item.id,
                            name = item.name,
                            category = item.category,
                            quantity = item.quantity,
                            daysRemaining = daysRemaining,
                            urgency = urgency
                        )
                    }
                    applyFilters()
                }
                is Result.Error -> {
                    _uiState.value = InventoryUiState.Error(
                        result.exception.message ?: "Error desconocido"
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
        applyFilters()
    }

    private fun applyFilters() {
        val query = _searchQuery.value.trim().lowercase()
        val category = _selectedCategory.value
        val filtered = allItems.filter { item ->
            val matchesSearch = query.isEmpty() || item.name.lowercase().contains(query)
            val matchesCategory = category == "Todos" || item.category.equals(category, ignoreCase = true)
            matchesSearch && matchesCategory
        }
        _uiState.value = InventoryUiState.Success(filtered)
    }

    fun createInventoryItem(
        name: String,
        category: String,
        quantity: Double,
        purchaseDate: String,
        expiryDate: String
    ) {
        viewModelScope.launch {
            _addItemState.value = AddItemUiState.Loading
            val request = InventoryItemRequest(
                name = name,
                category = category,
                quantity = quantity,
                purchase_date = purchaseDate,
                expiry_date = expiryDate
            )
            when (val result = repository.createInventoryItem(request)) {
                is Result.Success -> {
                    _addItemState.value = AddItemUiState.Success
                    loadInventory()
                }
                is Result.Error -> {
                    _addItemState.value = AddItemUiState.Error(
                        result.exception.message ?: "Error desconocido"
                    )
                }
            }
        }
    }

    fun resetAddItemState() {
        _addItemState.value = AddItemUiState.Idle
    }

    fun createInventoryItemsBulk(
        items: List<ScannedItem>,
        purchaseDate: String?
    ) {
        viewModelScope.launch {
            _addItemState.value = AddItemUiState.Loading
            
            val today = purchaseDate ?: LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
            var successCount = 0
            var errorCount = 0
            
            for (scannedItem in items) {
                try {
                    val request = InventoryItemRequest(
                        name = scannedItem.name,
                        category = scannedItem.category,
                        quantity = 1.0,
                        purchase_date = today,
                        expiry_date = ShelfLifePredictor.predictExpiryDate(today, scannedItem.category)
                    )
                    when (repository.createInventoryItem(request)) {
                        is Result.Success -> successCount++
                        is Result.Error -> errorCount++
                    }
                } catch (e: Exception) {
                    errorCount++
                }
            }
            
            if (errorCount == 0) {
                _addItemState.value = AddItemUiState.Success
                loadInventory()
            } else if (successCount > 0) {
                // Éxito parcial: algunos items se agregaron
                android.util.Log.w(
                    "InventoryViewModel",
                    "Bulk create: $successCount exitosos, $errorCount fallidos"
                )
                _addItemState.value = AddItemUiState.Success
                loadInventory()
            } else {
                _addItemState.value = AddItemUiState.Error("No se pudieron agregar los productos")
            }
        }
    }
}

class InventoryViewModelFactory(private val repository: InventoryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
