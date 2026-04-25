package com.app.secondserving.data.network

/**
 * Body de POST /analytics/events.
 *
 * El backend acepta hasta 100 eventos por request. En esta entrega los
 * mandamos uno por uno (fire-and-forget) — la versión con cola offline,
 * batches y reintentos vía WorkManager queda planificada para la siguiente
 * iteración (curso 15.EventualConnectivity / 18.Multi_threading).
 */
data class AnalyticsEventCreate(
    val event_name: String,
    val properties: Map<String, Any>? = null,
    val session_id: String? = null,
    val platform: String = "android",
    val app_version: String = "1.0",
    val occurred_at: String // ISO-8601 UTC, ej: 2026-04-25T10:30:00Z
)

data class AnalyticsEventBatch(
    val events: List<AnalyticsEventCreate>
)
