package com.app.secondserving

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.secondserving.data.InventoryRepository
import com.app.secondserving.data.scan.ReceiptScanner
import com.app.secondserving.ui.inventory.AddItemScreen
import com.app.secondserving.ui.inventory.InventoryItemUi
import com.app.secondserving.ui.inventory.InventoryScreen
import com.app.secondserving.ui.inventory.InventoryViewModel
import com.app.secondserving.ui.inventory.InventoryViewModelFactory
import com.app.secondserving.ui.inventory.ItemDetailScreen
import com.app.secondserving.ui.scan.ScanReceiptScreen
import com.app.secondserving.ui.scan.ReviewScanScreen
import com.app.secondserving.ui.inventory.WeatherViewModel
import com.app.secondserving.ui.recipes.RecipeDetailScreen
import com.app.secondserving.ui.recipes.RecipeScreen
import com.app.secondserving.ui.recipes.RecipeViewModel
import com.app.secondserving.ui.recipes.RecipeViewModelFactory
import com.app.secondserving.ui.scan.ScanViewModel
import com.app.secondserving.ui.scan.ScanViewModelFactory
import com.app.secondserving.ui.theme.MyApplicationTheme
import com.app.secondserving.data.repository.RecipeRepository
import com.app.secondserving.ui.scan.ScanUiState

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
    val context = LocalContext.current
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.DESPENSA) }
    var showAddItem by remember { mutableStateOf(false) }
    var showScanReceipt by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<InventoryItemUi?>(null) }
    var selectedItemTip by remember { mutableStateOf("") }
    
    // Estado para recetas
    var selectedRecipe by remember { mutableStateOf<com.app.secondserving.data.network.Recipe?>(null) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val app = androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner.current?.let {
        it as? ComponentActivity
    }?.application as? SecondServingApp

    val database = com.app.secondserving.data.local.AppDatabase.getDatabase(context.applicationContext)
    val inventoryRepo = app?.inventoryRepository ?: InventoryRepository(database)

    val inventoryViewModel: InventoryViewModel = viewModel(
        factory = InventoryViewModelFactory(inventoryRepo)
    )

    // Inicializar ScanViewModel en este nivel para que sea compartido
    val scanViewModel: ScanViewModel = viewModel(
        factory = ScanViewModelFactory(
            ReceiptScanner(context.applicationContext),
            inventoryRepo
        )
    )
    val scanReviewState by scanViewModel.reviewState.collectAsStateWithLifecycle()

    // Lógica de navegación prioritaria (Overlays)

    // 1. Pantalla de Revisión (si hay items escaneados)
    if (scanReviewState.items.isNotEmpty() && !showScanReceipt) {
        ReviewScanScreen(
            viewModel = scanViewModel,
            onConfirm = {
                // T3.1: Ahora el mapping vive en el ViewModel (MVVM puro)
                val requests = scanViewModel.getInventoryRequests()
                inventoryViewModel.createInventoryItems(requests)

                Toast.makeText(context, "Agregando ${requests.size} productos...", Toast.LENGTH_SHORT).show()
                scanViewModel.resetState()
                currentDestination = AppDestinations.DESPENSA
            },
            onNavigateBack = {
                showScanReceipt = true
            }
        )
        return
    }

    // 2. Pantalla de Cámara
    if (showScanReceipt) {
        ScanReceiptScreen(
            viewModel = scanViewModel,
            onItemsScanned = { _, _ ->
                showScanReceipt = false
                // Los items ya están guardados en el ScanViewModel.reviewState
            },
            onNavigateBack = { 
                showScanReceipt = false
                showAddItem = true
            }
        )
        return
    }

    // 3. Pantalla de Agregar Item Manual
    if (showAddItem) {
        AddItemScreen(
            viewModel = inventoryViewModel,
            onNavigateBack = { showAddItem = false },
            onOpenScanner = {
                showAddItem = false
                showScanReceipt = true
            }
        )
        return
    }

    // 4. Detalle de Item
    if (selectedItem != null) {
        val weatherVm: WeatherViewModel = viewModel()
        ItemDetailScreen(
            item = selectedItem!!,
            storageTip = selectedItemTip,
            weatherState = weatherVm.weatherState.collectAsState().value,
            onNavigateBack = { selectedItem = null }
        )
        return
    }

    // 5. Detalle de Receta
    if (selectedRecipe != null) {
        val recipeViewModel: RecipeViewModel = viewModel(
            factory = RecipeViewModelFactory(RecipeRepository(database))
        )
        RecipeDetailScreen(
            recipe = selectedRecipe!!,
            viewModel = recipeViewModel,
            onNavigateBack = { selectedRecipe = null }
        )
        return
    }

    // Flujo Principal (BottomBar)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                AppDestinations.entries.forEachIndexed { index, destination ->
                    if (index == 2) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.CenterVertically),
                            contentAlignment = Alignment.Center
                        ) {
                            FloatingActionButton(
                                onClick = {
                                    inventoryViewModel.resetAddItemState()
                                    showAddItem = true
                                },
                                containerColor = Color(0xFF386641),
                                contentColor = Color.White,
                                modifier = Modifier.size(52.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Agregar producto")
                            }
                        }
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
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentDestination) {
                AppDestinations.DESPENSA -> InventoryScreen(
                    viewModel = inventoryViewModel,
                    onItemClick = { item, tip ->
                        selectedItem = item
                        selectedItemTip = tip
                    }
                )
                AppDestinations.INICIO -> PlaceholderScreen("Inicio")
                AppDestinations.RECETAS -> {
                    val recipeViewModel: RecipeViewModel = viewModel(
                        factory = RecipeViewModelFactory(RecipeRepository(database))
                    )
                    RecipeScreen(
                        viewModel = recipeViewModel,
                        onRecipeClick = { recipe -> selectedRecipe = recipe }
                    )
                }
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
