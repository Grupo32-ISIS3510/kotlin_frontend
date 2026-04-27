package com.app.secondserving.data.network

import com.app.secondserving.data.local.FoodItemEntity
import com.app.secondserving.data.Result
import java.io.IOException

/**
 * Service Adapter / Web Service Broker.
 * Patrón: Encapsula el acceso a servicios remotos (Retrofit).
 *
 * Diferenciación de errores:
 *  - Si el backend respondió con status != 2xx → Result.Error(ApiException(code, userMessage)).
 *    Estos NO se tratan como offline; la UI muestra el mensaje del servidor.
 *  - Si hubo fallo real de red (sin respuesta) → Result.Error(IOException(...)).
 *    Estos sí los puede tomar el repo para encolar pending operations.
 */
class InventoryServiceAdapter(
    private val apiService: ApiService = RetrofitClient.authInstance
) {

    /**
     * Obtiene inventario desde el backend.
     */
    suspend fun getInventory(): Result<List<FoodItemEntity>> {
        return try {
            val response = apiService.getInventory()
            when {
                response.isSuccessful -> {
                    val items = response.body()?.items?.map { it.toEntity() } ?: emptyList()
                    Result.Success(items)
                }
                response.code() == 404 -> {
                    // 404 = usuario sin inventario aún (cuenta nueva)
                    Result.Success(emptyList())
                }
                else -> Result.Error(response.toApiException("Error al obtener inventario"))
            }
        } catch (e: IOException) {
            Result.Error(e)
        } catch (e: Exception) {
            Result.Error(e)
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
                Result.Error(response.toApiException("Error al crear el alimento"))
            }
        } catch (e: IOException) {
            Result.Error(e)
        } catch (e: Exception) {
            Result.Error(e)
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
                Result.Error(response.toApiException("Error al eliminar el alimento"))
            }
        } catch (e: IOException) {
            Result.Error(e)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /** Marca el item como consumido. El backend crea inventory_events type="consumed". */
    suspend fun consumeInventoryItem(itemId: String): Result<Unit> {
        return try {
            val response = apiService.consumeInventoryItem(itemId)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error(response.toApiException("Error al marcar como consumido"))
            }
        } catch (e: IOException) {
            Result.Error(e)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Marca el item como descartado con una razón. Si quantity es null, el
     * backend descarta toda la cantidad. Crea inventory_events type="discarded".
     */
    suspend fun discardInventoryItem(itemId: String, reason: String, quantity: Int? = null): Result<Unit> {
        return try {
            val response = apiService.discardInventoryItem(
                itemId,
                DiscardRequest(reason = reason, quantity = quantity)
            )
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error(response.toApiException("Error al descartar el alimento"))
            }
        } catch (e: IOException) {
            Result.Error(e)
        } catch (e: Exception) {
            Result.Error(e)
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
                expiry_date = item.expiryDate,
                unit_price = item.unitPrice
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
                Result.Error(response.toApiException("Error al actualizar el alimento"))
            }
        } catch (e: IOException) {
            Result.Error(e)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Obtiene métricas de ahorro para el dashboard de inicio.
     */
    suspend fun getSavingsAnalytics(month: Int? = null, year: Int? = null): Result<SavingsAnalyticsResponse> {
        return try {
            val response = apiService.getSavingsAnalytics(month = month, year = year)
            if (response.isSuccessful) {
                val analytics = response.body()
                if (analytics != null) {
                    Result.Success(analytics)
                } else {
                    Result.Error(IOException("Empty response body from server"))
                }
            } else {
                Result.Error(response.toApiException("Error al obtener métricas de ahorro"))
            }
        } catch (e: IOException) {
            Result.Error(e)
        } catch (e: Exception) {
            Result.Error(e)
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
        unitPrice = this.unit_price?.toDoubleOrNull(),
        originalShelfLifeDays = null,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}
