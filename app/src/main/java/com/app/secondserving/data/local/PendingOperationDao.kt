package com.app.secondserving.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete

@Dao
interface PendingOperationDao {
    @Insert
    suspend fun insert(operation: PendingOperationEntity)

    @Query("SELECT * FROM pending_operations ORDER BY createdAt ASC")
    suspend fun getAllPendingOperations(): List<PendingOperationEntity>

    @Delete
    suspend fun delete(operation: PendingOperationEntity)

    @Query("DELETE FROM pending_operations WHERE itemId = :itemId AND type = :type")
    suspend fun deleteByItemIdAndType(itemId: String, type: String)
}
