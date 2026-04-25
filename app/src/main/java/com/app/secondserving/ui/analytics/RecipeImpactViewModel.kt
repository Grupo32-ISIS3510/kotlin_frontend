package com.app.secondserving.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.secondserving.data.AnalyticsRepository
import com.app.secondserving.data.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RecipeImpactUiState {
    object Loading : RecipeImpactUiState()
    data class Success(val byCategory: Map<String, List<Double>>) : RecipeImpactUiState()
    data class Error(val message: String) : RecipeImpactUiState()
}

class RecipeImpactViewModel(
    private val repository: AnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecipeImpactUiState>(RecipeImpactUiState.Loading)
    val uiState: StateFlow<RecipeImpactUiState> = _uiState.asStateFlow()

    init {
        loadImpact()
    }

    fun loadImpact() {
        viewModelScope.launch {
            _uiState.value = RecipeImpactUiState.Loading
            when (val result = repository.getWasteByCategory(months = 3)) {
                is Result.Success -> _uiState.value = RecipeImpactUiState.Success(result.data)
                is Result.Error -> _uiState.value = RecipeImpactUiState.Error(
                    result.exception.message ?: "Error desconocido"
                )
            }
        }
    }
}

class RecipeImpactViewModelFactory(
    private val repository: AnalyticsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecipeImpactViewModel::class.java)) {
            return RecipeImpactViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
