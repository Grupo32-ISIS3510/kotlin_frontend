package com.app.secondserving.data.network

// Wrapper que envolvía la respuesta de recetas en la versión inicial del
// backend ({"recipes": [...]}). Ya no se usa: el endpoint actual
// /recipes/suggestions devuelve directamente una List<Recipe> y por eso
// ApiService.getRecipes() está tipado como Response<List<Recipe>>.
//
// Comentado en lugar de borrado por consistencia con el resto del refactor:
// si el backend cambia y vuelve a envolver la respuesta en un objeto,
// este data class queda listo para reactivarlo.

/*
data class RecipeResponse(
    val recipes: List<Recipe>?
)
*/
