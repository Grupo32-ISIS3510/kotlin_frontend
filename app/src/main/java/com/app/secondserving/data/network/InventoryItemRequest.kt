package com.app.secondserving.data.network
data class InventoryItemRequest(
    val name: String,
    val category: String,
    val quantity: Double,
    val purchase_date: String,
    val expiry_date: String,
    val price: Double? = null
)