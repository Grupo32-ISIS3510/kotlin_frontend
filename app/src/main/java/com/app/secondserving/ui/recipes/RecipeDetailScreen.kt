package com.app.secondserving.ui.recipes

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val cookState by viewModel.cookState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(cookState) {
        if (cookState is CookUiState.Success) {
            Toast.makeText(context, "¡Buen provecho! Ingredientes actualizados.", Toast.LENGTH_LONG).show()
            viewModel.resetCookState()
            onNavigateBack()
        } else if (cookState is CookUiState.Error) {
            Toast.makeText(context, (cookState as CookUiState.Error).message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe.title, fontWeight = FontWeight.Bold, color = GreenDark) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = GreenDark)
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
            // Recipe Description
            recipe.description?.let {
                Text(
                    text = it,
                    fontSize = 16.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            // Ingredients Section
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
                    recipe.ingredients.forEach { ingredient ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isMatched = recipe.matched_ingredients.any { 
                                it.equals(ingredient.name, ignoreCase = true) 
                            }
                            
                            Icon(
                                imageVector = if (isMatched) Icons.Default.Done else Icons.Default.Kitchen,
                                contentDescription = null,
                                tint = if (isMatched) GreenDark else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${ingredient.quantity} ${ingredient.unit ?: ""} ${ingredient.name}",
                                fontSize = 14.sp,
                                color = if (isMatched) GreenDark else Color.Black,
                                fontWeight = if (isMatched) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Instructions Section
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
                    text = recipe.instructions ?: "No hay instrucciones disponibles.",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Cook Button
            Button(
                onClick = { viewModel.cookRecipe(recipe.id) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenDark),
                shape = RoundedCornerShape(12.dp),
                enabled = cookState !is CookUiState.Loading
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
