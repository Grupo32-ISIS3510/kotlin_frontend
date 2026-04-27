package com.app.secondserving.data.network

data class InventoryItem(
    val id: String,
    val name: String,
    val category: String,
    val quantity: Double,
    val expiry_date: String,
    val unit: String? = null,
    val unit_price: String? = null
)
