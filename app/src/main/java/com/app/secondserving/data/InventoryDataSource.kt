package com.app.secondserving.data

import com.app.secondserving.data.network.InventoryItem
import com.app.secondserving.data.network.RetrofitClient
import java.io.IOException

class InventoryDataSource {

    suspend fun getInventory(): Result<List<InventoryItem>> {
        return try {
            val response = RetrofitClient.authInstance.getInventory()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.items)
            } else {
                Result.Error(IOException("Error fetching inventory: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(IOException("Error fetching inventory", e))
        }
    }
}
