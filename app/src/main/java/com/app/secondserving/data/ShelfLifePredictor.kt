package com.app.secondserving.data

import com.app.secondserving.data.local.FoodItemEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Algoritmo de predicción de vida útil de alimentos.
 * Feature (d): Smart feature — predicción de vida útil.
 * 
 * Basado en:
 * - Categoría del alimento
 * - Tipo de almacenamiento recomendado
 * - Datos históricos (si disponibles)
 */
object ShelfLifePredictor {

    /**
     * Vida útil estimada en días por categoría y tipo de almacenamiento.
     * Estos valores son aproximados y pueden variar según el producto específico.
     */
    private val shelfLifeDays = mapOf(
        // Lácteos
        "lacteos" to mapOf(
            "refrigerado" to 7,
            "congelado" to 90,
            "ambiente" to 1
        ),
        // Carnes
        "carnes" to mapOf(
            "refrigerado" to 3,
            "congelado" to 180,
            "ambiente" to 0
        ),
        // Pescados
        "pescados" to mapOf(
            "refrigerado" to 2,
            "congelado" to 90,
            "ambiente" to 0
        ),
        // Frutas y verduras
        "frutas_verduras" to mapOf(
            "refrigerado" to 14,
            "congelado" to 180,
            "ambiente" to 7
        ),
        // Granos y cereales
        "granos" to mapOf(
            "refrigerado" to 365,
            "congelado" to 730,
            "ambiente" to 180
        ),
        // Enlatados
        "enlatados" to mapOf(
            "refrigerado" to 365,
            "congelado" to 365,
            "ambiente" to 730
        ),
        // Panadería
        "panaderia" to mapOf(
            "refrigerado" to 7,
            "congelado" to 90,
            "ambiente" to 3
        ),
        // Bebidas
        "bebidas" to mapOf(
            "refrigerado" to 30,
            "congelado" to 90,
            "ambiente" to 180
        ),
        // Condimentos
        "condimentos" to mapOf(
            "refrigerado" to 180,
            "congelado" to 365,
            "ambiente" to 365
        ),
        // Otros
        "otros" to mapOf(
            "refrigerado" to 7,
            "congelado" to 90,
            "ambiente" to 30
        )
    )

    /**
     * Recomendaciones de almacenamiento por categoría.
     */
    val storageRecommendations = mapOf(
        "lacteos" to "refrigerado",
        "carnes" to "refrigerado",
        "pescados" to "refrigerado",
        "frutas_verduras" to "refrigerado",
        "granos" to "ambiente",
        "enlatados" to "ambiente",
        "panaderia" to "ambiente",
        "bebidas" to "ambiente",
        "condimentos" to "ambiente",
        "otros" to "ambiente"
    )

    /**
     * Predice la vida útil estimada en días para un alimento.
     * @param category Categoría del alimento
     * @param storageType Tipo de almacenamiento (refrigerado, congelado, ambiente)
     * @return Días estimados de vida útil
     */
    fun predictShelfLifeDays(category: String, storageType: String = "ambiente"): Int {
        val categoryKey = normalizeCategory(category)
        val storageKey = storageType.lowercase()
        
        return shelfLifeDays[categoryKey]?.get(storageKey) 
            ?: shelfLifeDays["otros"]?.get(storageKey) 
            ?: 7 // Default: 7 días
    }

    /**
     * Predice la fecha de expiración basada en la fecha de compra.
     * @param purchaseDate Fecha de compra
     * @param category Categoría del alimento
     * @param storageType Tipo de almacenamiento
     * @return Fecha de expiración estimada
     */
    fun predictExpiryDate(
        purchaseDate: LocalDate,
        category: String,
        storageType: String = "ambiente"
    ): LocalDate {
        val shelfLifeDaysValue = predictShelfLifeDays(category, storageType)
        return purchaseDate.plusDays(shelfLifeDaysValue.toLong())
    }

    /**
     * Predice la fecha de expiración desde una fecha en string.
     */
    fun predictExpiryDate(
        purchaseDateStr: String,
        category: String,
        storageType: String = "ambiente"
    ): String {
        return try {
            val purchaseDate = LocalDate.parse(purchaseDateStr)
            val expiryDate = predictExpiryDate(purchaseDate, category, storageType)
            expiryDate.toString()
        } catch (e: Exception) {
            // Si no se puede parsear, usar fecha actual
            predictExpiryDate(LocalDate.now(), category, storageType).toString()
        }
    }

    /**
     * Obtiene la recomendación de almacenamiento para una categoría.
     */
    fun getStorageRecommendation(category: String): String {
        val categoryKey = normalizeCategory(category)
        return storageRecommendations[categoryKey] ?: "ambiente"
    }

    /**
     * Calcula el porcentaje de vida útil restante.
     * @param purchaseDate Fecha de compra
     * @param expiryDate Fecha de expiración
     * @return Porcentaje de vida útil restante (0-100)
     */
    fun calculateRemainingShelfLifePercentage(
        purchaseDate: LocalDate,
        expiryDate: LocalDate
    ): Int {
        val today = LocalDate.now()
        
        if (today.isAfter(expiryDate) || today.isEqual(expiryDate)) {
            return 0 // Expirado
        }
        
        if (today.isBefore(purchaseDate)) {
            return 100 // Aún no comienza la cuenta
        }
        
        val totalDays = ChronoUnit.DAYS.between(purchaseDate, expiryDate).toInt()
        val elapsedDays = ChronoUnit.DAYS.between(purchaseDate, today).toInt()
        
        if (totalDays <= 0) return 0
        
        return ((totalDays - elapsedDays).toDouble() / totalDays * 100.0).coerceIn(0.0, 100.0).toInt()
    }

    /**
     * Evalúa si un alimento aún es seguro para consumir.
     * Considera un margen de seguridad de 1 día después de la expiración
     * para ciertos tipos de alimentos.
     */
    fun isSafeToConsume(
        expiryDate: LocalDate,
        category: String,
        daysAfterExpiry: Int = 0
    ): Boolean {
        val today = LocalDate.now()
        val categoryKey = normalizeCategory(category)
        
        // Margen de seguridad por categoría (días después de expiración)
        val safetyMargin = when (categoryKey) {
            "carnes", "pescados", "lacteos" -> 0 // Sin margen
            "frutas_verduras" -> 1
            "enlatados", "granos" -> 30
            else -> 1
        }
        
        return today.isBefore(expiryDate.plusDays(safetyMargin.toLong()))
    }

    /**
     * Actualiza un item con la predicción de vida útil si no tiene fecha de expiración.
     */
    fun enrichWithShelfLifePrediction(item: FoodItemEntity): FoodItemEntity {
        // Si ya tiene fecha de expiración, no hacer nada
        if (item.expiryDate.isNotBlank()) {
            return item
        }
        
        // Predecir fecha de expiración
        val storageType = getStorageRecommendation(item.category)
        val predictedExpiry = predictExpiryDate(
            purchaseDateStr = if (item.purchaseDate.isBlank()) LocalDate.now().toString() else item.purchaseDate,
            category = item.category,
            storageType = storageType
        )
        
        return item.copy(
            expiryDate = predictedExpiry,
            storageRecommendation = storageType,
            originalShelfLifeDays = predictShelfLifeDays(item.category, storageType)
        )
    }

    /**
     * Normaliza el nombre de la categoría para matching.
     */
    private fun normalizeCategory(category: String): String {
        val normalized = category.lowercase()
            .replace(" ", "_")
            .replace("-", "_")
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
        
        // Mapeo de variantes comunes
        return when {
            normalized.contains("lact") || normalized.contains("leche") -> "lacteos"
            normalized.contains("carne") || normalized.contains("pollo") || normalized.contains("res") -> "carnes"
            normalized.contains("pesc") || normalized.contains("marisc") -> "pescados"
            normalized.contains("fruta") || normalized.contains("verdura") || normalized.contains("vegetal") -> "frutas_verduras"
            normalized.contains("grano") || normalized.contains("cere") || normalized.contains("arroz") || normalized.contains("pasta") -> "granos"
            normalized.contains("enlat") || normalized.contains("conserva") -> "enlatados"
            normalized.contains("pan") || normalized.contains("gallet") || normalized.contains("pastel") -> "panaderia"
            normalized.contains("beb") || normalized.contains("jugo") || normalized.contains("refresco") -> "bebidas"
            normalized.contains("condiment") || normalized.contains("espec") || normalized.contains("salsa") -> "condimentos"
            else -> "otros"
        }
    }
}
