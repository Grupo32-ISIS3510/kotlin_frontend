package com.app.secondserving.data.network

// Cliente de la API pública de Edamam para recetas en español.
//
// Este archivo dejó de usarse el 2026-04-24. La app ya no llama a Edamam:
// ahora consume el endpoint /recipes/suggestions del backend del equipo
// (ver RecipeRepository y ApiService). El motivo del cambio está explicado
// con detalle en el header de RecipeRepository.kt.
//
// Conservo el archivo entero comentado por si en el futuro hay que revisar
// la firma original de las llamadas o restaurar el cliente Edamam como
// fallback. Si llegamos a borrarlo del todo, hay que confirmar primero
// que ningún caller sigue refiriéndose a EdamamService, EdamamResponse,
// EdamamHit, EdamamRecipe ni EdamamIngredient.

/*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface EdamamService {
    @GET("api/recipes/v2")
    suspend fun searchRecipes(
        @Query("type") type: String = "public",
        @Query("q") query: String,
        @Query("app_id") appId: String,
        @Query("app_key") appKey: String,
        @Query("lang") lang: String = "es",
        @Header("Edamam-Account-User") userId: String? = null,
        @Query("field") fields: List<String>? = null
    ): Response<EdamamResponse>
}

data class EdamamResponse(
    val hits: List<EdamamHit>?
)

data class EdamamHit(
    val recipe: EdamamRecipe?
)

data class EdamamRecipe(
    val label: String?,
    val uri: String?,
    val image: String?,
    val url: String?,
    val ingredientLines: List<String>?,
    val ingredients: List<EdamamIngredient>?,
    val cuisineType: List<String>?,
    val mealType: List<String>?,
    val dishType: List<String>?
)

data class EdamamIngredient(
    val text: String?,
    val quantity: Double?,
    val measure: String?,
    val food: String?,
    val weight: Double?,
    val foodCategory: String?,
    val image: String?
)
*/
