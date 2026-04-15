package com.app.secondserving.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para almacenamiento local de alimentos.
 * Patrón DAO: Esta entidad es la fuente de verdad local.
 */
@Entity(tableName = "food_items")
data class FoodItemEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: String,
    val quantity: Int,
    val purchaseDate: String,
    val expiryDate: String,
    val barcode: String? = null,
    val storageRecommendation: String? = null,
    val originalShelfLifeDays: Int? = null, // Vida útil original en días
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
