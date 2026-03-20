package com.app.secondserving.data

import com.app.secondserving.data.network.InventoryItem
import com.app.secondserving.data.network.InventoryItemRequest

class InventoryRepository(private val dataSource: InventoryDataSource = InventoryDataSource()) {

    suspend fun getInventory(): Result<List<InventoryItem>> {
        return dataSource.getInventory()
    }
    suspend fun createInventoryItem(request: InventoryItemRequest): Result<InventoryItem> {
        return dataSource.createInventoryItem(request)
    }
}
