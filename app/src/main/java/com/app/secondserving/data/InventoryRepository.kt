package com.app.secondserving.data

import com.app.secondserving.data.local.AppDatabase
import com.app.secondserving.data.local.FoodItemDao
import com.app.secondserving.data.local.FoodItemEntity
import com.app.secondserving.data.network.InventoryItemRequest
import com.app.secondserving.data.network.InventoryServiceAdapter
import com.app.secondserving.data.network.SavingsAnalyticsResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Inventory Repository - Single Source of Truth.
 * Patrón Repository:
 * - Única fuente de verdad para datos de inventario
 * - Decide si leer de red (backend) o caché local (Room)
 * - Los ViewModel solo dependen de este repositorio
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

    /**
     * Obtiene todos los items del inventario.
     * Estrategia: Primero intenta obtener del backend, luego guarda en caché local.
     * Si falla la red, devuelve datos locales.
     */
    suspend fun getInventory(forceRefresh: Boolean = false): Result<List<FoodItemEntity>> {
        return try {
            val localItems = dao.getAllItems().first()
            val latestUpdate = dao.getLatestUpdateTimestamp() ?: 0L
            val cacheIsFresh = localItems.isNotEmpty() &&
                (System.currentTimeMillis() - latestUpdate) < INVENTORY_CACHE_TTL_MS

            // Si el caché local sigue fresco y no se pidió refresh forzado, evitamos llamada de red.
            if (!forceRefresh && cacheIsFresh) {
                return Result.Success(localItems)
            }

            val remoteResult = serviceAdapter.getInventory()
            when (remoteResult) {
                is Result.Success -> {
                    // Reemplazamos caché local para mantener consistencia con backend.
                    dao.deleteAllItems()
                    dao.insertAllItems(remoteResult.data)
                    Result.Success(remoteResult.data)
                }
                is Result.Error -> {
                    if (localItems.isNotEmpty()) Result.Success(localItems) else remoteResult
                }
            }
        } catch (e: Exception) {
            // Fallback a datos locales en caso de excepción
            val localItems = dao.getAllItems().first()
            if (localItems.isNotEmpty()) {
                Result.Success(localItems)
            } else {
                Result.Error(e)
            }
        }
    }

    /**
     * Obtiene items como Flow reactivo (observa cambios en la DB local).
     */
    fun getInventoryFlow(): Flow<List<FoodItemEntity>> {
        return dao.getAllItems()
    }

    /**
     * Obtiene items que expiran pronto (próximos 3 días).
     */
    fun getExpiringSoonItems(): Flow<List<FoodItemEntity>> {
        return dao.getExpiringSoonItems()
    }

    /**
     * Snapshot one-shot de items que expiran pronto (sin observar la DB).
     * Usado por el worker diario para no recibir re-emisiones.
     */
    suspend fun getExpiringSoonItemsOnce(): List<FoodItemEntity> {
        return dao.getExpiringSoonItems().first()
    }

    /**
     * Obtiene items ya expirados.
     */
    fun getExpiredItems(): Flow<List<FoodItemEntity>> {
        return dao.getExpiredItems()
    }

    /**
     * Crea un nuevo item en el inventario.
     * Estrategia: Guarda en backend primero, luego en caché local.
     */
    suspend fun createInventoryItem(request: InventoryItemRequest): Result<FoodItemEntity> {
        return try {
            val remoteResult = serviceAdapter.createInventoryItem(request)
            when (remoteResult) {
                is Result.Success -> {
                    dao.insertItem(remoteResult.data)
                    remoteResult
                }
                is Result.Error -> remoteResult
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Actualiza un item existente.
     * Invalida la caché de ahorro porque un cambio de estado (ej. consumido)
     * modifica el cálculo del período.
     */
    suspend fun updateInventoryItem(item: FoodItemEntity): Result<FoodItemEntity> {
        return try {
            val updatedItem = item.copy(updatedAt = System.currentTimeMillis())
            val remoteResult = serviceAdapter.updateInventoryItem(updatedItem)
            when (remoteResult) {
                is Result.Success -> {
                    dao.updateItem(updatedItem)
                    savingsCache?.invalidate()
                    remoteResult
                }
                is Result.Error -> remoteResult
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Elimina un item del inventario.
     * Invalida la caché de ahorro porque el alimento puede haberse consumido
     * o descartado, lo que cambia saved_cop / wasted_cop del período.
     */
    suspend fun deleteInventoryItem(itemId: String): Result<Unit> {
        return try {
            val remoteResult = serviceAdapter.deleteInventoryItem(itemId)
            when (remoteResult) {
                is Result.Success -> {
                    dao.deleteItemById(itemId)
                    savingsCache?.invalidate()
                    remoteResult
                }
                is Result.Error -> remoteResult
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Busca items por nombre o código de barras.
     */
    fun searchItems(query: String): Flow<List<FoodItemEntity>> {
        return dao.searchItems("%$query%")
    }

    /**
     * Obtiene items por categoría.
     */
    fun getItemsByCategory(category: String): Flow<List<FoodItemEntity>> {
        return dao.getItemsByCategory(category)
    }

    /**
     * Sincroniza el inventario local con el backend.
     * Útil para refresh manual o al iniciar la app.
     */
    suspend fun syncInventory(): Result<List<FoodItemEntity>> {
        return getInventory(forceRefresh = true)
    }

    // ── Gestión de sesión ────────────────────────────────────────────────────

    /**
     * Elimina todos los datos locales del usuario actual.
     * Debe llamarse justo antes de navegar al login para que el siguiente
     * usuario no vea inventario ni analíticas del anterior.
     */
    suspend fun clearUserData() {
        dao.deleteAllItems()
        savingsCache?.clear()
    }

    // ── Analytics de ahorro ──────────────────────────────────────────────────

    /**
     * Devuelve los datos de ahorro almacenados en caché si todavía son válidos
     * (TTL de 24 h y mismo período mes/año).
     *
     * Es una función síncrona: SharedPreferences ya está en memoria y es seguro
     * llamarla desde el hilo principal sin coroutine.
     */
    fun getCachedSavingsAnalytics(month: Int, year: Int): SavingsAnalyticsResponse? =
        savingsCache?.get(month, year)

    /**
     * Obtiene analíticas de ahorro desde la red y las almacena en caché.
     * Solo llamar cuando la caché no tiene datos válidos.
     */
    suspend fun getSavingsAnalytics(month: Int, year: Int): Result<SavingsAnalyticsResponse> {
        val result = serviceAdapter.getSavingsAnalytics(month = month, year = year)
        if (result is Result.Success) {
            savingsCache?.put(month, year, result.data)
        }
        return result
    }
}
