package com.app.secondserving.ui.recipes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.secondserving.data.network.Recipe

private val GreenDark = Color(0xFF386641)
private val BackgroundColor = Color(0xFFF5F5F0)
private val OrangeExpiring = Color(0xFFBC4749)
private val StarColor = Color(0xFFFFB703)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeScreen(
    viewModel: RecipeViewModel,
    onRecipeClick: (Recipe) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Recomendaciones Inteligentes",
                        fontWeight = FontWeight.Bold,
                        color = GreenDark
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundColor)
            )
        },
        containerColor = BackgroundColor
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = GreenDark
                )
            } else if (uiState.error != null) {
                ErrorState(message = uiState.error!!, onRetry = { viewModel.fetchRecipes() })
            } else if (uiState.isEmpty) {
                EmptyRecipesState()
            } else {
                RecipeList(recipes = uiState.recipes, onRecipeClick = onRecipeClick)
            }
            
            // Overlay de carga cuando se está "cocinando"
            if (uiState.isCooking) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.3f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(color = GreenDark, modifier = Modifier.size(24.dp))
                                Text("Cocinando...", fontWeight = FontWeight.Medium, color = GreenDark)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeList(recipes: List<Recipe>, onRecipeClick: (Recipe) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(recipes) { recipe ->
            RecipeCard(recipe = recipe, onClick = { onRecipeClick(recipe) })
        }
    }
}

@Composable
fun RecipeCard(recipe: Recipe, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = recipe.title ?: "Sin título",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenDark,
                    modifier = Modifier.weight(1f)
                )
                
                // Smart Score Indicator
                recipe.score?.let { score ->
                    Surface(
                        color = GreenDark.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = StarColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${(score * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenDark
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // El backend a veces devuelve matched_ingredients como null,
            // por eso usamos isNullOrEmpty/joinToString con ?:.
            if (!recipe.matched_ingredients.isNullOrEmpty()) {
                Text(
                    text = "Aprovecha: ${recipe.matched_ingredients.joinToString(", ")}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            recipe.soonest_expiry_days?.let { days ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = if (days <= 3) OrangeExpiring.copy(alpha = 0.1f) else Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (days <= 3) Icons.Default.Warning else Icons.Default.RestaurantMenu,
                            contentDescription = null,
                            tint = if (days <= 3) OrangeExpiring else GreenDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (days <= 0) "Vence hoy" else "Ingredientes vencen en: $days días",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (days <= 3) OrangeExpiring else GreenDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyRecipesState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.RestaurantMenu,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.LightGray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Sin recetas por ahora",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Oops, no tenemos recetas disponibles que combinen con tu despensa. ¡Vuelve más tarde! :(",
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("No pudimos cargar las recetas", fontWeight = FontWeight.Bold, color = Color.Gray)
        Text(message, color = Color.Red.copy(alpha = 0.7f), textAlign = TextAlign.Center, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = GreenDark)) {
            Text("Reintentar")
        }
    }
}
