package com.app.secondserving.data.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface MealDbService {
    @GET("filter.php")
    suspend fun filterByIngredient(@Query("i") ingredient: String): Response<MealListResponse>

    @GET("lookup.php")
    suspend fun getMealDetails(@Query("i") id: String): Response<MealDetailResponse>
}

data class MealListResponse(
    val meals: List<MealSummary>?
)

data class MealSummary(
    val strMeal: String,
    val strMealThumb: String,
    val idMeal: String
)

data class MealDetailResponse(
    val meals: List<Map<String, String?>>?
)
