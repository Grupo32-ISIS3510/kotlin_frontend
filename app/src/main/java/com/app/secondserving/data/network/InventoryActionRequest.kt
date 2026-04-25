package com.app.secondserving.data.network

/**
 * Body de PATCH /inventory/{id}/discard.
 *
 * El backend acepta:
 *   reason: "expired" | "over_purchase" | "bad_storage" | "other"
 *   quantity: opcional. Si va null, descarta toda la cantidad.
 *
 * Cuando el usuario marca un alimento como descartado, el backend crea
 * automáticamente una entrada en inventory_events con type="discarded",
 * que luego alimenta las analíticas de waste y la BQ T3.2.
 *
 * El endpoint /consume no necesita body: la app solo notifica que el
 * alimento se aprovechó.
 */
data class DiscardRequest(
    val reason: String,
    val quantity: Int? = null
)

/** Razones permitidas por el backend (se exponen como constantes para evitar typos). */
object DiscardReasons {
    const val EXPIRED = "expired"
    const val OVER_PURCHASE = "over_purchase"
    const val BAD_STORAGE = "bad_storage"
    const val OTHER = "other"
}
