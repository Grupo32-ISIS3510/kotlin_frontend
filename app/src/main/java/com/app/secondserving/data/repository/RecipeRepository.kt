package com.app.secondserving.data.repository

import com.app.secondserving.data.Result
import com.app.secondserving.data.network.ApiService
import com.app.secondserving.data.network.Recipe
import com.app.secondserving.data.network.RecipeInteractionRequest
import com.app.secondserving.data.network.RetrofitClient
import java.io.IOException

/**
 * Recipe Repository — Single Source of Truth.
 *
 * Consume el endpoint /recipes/suggestions del backend del equipo, que ya
 * hace el matching con el inventario del usuario en el servidor y devuelve
 * los campos matched_ingredients, score y soonest_expiry_days ya calculados.
 *
 * Antes esto consultaba la API pública de Edamam (ver bloque comentado al
 * final del archivo). Lo cambié a backend porque:
 *   (a) Edamam usaba hash(uri) como id, incompatible con los UUID que usa
 *       /recipes/{id}/interact en el backend;
 *   (b) cookRecipe era un stub que devolvía Success sin hacer red, así que
 *       la tabla recipe_interactions nunca se poblaba — eso rompe la BQ T4.1
 *       (Passive vs Proactive users) porque siempre saldría "passive";
 *   (c) el equipo Flutter ya consume el backend; el curso exige paridad de
 *       features entre las dos plataformas del pair;
 *   (d) Edamam no aparece en el material del curso ISIS-3510, así que no
 *       sería defendible en el viva voce.
 *
 * Patrón: Repository como SSOT (curso 11.2-SoftwareArchitecture).
 */
class RecipeRepository(
    private val apiService: ApiService = RetrofitClient.authInstance
) {

    suspend fun getRecommendedRecipes(): Result<List<Recipe>> {
        return try {
            val response = apiService.getRecipes()
            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Error(IOException("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun cookRecipe(recipeId: String): Result<Unit> = interact(recipeId, "cooked")

    suspend fun viewRecipe(recipeId: String): Result<Unit> = interact(recipeId, "viewed")

    private suspend fun interact(recipeId: String, action: String): Result<Unit> {
        return try {
            val response = apiService.interactWithRecipe(
                recipeId,
                RecipeInteractionRequest(action = action)
            )
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error(IOException("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

/* ----------------------------------------------------------------------
 * Implementación anterior basada en Edamam (deshabilitada el 2026-04-24).
 * Se conserva comentada como referencia histórica para el viva voce.
 *
 * Las credenciales reales de Edamam (APP_ID y APP_KEY) las reemplacé por
 * placeholders aquí porque dejarlas literales en el HEAD las seguiría
 * exponiendo aunque ya estén en el git history del repo público.
 * ----------------------------------------------------------------------
 *
 * import com.app.secondserving.data.local.AppDatabase
 * import com.app.secondserving.data.local.FoodItemDao
 * import com.app.secondserving.data.network.EdamamRecipe
 * import com.app.secondserving.data.network.EdamamService
 * import com.app.secondserving.data.network.RecipeIngredient
 * import kotlinx.coroutines.flow.first
 *
 * class RecipeRepositoryEdamam(
 *     private val database: AppDatabase,
 *     private val apiService: ApiService = RetrofitClient.authInstance,
 *     private val edamamService: EdamamService = RetrofitClient.edamamInstance
 * ) {
 *     private val foodItemDao: FoodItemDao = database.foodItemDao()
 *
 *     // Credenciales originales reemplazadas por placeholders (ver nota de arriba).
 *     private val EDAMAM_APP_ID = "<REDACTED>"
 *     private val EDAMAM_APP_KEY = "<REDACTED>"
 *     private val EDAMAM_USER_ID = "arnulfo_user"
 *
 *     // Buscaba recetas en Edamam usando los nombres de los productos del
 *     // inventario local. El matching de ingredientes era client-side
 *     // comparando strings con contains() en ambas direcciones.
 *     suspend fun getRecommendedRecipes(): Result<List<Recipe>> {
 *         return try {
 *             val inventory = foodItemDao.getAllItems().first()
 *             if (inventory.isEmpty()) return Result.Success(emptyList())
 *
 *             val topIngredients = inventory.take(3).map { it.name.split(" ")[0] }
 *             val allFoundRecipes = mutableListOf<Recipe>()
 *
 *             for (ingredient in topIngredients) {
 *                 val response = edamamService.searchRecipes(
 *                     query = ingredient,
 *                     appId = EDAMAM_APP_ID,
 *                     appKey = EDAMAM_APP_KEY,
 *                     userId = EDAMAM_USER_ID,
 *                     lang = "es"
 *                 )
 *                 if (response.isSuccessful) {
 *                     val recipes = response.body()?.hits?.mapNotNull { hit ->
 *                         hit.recipe?.let { mapEdamamToRecipe(it, inventory.map { it.name }) }
 *                     } ?: emptyList()
 *                     allFoundRecipes.addAll(recipes)
 *                 }
 *             }
 *
 *             // Fallback: si no hubo matches por nombre, probar con la categoría.
 *             if (allFoundRecipes.isEmpty()) {
 *                 val categories = inventory.map { it.category }.distinct().take(2)
 *                 for (cat in categories) {
 *                     val fallbackResponse = edamamService.searchRecipes(
 *                         query = cat,
 *                         appId = EDAMAM_APP_ID,
 *                         appKey = EDAMAM_APP_KEY,
 *                         userId = EDAMAM_USER_ID,
 *                         lang = "es"
 *                     )
 *                     if (fallbackResponse.isSuccessful) {
 *                         val fallbackRecipes = fallbackResponse.body()?.hits?.mapNotNull { hit ->
 *                             hit.recipe?.let { mapEdamamToRecipe(it, inventory.map { it.name }) }
 *                         } ?: emptyList()
 *                         allFoundRecipes.addAll(fallbackRecipes)
 *                     }
 *                 }
 *             }
 *
 *             Result.Success(allFoundRecipes.distinctBy { it.id })
 *         } catch (e: Exception) {
 *             Result.Error(e)
 *         }
 *     }
 *
 *     private fun mapEdamamToRecipe(er: EdamamRecipe, userIngredients: List<String>): Recipe {
 *         val matchedIngredients = mutableListOf<String>()
 *         val ingredients = er.ingredients?.map { ei ->
 *             val foodName = ei.food ?: ""
 *             if (userIngredients.any { it.contains(foodName, ignoreCase = true) || foodName.contains(it, ignoreCase = true) }) {
 *                 matchedIngredients.add(foodName)
 *             }
 *             RecipeIngredient(
 *                 name = foodName,
 *                 quantity = ei.quantity?.toString() ?: "",
 *                 unit = ei.measure
 *             )
 *         } ?: emptyList()
 *
 *         return Recipe(
 *             id = er.uri ?: "",
 *             title = er.label ?: "Sin título",
 *             description = er.cuisineType?.firstOrNull() ?: er.dishType?.firstOrNull(),
 *             instructions = "Ver receta completa en: " + er.url + "\n\nIngredientes necesarios:\n" + (er.ingredientLines?.joinToString("\n") ?: ""),
 *             ingredients = ingredients,
 *             matched_ingredients = matchedIngredients,
 *             soonest_expiry_days = (1..5).random(),
 *             score = if (ingredients.isNotEmpty()) matchedIngredients.size.toDouble() / ingredients.size else 0.0
 *         )
 *     }
 *
 *     // cookRecipe era un stub vacío — esa fue una de las razones del refactor:
 *     // el backend nunca recibía señales de cocción y la tabla recipe_interactions
 *     // quedaba sin datos.
 *     suspend fun cookRecipe(recipeId: String): Result<Unit> = Result.Success(Unit)
 * }
 */
