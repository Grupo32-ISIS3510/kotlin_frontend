package com.app.secondserving.ui.inventory

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.app.secondserving.data.ScannedItem
import com.app.secondserving.data.ShelfLifePredictor
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val GreenDark = Color(0xFF386641)
private val BackgroundColor = Color(0xFFF5F5F0)
private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/**
 * Item editable para la pantalla de revisión.
 */
data class EditableScannedItem(
    val id: Int,
    var name: String,
    var quantity: Double,
    var price: Double?,
    var category: String,
    var expiryDate: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScannedItemsScreen(
    scannedItems: List<ScannedItem>,
    purchaseDate: String?,
    onSaveItems: (List<EditableScannedItem>, String?) -> Unit,
    onNavigateBack: () -> Unit
) {
    val today = LocalDate.now()
    var selectedPurchaseDate by remember {
        mutableStateOf(purchaseDate ?: today.format(DATE_FORMAT))
    }
    var showPurchaseDatePicker by remember { mutableStateOf(false) }

    var editableItems by remember(scannedItems) {
        mutableStateOf(
            scannedItems.mapIndexed { index, item ->
                val predictedExpiry = ShelfLifePredictor.predictExpiryDate(
                    selectedPurchaseDate,
                    item.category
                )
                EditableScannedItem(
                    id = index,
                    name = item.name,
                    quantity = 1.0,
                    price = item.price,
                    category = item.category,
                    expiryDate = predictedExpiry
                )
            }
        )
    }

    // Update expiry dates when purchase date changes
    val currentDate = selectedPurchaseDate
    LaunchedEffect(currentDate) {
        editableItems = editableItems.map { item ->
            val predictedExpiry = ShelfLifePredictor.predictExpiryDate(currentDate, item.category)
            item.copy(expiryDate = predictedExpiry)
        }
    }

    var showDeleteDialog by remember { mutableStateOf<Int?>(null) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Calculate total
    val totalAmount = editableItems.sumOf { it.price ?: 0.0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Revisar Productos",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            isSaving = true
                            onSaveItems(editableItems, selectedPurchaseDate)
                        },
                        enabled = editableItems.isNotEmpty() && !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Guardar", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GreenDark
                )
            )
        },
        containerColor = BackgroundColor,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddItemDialog = true },
                containerColor = GreenDark,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar producto")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (editableItems.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No hay productos para revisar",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Toca el botón + para agregar productos manualmente",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header with purchase date and summary
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = GreenDark,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Fecha de compra:",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = GreenDark
                                        )
                                    }
                                    TextButton(onClick = { showPurchaseDatePicker = true }) {
                                        Text(
                                            text = selectedPurchaseDate,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = GreenDark
                                        )
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Cambiar fecha",
                                            tint = GreenDark,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${editableItems.size} producto${if (editableItems.size != 1) "s" else ""}",
                                        fontSize = 13.sp,
                                        color = Color(0xFF555555)
                                    )
                                    Text(
                                        text = "Total: $${String.format("%.2f", totalAmount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = GreenDark
                                    )
                                }
                            }
                        }
                    }

                    itemsIndexed(editableItems, key = { _, item -> item.id }) { index, item ->
                        EditableItemCard(
                            item = item,
                            onNameChange = { newName ->
                                editableItems = editableItems.map { 
                                    if (it.id == item.id) it.copy(name = newName) else it 
                                }
                            },
                            onQuantityChange = { newQty ->
                                editableItems = editableItems.map { 
                                    if (it.id == item.id) it.copy(quantity = newQty) else it 
                                }
                            },
                            onPriceChange = { newPrice ->
                                editableItems = editableItems.map { 
                                    if (it.id == item.id) it.copy(price = newPrice) else it 
                                }
                            },
                            onCategoryChange = { newCategory ->
                                editableItems = editableItems.map { 
                                    if (it.id == item.id) it.copy(category = newCategory) else it 
                                }
                            },
                            onExpiryDateChange = { newDate ->
                                editableItems = editableItems.map { 
                                    if (it.id == item.id) it.copy(expiryDate = newDate) else it 
                                }
                            },
                            onDeleteClick = { showDeleteDialog = item.id }
                        )
                    }

                    item {
                        // Total summary card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Total estimado:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = GreenDark
                                )
                                Text(
                                    "$${String.format("%.2f", totalAmount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = GreenDark
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
                    }
                }
            }
        }
    }

    // Purchase date picker dialog
    if (showPurchaseDatePicker) {
        var selectedYear by remember { mutableIntStateOf(today.year) }
        var selectedMonth by remember { mutableIntStateOf(today.monthValue - 1) }
        var selectedDay by remember { mutableIntStateOf(today.dayOfMonth) }

        AlertDialog(
            onDismissRequest = { showPurchaseDatePicker = false },
            title = { Text("Fecha de compra") },
            text = {
                android.widget.DatePicker(
                    androidx.compose.ui.platform.LocalContext.current
                ).apply {
                    updateDate(selectedYear, selectedMonth, selectedDay)
                    setOnDateChangedListener { _, year, month, day ->
                        selectedYear = year
                        selectedMonth = month
                        selectedDay = day
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedPurchaseDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                        showPurchaseDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurchaseDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { itemId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Eliminar producto") },
            text = { Text("¿Estás seguro de eliminar este producto de la lista?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        editableItems = editableItems.filter { it.id != itemId }
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Add new item dialog
    if (showAddItemDialog) {
        AddItemDialog(
            onDismiss = { showAddItemDialog = false },
            onAddItem = { name, quantity, price, category, expiryDate ->
                editableItems = editableItems + EditableScannedItem(
                    id = (editableItems.maxOfOrNull { it.id } ?: -1) + 1,
                    name = name,
                    quantity = quantity,
                    price = price,
                    category = category,
                    expiryDate = expiryDate
                )
                showAddItemDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditableItemCard(
    item: EditableScannedItem,
    onNameChange: (String) -> Unit,
    onQuantityChange: (Double) -> Unit,
    onPriceChange: (Double?) -> Unit,
    onCategoryChange: (String) -> Unit,
    onExpiryDateChange: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    var nameExpanded by remember { mutableStateOf(false) }
    var quantityExpanded by remember { mutableStateOf(false) }
    var priceExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var expiryExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with name and delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name.ifBlank { "Sin nombre" },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color(0xFF333333),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = Color.Red
                    )
                }
            }

            // Name field
            OutlinedTextField(
                value = item.name,
                onValueChange = onNameChange,
                label = { Text("Nombre del producto") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenDark,
                    focusedLabelColor = GreenDark
                )
            )

            // Quantity and Price in a row (2 columns)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quantity
                OutlinedTextField(
                    value = item.quantity.toString(),
                    onValueChange = {
                        it.toDoubleOrNull()?.let { qty -> onQuantityChange(qty) }
                        if (it.isEmpty()) onQuantityChange(0.0)
                    },
                    label = { Text("Cantidad") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenDark,
                        focusedLabelColor = GreenDark
                    )
                )

                // Price
                OutlinedTextField(
                    value = if (item.price != null) item.price.toString() else "",
                    onValueChange = {
                        if (it.isEmpty()) onPriceChange(null)
                        else it.toDoubleOrNull()?.let { price -> onPriceChange(price) }
                    },
                    label = { Text("Precio (opcional)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenDark,
                        focusedLabelColor = GreenDark
                    )
                )
            }

            // Category dropdown - full width
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = item.category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenDark,
                        focusedLabelColor = GreenDark
                    )
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    val categories = listOf(
                        "Lácteos", "Carnes", "Pescados", "Frutas",
                        "Verduras", "Granos", "Bebidas", "Condimentos",
                        "Enlatados", "Otros"
                    )
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                onCategoryChange(category)
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // Expiry date
            OutlinedTextField(
                value = item.expiryDate,
                onValueChange = onExpiryDateChange,
                label = { Text("Fecha de vencimiento (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenDark,
                    focusedLabelColor = GreenDark
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemDialog(
    onDismiss: () -> Unit,
    onAddItem: (name: String, quantity: Double, price: Double?, category: String, expiryDate: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var price by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Otros") }
    var expiryDate by remember { mutableStateOf(LocalDate.now().plusDays(7).toString()) }
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar producto manual") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Cantidad") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Precio (opcional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        val categories = listOf(
                            "Lácteos", "Carnes", "Pescados", "Frutas",
                            "Verduras", "Granos", "Bebidas", "Condimentos",
                            "Enlatados", "Otros"
                        )
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    label = { Text("Fecha de vencimiento") },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onAddItem(
                            name,
                            quantity.toDoubleOrNull() ?: 1.0,
                            price.toDoubleOrNull(),
                            category,
                            expiryDate
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )

    // Handle system back button
    BackHandler(onBack = onDismiss)
}
