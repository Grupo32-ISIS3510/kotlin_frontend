package com.app.secondserving.ui.recipes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.secondserving.data.network.Recipe

private val GreenDark = Color(0xFF386641)
private val BackgroundColor = Color(0xFFF5F5F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipe: Recipe,
    viewModel: RecipeViewModel,
    onNavigateBack: () -> Unit
) {
    // SSOT: Observamos el estado de la acción de cocinar desde el ViewModel
    val cookState by viewModel.cookState.collectAsStateWithLifecycle(initialValue = CookUiState.Idle)

    // El listado /suggestions no incluye ingredients ni instructions, por eso
    // pedimos el detalle completo en cuanto se monta la pantalla. Mientras
    // llega seguimos mostrando el `recipe` liviano que recibimos como argumento.
    val detail by viewModel.selectedRecipeDetail.collectAsStateWithLifecycle()
    val displayRecipe = detail ?: recipe

    // Registra la visualización de la receta en el backend (recipe_interactions).
    // Necesario para la BQ T4.1: el segmento del usuario depende del open_rate,
    // y para distinguir Proactive vs Passive el backend cuenta tanto views como cooks.
    // Solo logueamos si el id no viene null (recetas sin id no se pueden referenciar).
    LaunchedEffect(recipe.id) {
        recipe.id?.let {
            viewModel.viewRecipe(it)
            viewModel.loadRecipeDetail(it)
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.clearRecipeDetail() }
    }

    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }

    // Reaccionar a cambios en el estado (MVVM)
    LaunchedEffect(cookState) {
        when (cookState) {
            is CookUiState.Success -> {
                dialogMessage = "¡Buen provecho! Ingredientes actualizados."
                isSuccess = true
                showDialog = true
                viewModel.resetCookState()
            }
            is CookUiState.Error -> {
                dialogMessage = (cookState as CookUiState.Error).message
                isSuccess = false
                showDialog = true
            }
            else -> {}
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { /* Bloqueante */ },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        if (isSuccess) {
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenDark)
                ) {
                    Text("Aceptar")
                }
            },
            title = {
                Text(
                    text = if (isSuccess) "Éxito" else "Atención",
                    fontWeight = FontWeight.Bold,
                    color = GreenDark
                )
            },
            text = { Text(dialogMessage) },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(displayRecipe.title ?: "Sin título", fontWeight = FontWeight.Bold, color = GreenDark) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        // Corregido: Usar versión AutoMirrored
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = GreenDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundColor)
            )
        },
        containerColor = BackgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            displayRecipe.description?.let {
                Text(
                    text = it,
                    fontSize = 16.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            Text(
                text = "Ingredientes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GreenDark,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val ingredients = displayRecipe.ingredients.orEmpty()
                    val matched = displayRecipe.matched_ingredients.orEmpty()
                    if (ingredients.isEmpty()) {
                        Text(
                            text = "Esta receta no trae ingredientes detallados.",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    } else {
                        ingredients.forEach { ingredient ->
                            val name = ingredient.name.orEmpty()
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isMatched = matched.any {
                                    it.equals(name, ignoreCase = true)
                                }

                                Icon(
                                    imageVector = if (isMatched) Icons.Default.Done else Icons.Default.Kitchen,
                                    contentDescription = null,
                                    tint = if (isMatched) GreenDark else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${ingredient.quantity.orEmpty()} ${ingredient.unit ?: ""} $name".trim(),
                                    fontSize = 14.sp,
                                    color = if (isMatched) GreenDark else Color.Black,
                                    fontWeight = if (isMatched) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Instrucciones",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GreenDark,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Text(
                    text = displayRecipe.instructions ?: "No hay instrucciones disponibles.",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { displayRecipe.id?.let { viewModel.cookRecipe(it) } },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenDark),
                shape = RoundedCornerShape(12.dp),
                enabled = cookState !is CookUiState.Loading && displayRecipe.id != null
            ) {
                if (cookState is CookUiState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Cocinar esta receta", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Al cocinar, se descontarán automáticamente los ingredientes de tu despensa.",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
