package com.app.secondserving.data

import com.app.secondserving.data.network.InventoryItem
import com.app.secondserving.data.network.InventoryItemRequest
import com.app.secondserving.data.network.RetrofitClient
import java.io.IOException

class InventoryDataSource {

    suspend fun getInventory(): Result<List<InventoryItem>> {
        return try {
            val response = RetrofitClient.authInstance.getInventory()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(IOException("Error fetching inventory: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(IOException("Error fetching inventory", e))
        }
    }
    suspend fun createInventoryItem(request: InventoryItemRequest): Result<InventoryItem> {
        return try {
            val response = RetrofitClient.authInstance.createInventoryItem(request)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(IOException("Error creating item: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(IOException("Error creating item", e))
        }
    }
}
