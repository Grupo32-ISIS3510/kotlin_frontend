package com.app.secondserving.data.network

import com.app.secondserving.data.local.FoodItemEntity
import com.app.secondserving.data.Result
import java.io.IOException

class InventoryServiceAdapter {

    // Cambiamos a un getter para que solo pida la instancia cuando REALMENTE la necesite
    // y no cuando se crea el objeto. Esto evita el crash de lateinit.
    private val apiService: ApiService
        get() = RetrofitClient.authInstance

    suspend fun getInventory(): Result<List<FoodItemEntity>> {
        return try {
            val response = apiService.getInventory()
            if (response.isSuccessful && response.body() != null) {
                val items = response.body()!!.items.map { it.toEntity() }
                Result.Success(items)
            } else {
                Result.Error(IOException("Error fetching: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(IOException("Error fetching", e))
        }
    }

    suspend fun createInventoryItem(request: InventoryItemRequest): Result<FoodItemEntity> {
        return try {
            val response = apiService.createInventoryItem(request)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.toEntity())
            } else {
                Result.Error(IOException("Error creating: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(IOException("Error creating", e))
        }
    }

    suspend fun deleteInventoryItem(itemId: String): Result<Unit> {
        // TODO: Implementar endpoint de eliminación en backend
        return Result.Error(IOException("Eliminación aún no implementada en el servidor"))
    }

    suspend fun updateInventoryItem(item: FoodItemEntity): Result<FoodItemEntity> {
        // TODO: Implementar endpoint de actualización en backend
        return Result.Error(IOException("Actualización aún no implementada en el servidor"))
    }
}

private fun InventoryItem.toEntity(): FoodItemEntity {
    return FoodItemEntity(
        id = this.id,
        name = this.name,
        category = this.category,
        quantity = this.quantity,
        purchaseDate = "", 
        expiryDate = this.expiry_date,
        originalShelfLifeDays = null,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}
