package com.app.secondserving.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.app.secondserving.data.ConsumptionCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class ConsumptionUiState {
    object Empty   : ConsumptionUiState()
    data class Success(val stats: List<ConsumptionCache.CategoryStats>) : ConsumptionUiState()
}

class ConsumptionAnalyticsViewModel(
    private val cache: ConsumptionCache
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConsumptionUiState>(ConsumptionUiState.Empty)
    val uiState: StateFlow<ConsumptionUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        val stats = cache.getStats()
        _uiState.value = if (stats.isEmpty()) ConsumptionUiState.Empty
                         else ConsumptionUiState.Success(stats)
    }
}

class ConsumptionAnalyticsViewModelFactory(
    private val cache: ConsumptionCache
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConsumptionAnalyticsViewModel::class.java)) {
            return ConsumptionAnalyticsViewModel(cache) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
