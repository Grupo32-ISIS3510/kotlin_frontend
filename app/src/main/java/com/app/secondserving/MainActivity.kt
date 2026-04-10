package com.app.secondserving

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.secondserving.data.InventoryRepository
import com.app.secondserving.data.ScannedItem
import com.app.secondserving.ui.inventory.AddItemScreen
import com.app.secondserving.ui.inventory.InventoryItemUi
import com.app.secondserving.ui.inventory.InventoryScreen
import com.app.secondserving.ui.inventory.InventoryViewModel
import com.app.secondserving.ui.inventory.InventoryViewModelFactory
import com.app.secondserving.ui.inventory.ItemDetailScreen
import com.app.secondserving.ui.inventory.ReviewScannedItemsScreen
import com.app.secondserving.ui.inventory.ScanReceiptScreen
import com.app.secondserving.ui.inventory.WeatherViewModel
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
    DESPENSA("Despensa", Icons.AutoMirrored.Filled.List),
    RECETAS("Recetas", Icons.AutoMirrored.Filled.MenuBook),
    PERFIL("Perfil", Icons.Default.AccountCircle),
}

@Composable
fun MyApplicationApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.DESPENSA) }
    var showAddItem by rememberSaveable { mutableStateOf(false) }
    var showScanFromAdd by rememberSaveable { mutableStateOf(false) }
    var showReviewItems by rememberSaveable { mutableStateOf(false) }
    // Data kept in regular remember - will reset on rotation but navigation state is preserved
    var scannedItems by remember { mutableStateOf<List<ScannedItem>?>(null) }
    var scannedPurchaseDate by remember { mutableStateOf<String?>(null) }
    var selectedItem by remember { mutableStateOf<InventoryItemUi?>(null) }
    var selectedItemTip by remember { mutableStateOf("") }
    // Pedir permiso de notificaciones (Android 13+)
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

    val inventoryViewModel: InventoryViewModel = viewModel(
        factory = InventoryViewModelFactory(
            app?.inventoryRepository ?: InventoryRepository(
                com.app.secondserving.data.local.AppDatabase.getDatabase(
                    androidx.compose.ui.platform.LocalContext.current.applicationContext
                )
            )
        )
    )

    // Flujo de escaneo desde AddItemScreen: Escanear → Revisión → Guardar
    if (showScanFromAdd) {
        androidx.activity.compose.BackHandler {
            showScanFromAdd = false
            showAddItem = true
        }
        ScanReceiptScreen(
            onItemsScanned = { items, date ->
                scannedItems = items
                scannedPurchaseDate = date
                showScanFromAdd = false
                showReviewItems = true
            },
            onNavigateBack = {
                showScanFromAdd = false
                showAddItem = true
            }
        )
        return
    }

    if (showReviewItems && scannedItems != null) {
        androidx.activity.compose.BackHandler {
            showReviewItems = false
            scannedItems = null
            scannedPurchaseDate = null
            showScanFromAdd = true
        }
        ReviewScannedItemsScreen(
            scannedItems = scannedItems!!,
            purchaseDate = scannedPurchaseDate,
            onSaveItems = { items, date ->
                inventoryViewModel.createInventoryItemsBulk(
                    items.map { editable ->
                        ScannedItem(
                            name = editable.name,
                            price = editable.price,
                            category = editable.category
                        )
                    },
                    date
                )
                showReviewItems = false
                scannedItems = null
                scannedPurchaseDate = null
                showAddItem = false
            },
            onNavigateBack = {
                showReviewItems = false
                scannedItems = null
                scannedPurchaseDate = null
                showScanFromAdd = true
            }
        )
        return
    } else if (showReviewItems && scannedItems == null) {
        // Data lost on rotation, go back
        showReviewItems = false
    }

    if (showAddItem) {
        androidx.activity.compose.BackHandler {
            showAddItem = false
        }
        AddItemScreen(
            viewModel = inventoryViewModel,
            onNavigateBack = { showAddItem = false },
            onOpenScanner = {
                showAddItem = false
                showScanFromAdd = true
            }
        )
        return
    }

    if (selectedItem != null) {
        androidx.activity.compose.BackHandler {
            selectedItem = null
            selectedItemTip = ""
        }
        val weatherVm: WeatherViewModel = viewModel()
        ItemDetailScreen(
            item = selectedItem!!,
            storageTip = selectedItemTip,
            weatherState = weatherVm.weatherState.collectAsState().value,
            onNavigateBack = { selectedItem = null }
        )
        return
    }

    val green = Color(0xFF386641)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavItem(
                        label = "Inicio",
                        icon = Icons.Default.Home,
                        selected = currentDestination == AppDestinations.INICIO,
                        onClick = { currentDestination = AppDestinations.INICIO },
                        modifier = Modifier.weight(1f)
                    )
                    NavItem(
                        label = "Despensa",
                        icon = Icons.AutoMirrored.Filled.List,
                        selected = currentDestination == AppDestinations.DESPENSA,
                        onClick = { currentDestination = AppDestinations.DESPENSA },
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier.weight(1.2f),
                        contentAlignment = Alignment.Center
                    ) {
                        FloatingActionButton(
                            onClick = {
                                inventoryViewModel.resetAddItemState()
                                showAddItem = true
                            },
                            containerColor = green,
                            contentColor = Color.White,
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Agregar producto",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    NavItem(
                        label = "Recetas",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        selected = currentDestination == AppDestinations.RECETAS,
                        onClick = { currentDestination = AppDestinations.RECETAS },
                        modifier = Modifier.weight(1f)
                    )
                    NavItem(
                        label = "Perfil",
                        icon = Icons.Default.AccountCircle,
                        selected = currentDestination == AppDestinations.PERFIL,
                        onClick = { currentDestination = AppDestinations.PERFIL },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                AppDestinations.DESPENSA -> InventoryScreen(
                    viewModel = inventoryViewModel,
                    onItemClick = { item, tip ->
                        selectedItem = item
                        selectedItemTip = tip
                    }
                )
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

@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val green = Color(0xFF386641)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) green else Color.Gray,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            label,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            maxLines = 1,
            color = if (selected) green else Color.Gray
        )
    }
}
