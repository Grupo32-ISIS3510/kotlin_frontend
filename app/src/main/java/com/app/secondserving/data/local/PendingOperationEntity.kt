package com.app.secondserving.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad para almacenar operaciones pendientes de sincronizar con el servidor.
 * Permite soporte offline para creación, consumo y descarte de alimentos.
 */
@Entity(tableName = "pending_operations")
data class PendingOperationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "CREATE", "CONSUME", "DISCARD"
    val itemId: String, // ID temporal o real del item
    val data: String, // JSON con los detalles de la operación (InventoryItemRequest o DiscardRequest)
    val createdAt: Long = System.currentTimeMillis()
)
