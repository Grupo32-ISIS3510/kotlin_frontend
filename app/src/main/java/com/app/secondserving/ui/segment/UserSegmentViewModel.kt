package com.app.secondserving.ui.segment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.secondserving.data.AnalyticsRepository
import com.app.secondserving.data.Result
import com.app.secondserving.data.network.UserSegmentResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado UI de la pantalla de segmento (BQ T4.1).
 *
 * Sealed UiState con Loading/Success/Error — patrón del curso de
 * Programming Components / Compose state holders.
 */
sealed class UserSegmentUiState {
    object Loading : UserSegmentUiState()
    data class Success(val data: UserSegmentResponse) : UserSegmentUiState()
    data class Error(val message: String) : UserSegmentUiState()
}

class UserSegmentViewModel(
    private val repository: AnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UserSegmentUiState>(UserSegmentUiState.Loading)
    val uiState: StateFlow<UserSegmentUiState> = _uiState.asStateFlow()

    init {
        loadSegment()
    }

    fun loadSegment() {
        viewModelScope.launch {
            _uiState.value = UserSegmentUiState.Loading
            when (val result = repository.getUserSegment()) {
                is Result.Success -> _uiState.value = UserSegmentUiState.Success(result.data)
                is Result.Error -> _uiState.value = UserSegmentUiState.Error(
                    result.exception.message ?: "Error desconocido"
                )
            }
        }
    }
}
