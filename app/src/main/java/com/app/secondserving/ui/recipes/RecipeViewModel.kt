package com.app.secondserving.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.secondserving.data.repository.RecipeRepository
import com.app.secondserving.data.Result
import com.app.secondserving.data.network.Recipe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado unificado para la pantalla de Recetas.
 */
data class RecipesUiState(
    val recipes: List<Recipe> = emptyList(),
    val isLoading: Boolean = false,
    val isCooking: Boolean = false,
    val error: String? = null,
    val isEmpty: Boolean = false
)

/**
 * Estado específico para la acción de cocinar.
 */
sealed class CookUiState {
    object Idle : CookUiState()
    object Loading : CookUiState()
    object Success : CookUiState()
    data class Error(val message: String) : CookUiState()
}

class RecipeViewModel(private val repository: RecipeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipesUiState())
    val uiState: StateFlow<RecipesUiState> = _uiState.asStateFlow()

    private val _cookState = MutableStateFlow<CookUiState>(CookUiState.Idle)
    val cookState: StateFlow<CookUiState> = _cookState.asStateFlow()

    init {
        fetchRecipes()
    }

    fun fetchRecipes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isEmpty = false) }

            when (val result = repository.getRecommendedRecipes()) {
                is Result.Success -> {
                    // Filtramos recetas sin título o sin id porque el backend a veces
                    // devuelve recetas con metadata incompleta y mostrar tarjetas
                    // genéricas "Sin título" no aporta nada al usuario.
                    val validRecipes = result.data.filter {
                        !it.title.isNullOrBlank() && !it.id.isNullOrBlank()
                    }
                    if (validRecipes.isEmpty()) {
                        _uiState.update { it.copy(isLoading = false, isEmpty = true, recipes = emptyList()) }
                    } else {
                        // T2.3: Ranked list - Sort by score (descending) and then by expiry days (ascending)
                        val rankedRecipes = validRecipes.sortedWith(
                            compareByDescending<Recipe> { it.score ?: 0.0 }
                                .thenBy { it.soonest_expiry_days ?: Int.MAX_VALUE }
                        )
                        _uiState.update { it.copy(isLoading = false, recipes = rankedRecipes) }
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.exception.message ?: "Error desconocido")
                    }
                }
            }
        }
    }

    fun cookRecipe(recipeId: String) {
        viewModelScope.launch {
            _cookState.value = CookUiState.Loading
            _uiState.update { it.copy(isCooking = true) }
            
            when (val result = repository.cookRecipe(recipeId)) {
                is Result.Success -> {
                    _cookState.value = CookUiState.Success
                    _uiState.update { it.copy(isCooking = false) }
                    // Refrescar recetas después de cocinar para actualizar ingredientes emparejados
                    fetchRecipes()
                }
                is Result.Error -> {
                    val errorMsg = result.exception.message ?: "Error al cocinar receta"
                    _cookState.value = CookUiState.Error(errorMsg)
                    _uiState.update { 
                        it.copy(isCooking = false, error = errorMsg) 
                    }
                }
            }
        }
    }

    fun resetCookState() {
        _cookState.value = CookUiState.Idle
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // Registra que el usuario abrió el detalle de una receta. El backend lo
    // guarda en recipe_interactions con action="viewed". Es fire-and-forget:
    // no exponemos estado UI porque al usuario no le interesa el resultado
    // de este log; cualquier fallo de red queda silencioso (en Rama 2 esto
    // pasará por la cola offline de analytics_events para sobrevivir process death).
    fun viewRecipe(recipeId: String) {
        viewModelScope.launch {
            repository.viewRecipe(recipeId)
        }
    }
}
