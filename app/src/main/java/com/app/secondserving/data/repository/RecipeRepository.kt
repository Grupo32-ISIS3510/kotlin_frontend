package com.app.secondserving.data.repository

import com.app.secondserving.data.Result
import com.app.secondserving.data.local.AppDatabase
import com.app.secondserving.data.local.FoodItemDao
import com.app.secondserving.data.network.*
import kotlinx.coroutines.flow.first
import java.io.IOException

/**
 * Recipe Repository - Single Source of Truth using Edamam (Spanish support).
 */
class RecipeRepository(
    private val database: AppDatabase,
    private val apiService: ApiService = RetrofitClient.authInstance,
    private val edamamService: EdamamService = RetrofitClient.edamamInstance
) {
    private val foodItemDao: FoodItemDao = database.foodItemDao()

    // Credenciales de Edamam
    private val EDAMAM_APP_ID = "21d09eba"
    private val EDAMAM_APP_KEY = "73941ebfcc612661590a8a7725bf367b"
    private val EDAMAM_USER_ID = "arnulfo_user" // Identificador para el header Edamam-Account-User

    suspend fun getRecommendedRecipes(): Result<List<Recipe>> {
        return try {
            val inventory = foodItemDao.getAllItems().first()
            if (inventory.isEmpty()) return Result.Success(emptyList())

            // 1. Crear una consulta basada en los ingredientes del usuario
            // Tomamos los 2-3 ingredientes más cercanos a vencer para la búsqueda
            val searchTerms = inventory.take(3).joinToString(" ") { it.name }
            
            val response = edamamService.searchRecipes(
                query = searchTerms,
                appId = EDAMAM_APP_ID,
                appKey = EDAMAM_APP_KEY,
                userId = EDAMAM_USER_ID
            )

            if (response.isSuccessful) {
                val edamamRecipes = response.body()?.hits?.mapNotNull { hit ->
                    hit.recipe?.let { mapEdamamToRecipe(it, inventory.map { it.name }) }
                } ?: emptyList()
                
                Result.Success(edamamRecipes)
            } else {
                // Fallback: Si la búsqueda específica falla, intentamos con categorías
                val fallbackQuery = inventory.map { it.category }.distinct().take(2).joinToString(" ")
                val fallbackResponse = edamamService.searchRecipes(
                    query = fallbackQuery,
                    appId = EDAMAM_APP_ID,
                    appKey = EDAMAM_APP_KEY,
                    userId = EDAMAM_USER_ID
                )
                
                if (fallbackResponse.isSuccessful) {
                    val fallbackRecipes = fallbackResponse.body()?.hits?.mapNotNull { hit ->
                        hit.recipe?.let { mapEdamamToRecipe(it, inventory.map { it.name }) }
                    } ?: emptyList()
                    Result.Success(fallbackRecipes)
                } else {
                    Result.Error(IOException("Error Edamam: ${response.code()} ${response.errorBody()?.string()}"))
                }
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun mapEdamamToRecipe(er: EdamamRecipe, userIngredients: List<String>): Recipe {
        val matchedIngredients = mutableListOf<String>()
        val ingredients = er.ingredients?.map { ei ->
            val foodName = ei.food ?: ""
            if (userIngredients.any { it.contains(foodName, ignoreCase = true) || foodName.contains(it, ignoreCase = true) }) {
                matchedIngredients.add(foodName)
            }
            RecipeIngredient(
                name = foodName,
                quantity = ei.quantity?.toString() ?: "",
                unit = ei.measure
            )
        } ?: emptyList()

        return Recipe(
            id = er.uri?.hashCode() ?: 0,
            title = er.label ?: "Sin título",
            description = er.cuisineType?.firstOrNull() ?: er.dishType?.firstOrNull(),
            instructions = "Ver receta completa en: ${er.url}\n\nIngredientes necesarios:\n" + (er.ingredientLines?.joinToString("\n") ?: ""),
            ingredients = ingredients,
            matched_ingredients = matchedIngredients,
            soonest_expiry_days = (1..5).random(), // Simulado
            score = if (ingredients.isNotEmpty()) matchedIngredients.size.toDouble() / ingredients.size else 0.0
        )
    }

    suspend fun cookRecipe(recipeId: Int): Result<Unit> = Result.Success(Unit)
}
