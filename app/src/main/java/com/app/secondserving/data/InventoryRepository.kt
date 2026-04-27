package com.app.secondserving.data

import com.app.secondserving.data.local.AppDatabase
import com.app.secondserving.data.local.FoodItemDao
import com.app.secondserving.data.local.FoodItemEntity
import com.app.secondserving.data.local.PendingOperationDao
import com.app.secondserving.data.local.PendingOperationEntity
import com.app.secondserving.data.network.ApiException
import com.app.secondserving.data.network.DiscardRequest
import com.app.secondserving.data.network.InventoryItemRequest
import com.app.secondserving.data.network.InventoryServiceAdapter
import com.app.secondserving.data.network.SavingsAnalyticsResponse
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.util.UUID

/**
 * Inventory Repository - Single Source of Truth with Offline Support.
 * Soporta persistencia local inmediata (Optimistic UI) y cola de sincronización.
 */
class InventoryRepository(
    private val database: AppDatabase,
    private val serviceAdapter: InventoryServiceAdapter = InventoryServiceAdapter(),
    private val savingsCache: SavingsCache? = null
) {
    companion object {
        private const val INVENTORY_CACHE_TTL_MS = 24 * 60 * 60 * 1000L
    }

    private val dao: FoodItemDao = database.foodItemDao()
    private val pendingDao: PendingOperationDao = database.pendingOperationDao()
    private val gson = Gson()

    /**
     * Obtiene todos los items del inventario.
     */
    suspend fun getInventory(forceRefresh: Boolean = false): Result<List<FoodItemEntity>> {
        return try {
            val localItems = dao.getAllItems().first()
            val latestUpdate = dao.getLatestUpdateTimestamp() ?: 0L
            val cacheIsFresh = localItems.isNotEmpty() &&
                (System.currentTimeMillis() - latestUpdate) < INVENTORY_CACHE_TTL_MS

            if (!forceRefresh && cacheIsFresh) {
                return Result.Success(localItems)
            }

            val remoteResult = serviceAdapter.getInventory()
            when (remoteResult) {
                is Result.Success -> {
                    dao.deleteAllItems()
                    dao.insertAllItems(remoteResult.data)
                    Result.Success(remoteResult.data)
                }
                is Result.Error -> {
                    if (localItems.isNotEmpty()) Result.Success(localItems) else remoteResult
                }
            }
        } catch (e: Exception) {
            val localItems = dao.getAllItems().first()
            if (localItems.isNotEmpty()) {
                Result.Success(localItems)
            } else {
                Result.Error(e)
            }
        }
    }

    fun getInventoryFlow(): Flow<List<FoodItemEntity>> = dao.getAllItems()

    /**
     * Crea un nuevo item. Soporta OFFLINE.
     */
    suspend fun createInventoryItem(request: InventoryItemRequest): Result<FoodItemEntity> {
        return try {
            val remoteResult = serviceAdapter.createInventoryItem(request)
            when (remoteResult) {
                is Result.Success -> {
                    dao.insertItem(remoteResult.data)
                    remoteResult
                }
                is Result.Error -> {
                    if (remoteResult.exception is IOException) {
                        savePendingCreate(request)
                    } else remoteResult
                }
            }
        } catch (e: IOException) {
            savePendingCreate(request)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun savePendingCreate(request: InventoryItemRequest): Result<FoodItemEntity> {
        val tempId = "tmp_" + UUID.randomUUID().toString()
        val localEntity = FoodItemEntity(
            id = tempId,
            name = request.name,
            category = request.category,
            quantity = request.quantity.toInt(),
            purchaseDate = request.purchase_date,
            expiryDate = request.expiry_date
        )
        dao.insertItem(localEntity)
        pendingDao.insert(PendingOperationEntity(
            type = "CREATE",
            itemId = tempId,
            data = gson.toJson(request)
        ))
        return Result.Success(localEntity)
    }

    /** Marca el item como consumido. Soporta OFFLINE. */
    suspend fun consumeInventoryItem(itemId: String): Result<Unit> {
        return try {
            val remoteResult = serviceAdapter.consumeInventoryItem(itemId)
            when (remoteResult) {
                is Result.Success -> {
                    dao.deleteItemById(itemId)
                    savingsCache?.invalidate()
                    remoteResult
                }
                is Result.Error -> {
                    if (remoteResult.exception is IOException) {
                        savePendingConsume(itemId)
                    } else remoteResult
                }
            }
        } catch (e: IOException) {
            savePendingConsume(itemId)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun savePendingConsume(itemId: String): Result<Unit> {
        dao.deleteItemById(itemId)
        pendingDao.insert(PendingOperationEntity(
            type = "CONSUME",
            itemId = itemId,
            data = ""
        ))
        savingsCache?.invalidate()
        return Result.Success(Unit)
    }

    /** Marca el item como descartado. Soporta OFFLINE. */
    suspend fun discardInventoryItem(
        itemId: String,
        reason: String,
        quantity: Int? = null
    ): Result<Unit> {
        val request = DiscardRequest(reason, quantity)
        return try {
            val remoteResult = serviceAdapter.discardInventoryItem(itemId, reason, quantity)
            when (remoteResult) {
                is Result.Success -> {
                    dao.deleteItemById(itemId)
                    savingsCache?.invalidate()
                    remoteResult
                }
                is Result.Error -> {
                    if (remoteResult.exception is IOException) {
                        savePendingDiscard(itemId, request)
                    } else remoteResult
                }
            }
        } catch (e: IOException) {
            savePendingDiscard(itemId, request)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun savePendingDiscard(itemId: String, request: DiscardRequest): Result<Unit> {
        dao.deleteItemById(itemId)
        pendingDao.insert(PendingOperationEntity(
            type = "DISCARD",
            itemId = itemId,
            data = gson.toJson(request)
        ))
        savingsCache?.invalidate()
        return Result.Success(Unit)
    }

    /**
     * Sincroniza operaciones pendientes con el servidor.
     *
     * Política de reintentos:
     *  - Result.Success → operación completada, se elimina de la cola.
     *  - Result.Error con ApiException (4xx/5xx) → no es recuperable
     *    (validación rechazada, item ya inexistente, etc.). Se dropea de la
     *    cola junto con su entidad temporal local para no reintentar
     *    infinitamente algo que el backend siempre va a rechazar.
     *  - Result.Error con IOException u otra → seguimos offline o error
     *    transitorio; se deja en la cola para el siguiente intento.
     */
    suspend fun syncPendingOperations(): Int {
        val pending = pendingDao.getAllPendingOperations()
        var successCount = 0
        pending.forEach { op ->
            val result = when (op.type) {
                "CREATE" -> {
                    val request = gson.fromJson(op.data, InventoryItemRequest::class.java)
                    serviceAdapter.createInventoryItem(request)
                }
                "CONSUME" -> serviceAdapter.consumeInventoryItem(op.itemId)
                "DISCARD" -> {
                    val request = gson.fromJson(op.data, DiscardRequest::class.java)
                    serviceAdapter.discardInventoryItem(op.itemId, request.reason, request.quantity)
                }
                else -> Result.Error(Exception("Unknown type"))
            }

            when (result) {
                is Result.Success -> {
                    pendingDao.delete(op)
                    if (op.type == "CREATE" && result.data is FoodItemEntity) {
                        dao.deleteItemById(op.itemId)
                        dao.insertItem(result.data as FoodItemEntity)
                    }
                    successCount++
                }
                is Result.Error -> {
                    if (result.exception is ApiException) {
                        // Backend rechazó la op de forma definitiva — se descarta.
                        pendingDao.delete(op)
                        if (op.type == "CREATE") dao.deleteItemById(op.itemId)
                    }
                    // IOException u otros: dejar en cola para el próximo intento.
                }
            }
        }
        if (successCount > 0) syncInventory()
        return successCount
    }

    suspend fun clearUserData() {
        dao.deleteAllItems()
        pendingDao.deleteAll()
        savingsCache?.clear()
    }

    suspend fun syncInventory(): Result<List<FoodItemEntity>> = getInventory(forceRefresh = true)

    fun getCachedSavingsAnalytics(month: Int, year: Int): SavingsAnalyticsResponse? =
        savingsCache?.get(month, year)

    suspend fun getSavingsAnalytics(month: Int, year: Int): Result<SavingsAnalyticsResponse> {
        val result = serviceAdapter.getSavingsAnalytics(month = month, year = year)
        if (result is Result.Success) {
            savingsCache?.put(month, year, result.data)
        }
        return result
    }

    suspend fun getExpiringSoonItemsOnce(): List<FoodItemEntity> {
        return dao.getExpiringSoonItems().first()
    }
}
