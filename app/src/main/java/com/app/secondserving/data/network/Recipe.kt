package com.app.secondserving.data.network

// Modelo alineado con el JSON real que devuelve GET /recipes/suggestions:
// {id, name, description, category, prep_time_minutes, servings,
//  image_url, inventory_matches}.
//
// Los campos `instructions`, `ingredients`, `matched_ingredients`,
// `soonest_expiry_days` y `score` se mantienen opcionales como hint para
// el endpoint de detalle y para preservar compatibilidad con el código UI
// existente, que cae a fallbacks (?: emptyList(), ?: "") cuando vienen null.
//
// `title` es un alias defensivo: la UI seguía leyendo `recipe.title` desde
// la versión anterior; el getter delega a `name` para que todo siga
// funcionando sin tocar la UI.
data class Recipe(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val category: String? = null,
    val prep_time_minutes: Int? = null,
    val servings: Int? = null,
    val image_url: String? = null,
    val inventory_matches: Int? = null,
    val instructions: String? = null,
    val ingredients: List<RecipeIngredient>? = null,
    val matched_ingredients: List<String>? = null,
    val soonest_expiry_days: Int? = null,
    val score: Double? = null
) {
    /** Alias para mantener compatibilidad con el código que leía `title`. */
    val title: String? get() = name
}

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
