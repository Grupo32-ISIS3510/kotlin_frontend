package com.app.secondserving.ui.inventory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel

private val GreenDark = Color(0xFF386641)
private val UrgencyRed = Color(0xFFE53935)
private val UrgencyYellow = Color(0xFFFB8C00)
private val UrgencyGreen = Color(0xFF6A994E)
private val BackgroundColor = Color(0xFFF5F5F0)
private val CardColor = Color.White

private val CATEGORIES = listOf("Todos", "Frutas", "Verduras", "Lácteos", "Carnes", "Otros")

@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    weatherViewModel: WeatherViewModel = viewModel(),
    onItemClick: (InventoryItemUi, String) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val expiredItems by viewModel.expiredItems.collectAsState()
    val weatherState by weatherViewModel.weatherState.collectAsState()
    val context = LocalContext.current
    var showExpiringSheet by remember { mutableStateOf(false) }
    var showExpiredAlert by remember { mutableStateOf(false) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) weatherViewModel.loadWeather()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadInventory(showLoading = false)
                locationLauncher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(expiredItems) {
        if (expiredItems.isNotEmpty() && ExpiredAlertPreferences.shouldShowExpiredAlert(context)) {
            showExpiredAlert = true
        }
    }

    val itemCount = if (uiState is InventoryUiState.Success) {
        (uiState as InventoryUiState.Success).items.size
    } else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Mi Despensa",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = "$itemCount productos registrados",
                    fontSize = 14.sp,
                    color = GreenDark,
                    fontWeight = FontWeight.Medium
                )
            }
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                IconButton(onClick = { showExpiringSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Alimentos próximos a vencer",
                        tint = GreenDark
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (val ws = weatherState) {
            is WeatherUiState.Success -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🌤️", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${ws.weather.city} · ${ws.weather.temperature.toInt()}°C",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = GreenDark
                            )
                            Text(
                                text = "Humedad ${ws.weather.humidity}% · ${ws.weather.description}",
                                fontSize = 12.sp,
                                color = Color(0xFF555555)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            is WeatherUiState.Loading -> {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = GreenDark
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            else -> {}
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            placeholder = { Text("Buscar alimentos...", color = Color.Gray) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GreenDark,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CATEGORIES) { category ->
                val selected = category == selectedCategory
                Surface(
                    onClick = { viewModel.onCategorySelected(category) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (selected) GreenDark else Color.White,
                    shadowElevation = if (selected) 0.dp else 1.dp
                ) {
                    Text(
                        text = category,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = if (selected) Color.White else Color(0xFF555555),
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "INVENTARIO",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 1.sp
            )
            Text(
                text = "Vence pronto ▼",
                fontSize = 13.sp,
                color = GreenDark,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (val state = uiState) {
                is InventoryUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GreenDark)
                    }
                }
                is InventoryUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No se pudo cargar el inventario", color = Color.Gray, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.loadInventory() },
                                colors = ButtonDefaults.buttonColors(containerColor = GreenDark)
                            ) { Text("Reintentar") }
                        }
                    }
                }
                is InventoryUiState.Success -> {
                    if (state.items.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Text("🛒", fontSize = 48.sp)
                                Text(
                                    text = "Tu despensa está vacía",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1A1A)
                                )
                                Text(
                                    text = "Agrega tu primer producto con el botón + para empezar a gestionar tu inventario",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            modifier = Modifier.fillMaxSize(),
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(state.items) { item ->
                                val tip = weatherViewModel.getStorageTip(item.name)
                                InventoryCard(
                                    item = item,
                                    storageTip = tip,
                                    onClick = { onItemClick(item, tip) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExpiringSheet) {
        val expiring = (uiState as? InventoryUiState.Success)
            ?.items
            ?.filter { it.daysRemaining <= 3 }
            ?.sortedBy { it.daysRemaining }
            .orEmpty()
        ExpiringItemsSheet(
            items = expiring,
            onDismiss = { showExpiringSheet = false }
        )
    }

    if (showExpiredAlert) {
        ExpiredItemsAlert(
            items = expiredItems,
            onHideForToday = {
                ExpiredAlertPreferences.hideExpiredAlertFor24Hours(context)
                showExpiredAlert = false
            },
            onDismiss = { showExpiredAlert = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpiringItemsSheet(
    items: List<InventoryItemUi>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Próximos a vencer",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (items.isEmpty())
                    "Todos tus alimentos están en buen estado 🎉"
                else
                    "${items.size} ${if (items.size == 1) "producto vence" else "productos vencen"} en los próximos 3 días",
                fontSize = 13.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (items.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    items.forEach { item -> ExpiringRow(item) }
                }
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ExpiringRow(item: InventoryItemUi) {
    val (label, color) = when {
        item.daysRemaining < 0L -> "Vencido hace ${kotlin.math.abs(item.daysRemaining)} días" to UrgencyRed
        item.daysRemaining == 0L -> "Vence hoy" to UrgencyRed
        item.daysRemaining == 1L -> "Vence mañana" to UrgencyRed
        else -> "En ${item.daysRemaining} días" to UrgencyYellow
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8F8F5),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = "${item.quantity} ${if (item.quantity == 1) "unidad" else "unidades"}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = color
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ExpiredItemsAlert(
    items: List<InventoryItemUi>,
    onHideForToday: () -> Unit,
    onDismiss: () -> Unit
) {
    if (items.isEmpty()) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Tienes productos vencidos",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Revisa estos productos para evitar riesgos de consumo:",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
                items.forEach { item ->
                    val status = if (item.daysRemaining == 0L) {
                        "vence hoy"
                    } else {
                        "vencido hace ${kotlin.math.abs(item.daysRemaining)} días"
                    }
                    Text(
                        text = "• ${item.name}: $status",
                        fontSize = 14.sp,
                        color = Color(0xFF1A1A1A)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Entendido", color = GreenDark)
            }
        },
        dismissButton = {
            TextButton(onClick = onHideForToday) {
                Text("No volver a mostrar hoy", color = Color.Gray)
            }
        }
    )
}

@Composable
private fun InventoryCard(
    item: InventoryItemUi,
    storageTip: String,
    onClick: () -> Unit = {}
) {
    val urgencyColor = when (item.urgency) {
        Urgency.RED -> UrgencyRed
        Urgency.YELLOW -> UrgencyYellow
        Urgency.GREEN -> UrgencyGreen
    }
    val daysLabel = when {
        item.daysRemaining < 0L -> "VENCIDO"
        item.daysRemaining == 1L -> "1 DÍA"
        else -> "${maxOf(0, item.daysRemaining)} DÍAS"
    }
    val progress = (maxOf(0L, item.daysRemaining) / 14f).coerceIn(0f, 1f)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFFF0F0F0), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = Color(0xFFCCCCCC),
                    modifier = Modifier.size(48.dp).align(Alignment.Center)
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = urgencyColor
                ) {
                    Text(
                        text = daysLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1A1A1A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.quantity} ${if (item.quantity == 1) "unidad" else "unidades"}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(4.dp)),
                    color = urgencyColor,
                    trackColor = Color(0xFFE0E0E0)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = storageTip,
                    fontSize = 11.sp,
                    color = Color(0xFF555555),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
            }
        }
    }
}