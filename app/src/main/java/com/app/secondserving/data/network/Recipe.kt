package com.app.secondserving.data.network

data class Recipe(
    val id: Int,
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
