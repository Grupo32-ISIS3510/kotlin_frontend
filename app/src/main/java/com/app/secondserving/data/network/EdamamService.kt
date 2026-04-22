package com.app.secondserving.data.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface EdamamService {
    @GET("api/recipes/v2")
    suspend fun searchRecipes(
        @Query("type") type: String = "public",
        @Query("q") query: String,
        @Query("app_id") appId: String,
        @Query("app_key") appKey: String,
        @Query("lang") lang: String = "es",
        @Query("field") fields: List<String> = listOf("label", "uri", "image", "source", "url", "yield", "dietLabels", "healthLabels", "ingredientLines", "ingredients", "calories", "totalTime", "cuisineType", "mealType", "dishType")
    ): Response<EdamamResponse>
}

data class EdamamResponse(
    val hits: List<EdamamHit>
)

data class EdamamHit(
    val recipe: EdamamRecipe
)

data class EdamamRecipe(
    val label: String,
    val uri: String,
    val image: String?,
    val url: String?,
    val ingredientLines: List<String>,
    val ingredients: List<EdamamIngredient>,
    val cuisineType: List<String>?,
    val mealType: List<String>?,
    val dishType: List<String>?
)

data class EdamamIngredient(
    val text: String,
    val quantity: Double,
    val measure: String?,
    val food: String,
    val weight: Double,
    val foodCategory: String?,
    val image: String?
)
