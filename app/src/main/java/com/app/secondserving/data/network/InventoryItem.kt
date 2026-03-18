package com.app.secondserving.data.network

data class InventoryItem(
    val id: String,
    val name: String,
    val category: String,
    val quantity: Int,
    val expiry_date: String
)
