package com.app.secondserving.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<TokenResponse>

    @GET("inventory")
    suspend fun getInventory(): Response<InventoryResponse>

    @POST("inventory")
    suspend fun createInventoryItem(@Body request: InventoryItemRequest): Response<InventoryItem>

    @POST("notifications/token")
    suspend fun registerFcmToken(@Body body: Map<String, String>): Response<Unit>
}