package com.app.secondserving

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.secondserving.data.InventoryRepository
import com.app.secondserving.data.SavingsCache
import com.app.secondserving.data.SessionManager
import com.app.secondserving.ui.home.HomeScreen
import com.app.secondserving.ui.home.HomeViewModel
import com.app.secondserving.ui.home.HomeViewModelFactory
import com.app.secondserving.data.scan.ReceiptScanner
import com.app.secondserving.ui.inventory.AddItemScreen
import com.app.secondserving.ui.inventory.ExpiredAlertPreferences
import com.app.secondserving.ui.inventory.InventoryItemUi
import com.app.secondserving.ui.inventory.InventoryScreen
import com.app.secondserving.ui.inventory.InventoryViewModel
import com.app.secondserving.ui.inventory.InventoryViewModelFactory
import com.app.secondserving.ui.inventory.ItemDetailScreen
import com.app.secondserving.ui.inventory.WeatherViewModel
import com.app.secondserving.ui.login.LoginActivity
import com.app.secondserving.ui.recipes.RecipeDetailScreen
import com.app.secondserving.ui.recipes.RecipeScreen
import com.app.secondserving.ui.recipes.RecipeViewModel
import com.app.secondserving.ui.recipes.RecipeViewModelFactory
import com.app.secondserving.ui.scan.ReviewScanScreen
import com.app.secondserving.ui.scan.ScanReceiptScreen
import com.app.secondserving.ui.scan.ScanViewModel
import com.app.secondserving.ui.scan.ScanViewModelFactory
import com.app.secondserving.ui.analytics.RecipeImpactScreen
import com.app.secondserving.ui.analytics.RecipeImpactViewModel
import com.app.secondserving.ui.analytics.RecipeImpactViewModelFactory
import com.app.secondserving.ui.segment.UserSegmentScreen
import com.app.secondserving.ui.segment.UserSegmentViewModel
import com.app.secondserving.ui.segment.UserSegmentViewModelFactory
import com.app.secondserving.ui.theme.MyApplicationTheme
import com.app.secondserving.data.AnalyticsRepository
import com.app.secondserving.data.repository.RecipeRepository
import com.app.secondserving.notifications.InventorySyncWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        logNotificationOpenedIfFromIntent()
        setContent {
            MyApplicationTheme {
                MyApplicationApp()
            }
        }
    }

    private fun logNotificationOpenedIfFromIntent() {
        val app = application as? SecondServingApp ?: return
        val analytics: AnalyticsRepository = app.analyticsRepository
        intent?.let { i ->
            when {
                i.hasExtra("expiring_item_id") -> analytics.logNotificationOpened(
                    mapOf(
                        "source" to "expiration_notifier",
                        "item_id" to (i.getStringExtra("expiring_item_id") ?: "")
                    )
                )
                i.hasExtra(SecondServingMessagingService.EXTRA_NAVIGATE_TO) -> analytics.logNotificationOpened(
                    mapOf(
                        "source" to "fcm",
                        "navigate_to" to (i.getStringExtra(SecondServingMessagingService.EXTRA_NAVIGATE_TO) ?: "")
                    )
                )
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
    val context = LocalContext.current
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.INICIO) }
    var showAddItem by remember { mutableStateOf(false) }
    var showScanReceipt by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showPreferencesDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<InventoryItemUi?>(null) }
    var selectedItemTip by remember { mutableStateOf("") }
    var selectedRecipe by remember { mutableStateOf<com.app.secondserving.data.network.Recipe?>(null) }
    var showUserSegment by remember { mutableStateOf(false) }
    var showRecipeImpact by remember { mutableStateOf(false) }

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

    val isOnline by (app?.networkMonitor?.isOnline?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(true) })

    // Sincronizar automáticamente cuando vuelve el internet
    LaunchedEffect(isOnline) {
        if (isOnline) {
            InventorySyncWorker.schedule(context)
        }
    }

    val database = com.app.secondserving.data.local.AppDatabase.getDatabase(context.applicationContext)
    val sharedRepository = app?.inventoryRepository ?: InventoryRepository(
        database,
        savingsCache = SavingsCache(context.applicationContext)
    )

    val inventoryViewModel: InventoryViewModel = viewModel(
        factory = InventoryViewModelFactory(sharedRepository, app?.expirationNotifier)
    )
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(sharedRepository)
    )
    val recipeViewModel: RecipeViewModel = viewModel(
        factory = RecipeViewModelFactory(RecipeRepository())
    )
    val analyticsRepository = app?.analyticsRepository ?: AnalyticsRepository()
    val userSegmentViewModel: UserSegmentViewModel = viewModel(
        factory = UserSegmentViewModelFactory(analyticsRepository)
    )
    val recipeImpactViewModel: RecipeImpactViewModel = viewModel(
        factory = RecipeImpactViewModelFactory(analyticsRepository)
    )
    val scanViewModel: ScanViewModel = viewModel(
        factory = ScanViewModelFactory(
            scanner = ReceiptScanner(context.applicationContext),
            inventoryRepository = sharedRepository,
            networkMonitor = app?.networkMonitor
        )
    )
    val scanReviewState by scanViewModel.reviewState.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val performLogout = {
        coroutineScope.launch {
            sharedRepository.clearUserData()
            SessionManager(context).clearSession()
            val intent = Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
            (context as? ComponentActivity)?.finish()
        }
        Unit
    }

    BackHandler(
        enabled = selectedItem != null ||
            selectedRecipe != null ||
            showUserSegment ||
            showRecipeImpact ||
            showScanReceipt ||
            showAddItem ||
            scanReviewState.items.isNotEmpty() ||
            currentDestination != AppDestinations.INICIO
    ) {
        when {
            selectedItem != null -> selectedItem = null
            selectedRecipe != null -> selectedRecipe = null
            showUserSegment -> showUserSegment = false
            showRecipeImpact -> showRecipeImpact = false
            showScanReceipt -> {
                scanViewModel.resetReviewState()
                scanViewModel.resetState()
                showScanReceipt = false
                showAddItem = true
            }
            showAddItem -> showAddItem = false
            scanReviewState.items.isNotEmpty() -> {
                scanViewModel.resetState()
                showScanReceipt = true
            }
            currentDestination != AppDestinations.INICIO -> {
                currentDestination = AppDestinations.INICIO
            }
        }
    }

    if (selectedItem != null) {
        val weatherVm: WeatherViewModel = viewModel()
        val actionState by inventoryViewModel.actionState.collectAsStateWithLifecycle()
        ItemDetailScreen(
            item = selectedItem!!,
            storageTip = selectedItemTip,
            weatherState = weatherVm.weatherState.collectAsState().value,
            actionState = actionState,
            onConsume = { itemId -> inventoryViewModel.consumeItem(itemId) },
            onDiscard = { itemId, reason -> inventoryViewModel.discardItem(itemId, reason) },
            onActionConsumed = { inventoryViewModel.resetActionState() },
            onNavigateBack = { selectedItem = null }
        )
        return
    }

    if (selectedRecipe != null) {
        RecipeDetailScreen(
            recipe = selectedRecipe!!,
            viewModel = recipeViewModel,
            onNavigateBack = { selectedRecipe = null }
        )
        return
    }

    if (showUserSegment) {
        UserSegmentScreen(
            viewModel = userSegmentViewModel,
            onNavigateBack = { showUserSegment = false }
        )
        return
    }

    if (showRecipeImpact) {
        RecipeImpactScreen(
            viewModel = recipeImpactViewModel,
            onNavigateBack = { showRecipeImpact = false }
        )
        return
    }

    if (showScanReceipt) {
        ScanReceiptScreen(
            viewModel = scanViewModel,
            onItemsScanned = { _, _ ->
                showScanReceipt = false
            },
            onNavigateBack = {
                scanViewModel.resetReviewState()
                scanViewModel.resetState()
                showScanReceipt = false
                showAddItem = true
            }
        )
        return
    }

    if (showAddItem) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AddItemScreen(
                viewModel = inventoryViewModel,
                onNavigateBack = { showAddItem = false },
                onOpenScanner = {
                    showAddItem = false
                    showScanReceipt = true
                }
            )
            return
        } else {
            Toast.makeText(context, "Esta funcionalidad requiere Android 8.0+", Toast.LENGTH_SHORT).show()
            showAddItem = false
        }
    }

    if (scanReviewState.items.isNotEmpty()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ReviewScanScreen(
                viewModel = scanViewModel,
                onConfirm = {
                    val requests = scanViewModel.getInventoryRequests()
                    inventoryViewModel.createInventoryItems(requests)
                    Toast.makeText(context, "Agregando ${requests.size} productos...", Toast.LENGTH_SHORT).show()
                    scanViewModel.resetState()
                    scanViewModel.resetReviewState()
                    currentDestination = AppDestinations.DESPENSA
                },
                onNavigateBack = {
                    showScanReceipt = true
                }
            )
            return
        } else {
             Toast.makeText(context, "Esta funcionalidad requiere Android 8.0+", Toast.LENGTH_SHORT).show()
             scanViewModel.resetState()
             scanViewModel.resetReviewState()
        }
    }

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
                AppDestinations.INICIO -> HomeScreen(
                    viewModel = homeViewModel,
                    inventoryViewModel = inventoryViewModel,
                    userName = SessionManager(context).getFullName().orEmpty(),
                    onNavigateToProfile = { currentDestination = AppDestinations.PERFIL },
                    onNavigateToSegment = { showUserSegment = true },
                    onNavigateToImpact = { showRecipeImpact = true },
                    isOnline = isOnline
                )
                AppDestinations.DESPENSA -> InventoryScreen(
                    viewModel = inventoryViewModel,
                    onItemClick = { item, tip ->
                        selectedItem = item
                        selectedItemTip = tip
                    },
                    isOnline = isOnline
                )
                AppDestinations.RECETAS -> {
                    RecipeScreen(
                        viewModel = recipeViewModel,
                        onRecipeClick = { recipe -> selectedRecipe = recipe }
                    )
                }
                AppDestinations.PERFIL -> ProfileScreen(
                    onOpenPreferences = { showPreferencesDialog = true },
                    onLogout = { showLogoutDialog = true }
                )
            }
        }
    }

    if (showPreferencesDialog) {
        AlertDialog(
            onDismissRequest = { showPreferencesDialog = false },
            title = { Text("Preferencias") },
            text = {
                Text("Si seleccionas esta opción, el aviso de productos vencidos se mostrará siempre al entrar al inventario (se desactiva el \"no mostrar hoy\").")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        ExpiredAlertPreferences.resetExpiredAlertVisibility(context)
                        showPreferencesDialog = false
                    }
                ) {
                    Text("Activar: volver a mostrar siempre", color = Color(0xFF386641))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPreferencesDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Seguro que quieres cerrar sesión?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        performLogout()
                    }
                ) {
                    Text("Cerrar sesión", color = Color(0xFFB71C1C))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ProfileScreen(
    onOpenPreferences: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember(context) { SessionManager(context) }
    val greenDark = Color(0xFF386641)
    val greenLight = Color(0xFFE8F5E9)
    val background = Color(0xFFF5F5F0)
    val fullName = sessionManager.getFullName().orEmpty().ifBlank { "Usuario" }
    val email = sessionManager.getEmail().orEmpty().ifBlank { "Sin correo registrado" }
    val initial = fullName.firstOrNull()?.uppercase() ?: "?"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Mi Perfil",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = greenLight,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initial,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = greenDark
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = fullName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = email,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(20.dp))

                StatsRow(greenDark = greenDark, greenLight = greenLight)

                Spacer(modifier = Modifier.height(24.dp))

                ProfileOptionRow(label = "Notificaciones", greenDark = greenDark)
                ProfileOptionRow(
                    label = "Preferencias",
                    greenDark = greenDark,
                    onClick = onOpenPreferences
                )
                ProfileOptionRow(label = "Ayuda y soporte", greenDark = greenDark)
                ProfileOptionRow(label = "Acerca de", greenDark = greenDark)
                ProfileOptionRow(
                    label = "Cerrar sesión",
                    greenDark = greenDark,
                    isDestructive = true,
                    onClick = onLogout
                )
            }
        }
    }
}

@Composable
private fun StatsRow(greenDark: Color, greenLight: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(value = "20", label = "Productos", greenDark = greenDark, greenLight = greenLight)
        StatCard(value = "3", label = "Recetas", greenDark = greenDark, greenLight = greenLight)
        StatCard(value = "12", label = "Rescatados", greenDark = greenDark, greenLight = greenLight)
    }
}

@Composable
private fun RowScope.StatCard(
    value: String,
    label: String,
    greenDark: Color,
    greenLight: Color
) {
    Surface(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(14.dp),
        color = greenLight
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = greenDark
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = greenDark
            )
        }
    }
}

@Composable
private fun ProfileOptionRow(
    label: String,
    greenDark: Color,
    isDestructive: Boolean = false,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8F8F5)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 15.sp,
                color = if (isDestructive) Color(0xFFB71C1C) else Color(0xFF1A1A1A),
                fontWeight = if (isDestructive) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "›",
                fontSize = 20.sp,
                color = greenDark
            )
        }
    }
}
