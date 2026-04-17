package com.app.secondserving.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.secondserving.data.RecipeRepository
import com.app.secondserving.data.Result
import com.app.secondserving.data.network.Recipe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RecipesUiState {
    object Loading : RecipesUiState()
    data class Success(val recipes: List<Recipe>) : RecipesUiState()
    data class Error(val message: String) : RecipesUiState()
    object Empty : RecipesUiState()
}

sealed class CookUiState {
    object Idle : CookUiState()
    object Loading : CookUiState()
    object Success : CookUiState()
    data class Error(val message: String) : CookUiState()
}

class RecipeViewModel(private val repository: RecipeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<RecipesUiState>(RecipesUiState.Loading)
    val uiState: StateFlow<RecipesUiState> = _uiState.asStateFlow()

    private val _cookState = MutableStateFlow<CookUiState>(CookUiState.Idle)
    val cookState: StateFlow<CookUiState> = _cookState.asStateFlow()

    init {
        fetchRecipes()
    }

    fun fetchRecipes() {
        viewModelScope.launch {
            _uiState.value = RecipesUiState.Loading
            when (val result = repository.getRecommendedRecipes()) {
                is Result.Success -> {
                    if (result.data.isEmpty()) {
                        _uiState.value = RecipesUiState.Empty
                    } else {
                        _uiState.value = RecipesUiState.Success(result.data)
                    }
                }
                is Result.Error -> {
                    _uiState.value = RecipesUiState.Error(result.exception.message ?: "Unknown error")
                }
            }
        }
    }

    fun cookRecipe(recipeId: Int) {
        viewModelScope.launch {
            _cookState.value = CookUiState.Loading
            when (val result = repository.cookRecipe(recipeId)) {
                is Result.Success -> {
                    _cookState.value = CookUiState.Success
                    // Refresh recipes after cooking to update matched ingredients
                    fetchRecipes()
                }
                is Result.Error -> {
                    _cookState.value = CookUiState.Error(result.exception.message ?: "Error cooking recipe")
                }
            }
        }
    }

    fun resetCookState() {
        _cookState.value = CookUiState.Idle
    }
}
