package com.app.secondserving.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.secondserving.data.InventoryRepository
import com.app.secondserving.data.Result
import com.app.secondserving.data.network.SavingsAnalyticsResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.UnknownHostException
import java.time.LocalDate
import java.util.concurrent.TimeoutException

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val analytics: SavingsAnalyticsResponse) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(
    private val repository: InventoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Carga las analíticas de ahorro aplicando la siguiente política de caché:
     *
     * 1. Si ya hay un [HomeUiState.Success] en memoria (ViewModel vivo) y la caché
     *    persistente aún es válida → devuelve inmediatamente sin spinner ni red.
     * 2. Si hay caché persistente válida pero el ViewModel fue recreado → aplica los
     *    datos de caché al instante (sin spinner) y retorna.
     * 3. Si la caché fue invalidada (usuario consumió/descartó un alimento) o expiró
     *    (> 24 h) → muestra spinner y hace la petición de red.
     * 4. Con [forceRefresh] = true (botón "Reintentar") siempre va a la red.
     */
    fun loadSavings(month: Int? = null, year: Int? = null, forceRefresh: Boolean = false) {
        val now = LocalDate.now()
        val requestMonth = month ?: now.monthValue
        val requestYear  = year  ?: now.year

        if (!forceRefresh) {
            // ── Caso 1: datos ya en memoria y caché persistente todavía vigente ──
            if (_uiState.value is HomeUiState.Success &&
                repository.getCachedSavingsAnalytics(requestMonth, requestYear) != null
            ) return

            // ── Caso 2: sin datos en memoria, pero caché persistente válida ──
            val cached = repository.getCachedSavingsAnalytics(requestMonth, requestYear)
            if (cached != null) {
                _uiState.value = HomeUiState.Success(cached)
                return          // sin coroutine, respuesta instantánea
            }
        }

        // ── Caso 3 / 4: caché inválida o expirada → ir a la red ──
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            when (val result = repository.getSavingsAnalytics(requestMonth, requestYear)) {
                is Result.Success -> _uiState.value = HomeUiState.Success(result.data)
                is Result.Error   -> _uiState.value = HomeUiState.Error(
                    getUserFriendlyMessage(result.exception)
                )
            }
        }
    }

    private fun getUserFriendlyMessage(exception: Throwable): String {
        val rootCause = unwrapCause(exception)
        return when (rootCause) {
            is UnknownHostException ->
                "No hay conexión a internet. Verifica tu red e intenta de nuevo."
            is java.net.SocketTimeoutException ->
                "La conexión tardó demasiado. Verifica tu red e intenta de nuevo."
            is TimeoutException ->
                "La conexión tardó demasiado. Verifica tu red e intenta de nuevo."
            is java.net.ConnectException ->
                "No se pudo conectar al servidor. Intenta de nuevo más tarde."
            else -> {
                val msg = rootCause.message
                if (msg.isNullOrBlank() ||
                    msg.contains("HTTP") ||
                    msg.contains("Exception") ||
                    msg.contains("Error")
                ) {
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
}

class HomeViewModelFactory(
    private val repository: InventoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
