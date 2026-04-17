package com.app.secondserving.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<TokenResponse>

    @GET("inventory")
    suspend fun getInventory(): Response<InventoryResponse>

    @POST("inventory")
    suspend fun createInventoryItem(@Body request: InventoryItemRequest): Response<InventoryItem>

    @DELETE("inventory/{id}")
    suspend fun deleteInventoryItem(@Path("id") itemId: String): Response<Unit>

    @PUT("inventory/{id}")
    suspend fun updateInventoryItem(@Path("id") itemId: String, @Body request: InventoryItemRequest): Response<InventoryItem>

    @POST("notifications/token")
    suspend fun registerFcmToken(@Body body: Map<String, String>): Response<Unit>

    // Recipe Endpoints - Updated to match T2.3 Requirement
    @GET("recipes/suggestions")
    suspend fun getRecipes(): Response<RecipeResponse>

    @POST("recipes/{id}/cook")
    suspend fun cookRecipe(@Path("id") recipeId: Int): Response<Unit>
}
