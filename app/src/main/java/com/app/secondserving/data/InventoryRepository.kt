package com.app.secondserving.data

import com.app.secondserving.data.network.InventoryItem

class InventoryRepository(private val dataSource: InventoryDataSource = InventoryDataSource()) {

    suspend fun getInventory(): Result<List<InventoryItem>> {
        return dataSource.getInventory()
    }
}
