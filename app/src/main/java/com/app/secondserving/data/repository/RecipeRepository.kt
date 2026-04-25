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

            // 1. Obtener los nombres de los 3 productos más próximos a vencer y limpiarlos
            // Tomamos la primera palabra para evitar marcas que confundan a la API (ej: "Leche Parmalat" -> "Leche")
            val topIngredients = inventory.take(3).map { it.name.split(" ")[0] }
            
            val allFoundRecipes = mutableListOf<Recipe>()

            // 2. Buscar recetas para cada ingrediente individualmente para maximizar resultados
            for (ingredient in topIngredients) {
                val response = edamamService.searchRecipes(
                    query = ingredient,
                    appId = EDAMAM_APP_ID,
                    appKey = EDAMAM_APP_KEY,
                    userId = EDAMAM_USER_ID,
                    lang = "es"
                )

                if (response.isSuccessful) {
                    val recipes = response.body()?.hits?.mapNotNull { hit ->
                        hit.recipe?.let { mapEdamamToRecipe(it, inventory.map { it.name }) }
                    } ?: emptyList()
                    allFoundRecipes.addAll(recipes)
                }
            }

            // 3. Fallback: Si no hay resultados con nombres, intentamos con categorías
            if (allFoundRecipes.isEmpty()) {
                val categories = inventory.map { it.category }.distinct().take(2)
                for (cat in categories) {
                    val fallbackResponse = edamamService.searchRecipes(
                        query = cat,
                        appId = EDAMAM_APP_ID,
                        appKey = EDAMAM_APP_KEY,
                        userId = EDAMAM_USER_ID,
                        lang = "es"
                    )
                    
                    if (fallbackResponse.isSuccessful) {
                        val fallbackRecipes = fallbackResponse.body()?.hits?.mapNotNull { hit ->
                            hit.recipe?.let { mapEdamamToRecipe(it, inventory.map { it.name }) }
                        } ?: emptyList()
                        allFoundRecipes.addAll(fallbackRecipes)
                    }
                }
            }
            
            // Devolver lista sin duplicados
            Result.Success(allFoundRecipes.distinctBy { it.id })

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
            id = er.uri ?: "",
            title = er.label ?: "Sin título",
            description = er.cuisineType?.firstOrNull() ?: er.dishType?.firstOrNull(),
            instructions = "Ver receta completa en: ${er.url}\n\nIngredientes necesarios:\n" + (er.ingredientLines?.joinToString("\n") ?: ""),
            ingredients = ingredients,
            matched_ingredients = matchedIngredients,
            soonest_expiry_days = (1..5).random(), // Simulado
            score = if (ingredients.isNotEmpty()) matchedIngredients.size.toDouble() / ingredients.size else 0.0
        )
    }

    suspend fun cookRecipe(recipeId: String): Result<Unit> = Result.Success(Unit)
}
