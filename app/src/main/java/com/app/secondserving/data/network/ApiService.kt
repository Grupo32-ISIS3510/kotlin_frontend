package com.app.secondserving.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

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

    @GET("analytics/savings")
    suspend fun getSavingsAnalytics(
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null
    ): Response<SavingsAnalyticsResponse>

    // Recipe Endpoints
    // El backend computa los matches con el inventario en el servidor
    // y devuelve directamente la lista de recetas sugeridas.
    @GET("recipes/suggestions")
    suspend fun getRecipes(): Response<List<Recipe>>

    // Registra una interacción del usuario con una receta (action = "viewed" | "cooked").
    // Antes era POST /recipes/{id}/cook (sin body) — ese endpoint no existe en
    // el backend del equipo y por eso cookRecipe era un stub vacío. Ahora se usa
    // /interact con un body que diferencia ver vs cocinar; eso pobla la tabla
    // recipe_interactions que necesita la BQ T4.1 para distinguir Passive vs
    // Proactive users.
    @POST("recipes/{id}/interact")
    suspend fun interactWithRecipe(
        @Path("id") recipeId: String,
        @Body request: RecipeInteractionRequest
    ): Response<Unit>

    // Endpoint anterior: lo dejé comentado como referencia. No existía en el
    // backend, por eso cookRecipe en el repo era un stub que devolvía Success
    // sin hacer ninguna llamada de red.
    // @POST("recipes/{id}/cook")
    // suspend fun cookRecipe(@Path("id") recipeId: String): Response<Unit>
}