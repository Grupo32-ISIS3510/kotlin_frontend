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
                appKey = EDAMAM_APP_KEY
            )

            if (response.isSuccessful) {
                val edamamRecipes = response.body()?.hits?.map { hit ->
                    mapEdamamToRecipe(hit.recipe, inventory.map { it.name })
                } ?: emptyList()
                
                Result.Success(edamamRecipes)
            } else {
                // Fallback: Si la búsqueda específica falla, intentamos con categorías
                val fallbackQuery = inventory.map { it.category }.distinct().take(2).joinToString(" ")
                val fallbackResponse = edamamService.searchRecipes(
                    query = fallbackQuery,
                    appId = EDAMAM_APP_ID,
                    appKey = EDAMAM_APP_KEY
                )
                
                if (fallbackResponse.isSuccessful) {
                    val fallbackRecipes = fallbackResponse.body()?.hits?.map { hit ->
                        mapEdamamToRecipe(hit.recipe, inventory.map { it.name })
                    } ?: emptyList()
                    Result.Success(fallbackRecipes)
                } else {
                    Result.Error(IOException("Error Edamam: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun mapEdamamToRecipe(er: EdamamRecipe, userIngredients: List<String>): Recipe {
        val matchedIngredients = mutableListOf<String>()
        val ingredients = er.ingredients.map { ei ->
            if (userIngredients.any { it.contains(ei.food, ignoreCase = true) || ei.food.contains(it, ignoreCase = true) }) {
                matchedIngredients.add(ei.food)
            }
            RecipeIngredient(
                name = ei.food,
                quantity = ei.quantity.toString(),
                unit = ei.measure
            )
        }

        return Recipe(
            id = er.uri.hashCode(),
            title = er.label,
            description = er.cuisineType?.firstOrNull() ?: er.dishType?.firstOrNull(),
            instructions = "Ver receta completa en: ${er.url}\n\nIngredientes necesarios:\n" + er.ingredientLines.joinToString("\n"),
            ingredients = ingredients,
            matched_ingredients = matchedIngredients,
            soonest_expiry_days = (1..5).random(), // Simulado
            score = if (ingredients.isNotEmpty()) matchedIngredients.size.toDouble() / ingredients.size else 0.0
        )
    }

    suspend fun cookRecipe(recipeId: Int): Result<Unit> = Result.Success(Unit)
}
