package com.app.secondserving.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
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

    // PATCH /inventory/{id}/consume — el backend marca el item como consumido
    // y crea una entrada en inventory_events con type="consumed". Esto pobla
    // las analíticas de saved_cop (lo que el usuario aprovechó antes de vencer).
    @PATCH("inventory/{id}/consume")
    suspend fun consumeInventoryItem(@Path("id") itemId: String): Response<Unit>

    // PATCH /inventory/{id}/discard — marca el item como descartado y crea
    // inventory_events con type="discarded". Pobla wasted_cop y es la entrada
    // de la BQ T3.2 (impacto de recetas en reducción de waste).
    @PATCH("inventory/{id}/discard")
    suspend fun discardInventoryItem(
        @Path("id") itemId: String,
        @Body request: DiscardRequest
    ): Response<Unit>

    @POST("notifications/token")
    suspend fun registerFcmToken(@Body body: Map<String, String>): Response<Unit>

    @GET("analytics/savings")
    suspend fun getSavingsAnalytics(
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null
    ): Response<SavingsAnalyticsResponse>

    // BQ T4.1 — el backend ya computa el segmento del usuario (Proactive /
    // Neutral / Passive) leyendo recipe_interactions y analytics_events de
    // los últimos 30 días. Solo lo consumimos.
    @GET("analytics/segment")
    suspend fun getUserSegment(): Response<UserSegmentResponse>

    // BQ T3.2 — waste agregado por mes y categoría. La pantalla
    // RecipeImpactScreen agrupa los items por categoría y usa las
    // observaciones mensuales para construir un box plot.
    @GET("analytics/waste")
    suspend fun getWasteAnalytics(
        @Query("months") months: Int = 3
    ): Response<List<WasteAnalyticsItem>>

    // Ingesta de eventos del cliente. Lo usamos para registrar
    // notification_received y notification_opened (entrada que necesita la
    // BQ T4.1 para calcular el open_rate).
    @POST("analytics/events")
    suspend fun postAnalyticsEvents(@Body batch: AnalyticsEventBatch): Response<Unit>

    // Recipe Endpoints
    // El backend computa los matches con el inventario en el servidor
    // y devuelve directamente la lista de recetas sugeridas. Esta lista
    // viene "liviana" (sin ingredientes ni instrucciones).
    @GET("recipes/suggestions")
    suspend fun getRecipes(): Response<List<Recipe>>

    // Detalle completo de una receta. /suggestions no incluye ingredients
    // ni instructions, así que este endpoint los trae cuando el usuario
    // abre la pantalla de detalle.
    @GET("recipes/{id}")
    suspend fun getRecipeDetail(@Path("id") recipeId: String): Response<Recipe>

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