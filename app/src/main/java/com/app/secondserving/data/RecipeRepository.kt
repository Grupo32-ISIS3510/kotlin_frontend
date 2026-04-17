package com.app.secondserving.data

import com.app.secondserving.data.network.ApiService
import com.app.secondserving.data.network.Recipe
import com.app.secondserving.data.network.RetrofitClient
import java.io.IOException

class RecipeRepository(
    private val apiService: ApiService = RetrofitClient.authInstance
) {
    suspend fun getRecommendedRecipes(): Result<List<Recipe>> {
        return try {
            val response = apiService.getRecipes()
            if (response.isSuccessful) {
                val recipeResponse = response.body()
                val recipes = recipeResponse?.recipes
                if (recipes != null) {
                    Result.Success(recipes)
                } else {
                    // Si el objeto existe pero la lista es nula, devolvemos lista vacía
                    Result.Success(emptyList())
                }
            } else {
                Result.Error(IOException("Error fetching recipes: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun cookRecipe(recipeId: Int): Result<Unit> {
        return try {
            val response = apiService.cookRecipe(recipeId)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error(IOException("Error cooking recipe: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
