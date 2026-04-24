package com.app.secondserving.data.network

import com.app.secondserving.data.local.FoodItemEntity
import com.app.secondserving.data.Result
import java.io.IOException

/**
 * Service Adapter / Web Service Broker.
 * Patrón: Encapsula el acceso a servicios remotos (Retrofit).
 */
class InventoryServiceAdapter(
    private val apiService: ApiService = RetrofitClient.authInstance
) {

    /**
     * Obtiene inventario desde el backend.
     * @return Result con lista de FoodItemEntity o Error
     */
    suspend fun getInventory(): Result<List<FoodItemEntity>> {
        return try {
            val response = apiService.getInventory()
            if (response.isSuccessful) {
                // Ahora response.body() es directamente List<InventoryItem>
                val items = response.body()?.map { it.toEntity() }
                if (items != null) {
                    Result.Success(items)
                } else {
                    Result.Success(emptyList())
                }
            } else {
                Result.Error(
                    IOException(
                        "Error fetching inventory: ${response.code()} ${response.message()}"
                    )
                )
            }
        } catch (e: Exception) {
            Result.Error(IOException("Error fetching inventory", e))
        }
    }

    /**
     * Crea un nuevo item en el backend.
     */
    suspend fun createInventoryItem(request: InventoryItemRequest): Result<FoodItemEntity> {
        return try {
            val response = apiService.createInventoryItem(request)
            if (response.isSuccessful) {
                val item = response.body()?.toEntity()
                if (item != null) {
                    Result.Success(item)
                } else {
                    Result.Error(IOException("Empty response body from server"))
                }
            } else {
                Result.Error(
                    IOException("Error creating item: ${response.code()} ${response.message()}")
                )
            }
        } catch (e: Exception) {
            Result.Error(IOException("Error creating item", e))
        }
    }

    /**
     * Elimina un item del backend.
     */
    suspend fun deleteInventoryItem(itemId: String): Result<Unit> {
        return try {
            val response = apiService.deleteInventoryItem(itemId)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error(
                    IOException("Error deleting item: ${response.code()} ${response.message()}")
                )
            }
        } catch (e: Exception) {
            Result.Error(IOException("Error deleting item", e))
        }
    }

    /**
     * Actualiza un item en el backend.
     */
    suspend fun updateInventoryItem(item: FoodItemEntity): Result<FoodItemEntity> {
        return try {
            val request = InventoryItemRequest(
                name = item.name,
                category = item.category,
                quantity = item.quantity.toDouble(),
                purchase_date = item.purchaseDate,
                expiry_date = item.expiryDate
            )
            val response = apiService.updateInventoryItem(item.id, request)
            if (response.isSuccessful) {
                val updated = response.body()?.toEntity()
                if (updated != null) {
                    Result.Success(updated)
                } else {
                    Result.Error(IOException("Empty response body from server"))
                }
            } else {
                Result.Error(
                    IOException("Error updating item: ${response.code()} ${response.message()}")
                )
            }
        } catch (e: Exception) {
            Result.Error(IOException("Error updating item", e))
        }
    }
}

/**
 * Extensión para mapear DTO del backend a entidad Room.
 */
private fun InventoryItem.toEntity(): FoodItemEntity {
    return FoodItemEntity(
        id = this.id,
        name = this.name,
        category = this.category,
        quantity = this.quantity.toInt(),
        purchaseDate = "", 
        expiryDate = this.expiry_date,
        originalShelfLifeDays = null,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}
