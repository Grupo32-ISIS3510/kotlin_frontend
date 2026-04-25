package com.app.secondserving.data.network

// Todos los campos son nullables porque el backend a veces omite o
// envía null en `ingredients` y `matched_ingredients` para recetas con
// metadata incompleta. Gson no respeta los defaults de Kotlin cuando el
// JSON contiene null explícito, así que tipamos nullable y manejamos
// con `?: emptyList()` / `?: ""` en los call sites.
data class Recipe(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val instructions: String? = null,
    val ingredients: List<RecipeIngredient>? = null,
    val matched_ingredients: List<String>? = null,
    val soonest_expiry_days: Int? = null,
    val score: Double? = null
)

data class RecipeIngredient(
    val name: String? = null,
    val quantity: String? = null,
    val unit: String? = null
)

// Body que espera POST /recipes/{id}/interact en el backend.
// action solo puede ser "viewed" o "cooked"; el backend lo valida y rechaza
// cualquier otro valor con 422.
data class RecipeInteractionRequest(
    val action: String
)
