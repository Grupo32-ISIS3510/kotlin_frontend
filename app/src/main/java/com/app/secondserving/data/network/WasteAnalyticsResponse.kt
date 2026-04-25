package com.app.secondserving.data.network

/**
 * Cada elemento que devuelve GET /analytics/waste?months=N representa el
 * waste agregado de UNA categoría en UN mes.
 *
 * Para la BQ T3.2 (impacto de recetas en reducción de waste por categoría)
 * agrupamos por `category` y usamos `value_lost_cop` de cada mes como las
 * observaciones que alimentan el box plot.
 *
 * Nota: el backend hoy agrega por categoría de **alimento**. La BQ original
 * pide por categoría de **receta**. La diferencia se cierra mapeando
 * receta → categoría dominante de ingredientes en una iteración futura
 * (CQRS client-side leyendo recipe_interactions × inventory_events). Para
 * esta entrega usamos los datos disponibles porque cumplen el patrón
 * visual y la lectura defensiva: la categoría de alimento es proxy directo
 * de qué tipo de receta lo aprovecharía.
 */
data class WasteAnalyticsItem(
    val month: String,
    val category: String,
    val items_discarded: Int,
    val value_lost_cop: Double
)
