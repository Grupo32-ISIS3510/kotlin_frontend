package com.app.secondserving.data.network

data class Recipe(
    val id: String,
    val title: String,
    val description: String?,
    val instructions: String?, // Or steps
    val ingredients: List<RecipeIngredient>,
    val matched_ingredients: List<String>,
    val soonest_expiry_days: Int?,
    val score: Double?
)

data class RecipeIngredient(
    val name: String,
    val quantity: String,
    val unit: String?
)

// Body que espera POST /recipes/{id}/interact en el backend.
// action solo puede ser "viewed" o "cooked"; el backend lo valida y rechaza
// cualquier otro valor con 422.
data class RecipeInteractionRequest(
    val action: String
)
