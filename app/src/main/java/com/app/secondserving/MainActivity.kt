package com.app.secondserving

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.secondserving.ui.inventory.InventoryViewModel
import com.app.secondserving.data.InventoryRepository
import com.app.secondserving.ui.inventory.InventoryScreen
import com.app.secondserving.ui.inventory.InventoryViewModelFactory
import com.app.secondserving.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MyApplicationApp()
            }
        }
    }
}

enum class AppDestinations(val label: String, val icon: ImageVector) {
    INICIO("Inicio", Icons.Default.Home),
    DESPENSA("Despensa", Icons.Default.List),
    RECETAS("Recetas", Icons.Default.MenuBook),
    PERFIL("Perfil", Icons.Default.AccountCircle),
}

@Composable
fun MyApplicationApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.DESPENSA) }

    val inventoryViewModel: InventoryViewModel = viewModel(
        factory = InventoryViewModelFactory(InventoryRepository())
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                AppDestinations.entries.forEachIndexed { index, destination ->
                    if (index == 2) {
                        // FAB placeholder slot between RECETAS and PERFIL — spacer
                        NavigationBarItem(
                            selected = false,
                            onClick = {},
                            icon = {},
                            enabled = false
                        )
                    }
                    NavigationBarItem(
                        selected = currentDestination == destination,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(destination.icon, contentDescription = destination.label)
                        },
                        label = { Text(destination.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF386641),
                            selectedTextColor = Color(0xFF386641),
                            indicatorColor = Color(0x1A386641)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {},
                containerColor = Color(0xFF386641),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar producto")
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentDestination) {
                AppDestinations.DESPENSA -> InventoryScreen(viewModel = inventoryViewModel)
                AppDestinations.INICIO -> PlaceholderScreen("Inicio")
                AppDestinations.RECETAS -> PlaceholderScreen("Recetas")
                AppDestinations.PERFIL -> PlaceholderScreen("Perfil")
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = name, style = MaterialTheme.typography.headlineMedium, color = Color.Gray)
    }
}
