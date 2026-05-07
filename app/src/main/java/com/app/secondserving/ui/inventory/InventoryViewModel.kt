package com.app.secondserving.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.secondserving.data.ExpirationNotifier
import com.app.secondserving.data.InventoryRepository
import com.app.secondserving.data.Result
import com.app.secondserving.data.local.FoodItemEntity
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

class InventoryViewModel(
    private val repository: InventoryRepository,
    private val expirationNotifier: ExpirationNotifier? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<InventoryUiState>(InventoryUiState.Loading)
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _allItems = MutableStateFlow<List<InventoryItemUi>>(emptyList())
    val allItems: StateFlow<List<InventoryItemUi>> = _allItems.asStateFlow()
    private val _expiredItems = MutableStateFlow<List<InventoryItemUi>>(emptyList())
    val expiredItems: StateFlow<List<InventoryItemUi>> = _expiredItems.asStateFlow()
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

    fun loadInventory(forceRefresh: Boolean = false, showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading || _uiState.value !is InventoryUiState.Success) {
                _uiState.value = InventoryUiState.Loading
            }
            when (val result = repository.getInventory(forceRefresh = forceRefresh)) {
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
                    _expiredItems.value = _allItems.value
                        .filter { it.daysRemaining <= 0L }
                        .sortedBy { it.daysRemaining }
                    applyFilters()
                }
                is Result.Error -> {
                    _expiredItems.value = emptyList()
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
        expiryDate: String,
        price: Double? = null
    ) {
        viewModelScope.launch {
            _addItemState.value = AddItemUiState.Loading
            val request = InventoryItemRequest(
                name = name,
                category = category,
                quantity = quantity,
                purchase_date = purchaseDate,
                expiry_date = expiryDate,
                price = price
            )
            when (val result = repository.createInventoryItem(request)) {
                is Result.Success -> {
                    _addItemState.value = AddItemUiState.Success
                    expirationNotifier?.notifyImmediateExpiration(result.data)
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
            val created = mutableListOf<FoodItemEntity>()

            items.forEach { request ->
                when (val result = repository.createInventoryItem(request)) {
                    is Result.Success -> {
                        successCount++
                        created += result.data
                    }
                    is Result.Error -> lastError = result.exception
                }
            }

            if (successCount > 0) {
                _addItemState.value = AddItemUiState.Success
                expirationNotifier?.checkAndNotify(created)
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

    // ── Consume / Discard ────────────────────────────────────────────────────
    //
    // El usuario decide si un alimento se aprovechó (consume) o se perdió
    // (discard con razón). El backend distingue ambos casos en
    // inventory_events y eso alimenta tanto saved_cop / wasted_cop como la
    // BQ T3.2 (impacto de recetas en reducción de waste por categoría).

    private val _actionState = MutableStateFlow<ItemActionState>(ItemActionState.Idle)
    val actionState: StateFlow<ItemActionState> = _actionState.asStateFlow()

    fun consumeItem(itemId: String) {
        viewModelScope.launch {
            _actionState.value = ItemActionState.Loading
            when (val result = repository.consumeInventoryItem(itemId)) {
                is Result.Success -> {
                    _actionState.value = ItemActionState.Success
                    loadInventory(showLoading = false)
                }
                is Result.Error -> {
                    _actionState.value = ItemActionState.Error(getUserFriendlyMessage(result.exception))
                }
            }
        }
    }

    fun discardItem(itemId: String, reason: String) {
        viewModelScope.launch {
            _actionState.value = ItemActionState.Loading
            when (val result = repository.discardInventoryItem(itemId, reason)) {
                is Result.Success -> {
                    _actionState.value = ItemActionState.Success
                    loadInventory(showLoading = false)
                }
                is Result.Error -> {
                    _actionState.value = ItemActionState.Error(getUserFriendlyMessage(result.exception))
                }
            }
        }
    }

    fun resetActionState() {
        _actionState.value = ItemActionState.Idle
    }
}

sealed class ItemActionState {
    object Idle : ItemActionState()
    object Loading : ItemActionState()
    object Success : ItemActionState()
    data class Error(val message: String) : ItemActionState()
}

class InventoryViewModelFactory(
    private val repository: InventoryRepository,
    private val expirationNotifier: ExpirationNotifier? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryViewModel(repository, expirationNotifier) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
