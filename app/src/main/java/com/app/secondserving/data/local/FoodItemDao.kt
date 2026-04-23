package com.app.secondserving.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO para acceso a la tabla de alimentos.
 * Patrón DAO: Abstracta el acceso a datos local (Room).
 */
@Dao
interface FoodItemDao {

    @Query("SELECT * FROM food_items ORDER BY expiryDate ASC")
    fun getAllItems(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getItemById(id: String): FoodItemEntity?

    @Query("SELECT * FROM food_items WHERE expiryDate >= date('now') ORDER BY expiryDate ASC")
    fun getNonExpiredItems(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE expiryDate <= date('now', '+3 days') AND expiryDate >= date('now') ORDER BY expiryDate ASC")
    fun getExpiringSoonItems(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE expiryDate < date('now') ORDER BY expiryDate ASC")
    fun getExpiredItems(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE category = :category ORDER BY expiryDate ASC")
    fun getItemsByCategory(category: String): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE name LIKE :query OR barcode LIKE :query ORDER BY expiryDate ASC")
    fun searchItems(query: String): Flow<List<FoodItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: FoodItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllItems(items: List<FoodItemEntity>)

    @Update
    suspend fun updateItem(item: FoodItemEntity)

    @Delete
    suspend fun deleteItem(item: FoodItemEntity)

    @Query("DELETE FROM food_items WHERE id = :id")
    suspend fun deleteItemById(id: String)

    @Query("DELETE FROM food_items")
    suspend fun deleteAllItems()

    @Query("SELECT COUNT(*) FROM food_items")
    fun getItemCount(): Flow<Int>

    @Query("SELECT MAX(updatedAt) FROM food_items")
    suspend fun getLatestUpdateTimestamp(): Long?
}
