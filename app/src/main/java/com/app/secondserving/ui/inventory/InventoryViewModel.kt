package com.app.secondserving.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.secondserving.data.InventoryRepository
import com.app.secondserving.data.Result
import com.app.secondserving.data.network.InventoryItemRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.UnknownHostException
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeoutException

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

    private val _allItems = MutableStateFlow<List<InventoryItemUi>>(emptyList())
    private val _addItemState = MutableStateFlow<AddItemUiState>(AddItemUiState.Idle)
    val addItemState: StateFlow<AddItemUiState> = _addItemState.asStateFlow()

    /**
     * Transforms a raw exception into a user-friendly message.
     */
    private fun getUserFriendlyMessage(exception: Throwable): String {
        val rootCause = unwrapCause(exception)
        return when (rootCause) {
            is UnknownHostException -> "No hay conexión a internet. Verifica tu red e intenta de nuevo."
            is java.net.SocketTimeoutException -> "La conexión tardó demasiado. Verifica tu red e intenta de nuevo."
            is TimeoutException -> "La conexión tardó demasiado. Verifica tu red e intenta de nuevo."
            is java.net.ConnectException -> "No se pudo conectar al servidor. Intenta de nuevo más tarde."
            else -> {
                val msg = rootCause.message
                if (msg.isNullOrBlank() || msg.contains("HTTP") || msg.contains("Exception") || msg.contains("Error")) {
                    "Ocurrió un error inesperado. Intenta de nuevo más tarde."
                } else {
                    msg
                }
            }
        }
    }

    private fun unwrapCause(throwable: Throwable): Throwable {
        var current: Throwable = throwable
        while (current.cause != null && current.cause !== current) {
            current = current.cause!!
        }
        return current
    }

    fun loadInventory() {
        viewModelScope.launch {
            _uiState.value = InventoryUiState.Loading
            when (val result = repository.getInventory()) {
                is Result.Success -> {
                    val today = LocalDate.now()
                    _allItems.value = result.data.map { item ->
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
                            quantity = item.quantity.toInt(),
                            daysRemaining = daysRemaining,
                            urgency = urgency
                        )
                    }
                    applyFilters()
                }
                is Result.Error -> {
                    _uiState.value = InventoryUiState.Error(getUserFriendlyMessage(result.exception))
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
        val items = _allItems.value
        val filtered = items.filter { item ->
            val matchesSearch = query.isEmpty() || item.name.lowercase().contains(query)
            val matchesCategory = category == "Todos" || item.category.equals(category, ignoreCase = true)
            matchesSearch && matchesCategory
        }
        _uiState.value = InventoryUiState.Success(filtered)
    }

    /**
     * Crea un solo item.
     */
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
                    _addItemState.value = AddItemUiState.Error(getUserFriendlyMessage(result.exception))
                }
            }
        }
    }

    /**
     * Anti-patrón resuelto: Manejo de carga masiva (Batch).
     * Crea múltiples items de forma eficiente y maneja el estado de carga global.
     */
    fun createInventoryItems(items: List<InventoryItemRequest>) {
        viewModelScope.launch {
            _addItemState.value = AddItemUiState.Loading
            var successCount = 0
            var lastError: Throwable? = null

            items.forEach { request ->
                when (val result = repository.createInventoryItem(request)) {
                    is Result.Success -> successCount++
                    is Result.Error -> lastError = result.exception
                }
            }

            if (successCount > 0) {
                _addItemState.value = AddItemUiState.Success
                loadInventory()
            } else if (lastError != null) {
                _addItemState.value = AddItemUiState.Error(getUserFriendlyMessage(lastError!!))
            } else {
                _addItemState.value = AddItemUiState.Idle
            }
        }
    }

    fun resetAddItemState() {
        _addItemState.value = AddItemUiState.Idle
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
