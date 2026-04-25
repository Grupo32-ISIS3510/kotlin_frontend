package com.app.secondserving.data.network

/**
 * Respuesta de GET /analytics/segment.
 *
 * El backend computa el segmento del usuario leyendo:
 *   - recipe_interactions (action="cooked") en los últimos 30 días
 *   - analytics_events (event_name IN notification_received | notification_opened)
 *
 * Fórmula:
 *   proactive si open_rate >= 0.6 AND recipes_cooked >= 3
 *   passive   si open_rate <  0.2 AND recipes_cooked == 0
 *   neutral   en otro caso
 */
data class UserSegmentResponse(
    val segment: String, // "proactive" | "neutral" | "passive"
    val recipes_cooked_last_30_days: Int,
    val open_rate: Double
)
