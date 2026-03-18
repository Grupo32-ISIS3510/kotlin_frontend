package com.app.secondserving.data.network

data class InventoryResponse(
    val items: List<InventoryItem>,
    val total: Int
)
