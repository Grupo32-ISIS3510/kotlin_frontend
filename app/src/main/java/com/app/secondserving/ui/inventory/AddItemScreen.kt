package com.app.secondserving.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.app.secondserving.data.ShelfLifePredictor
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val GreenDark = Color(0xFF386641)
private val BackgroundColor = Color(0xFFF5F5F0)
private val CATEGORIES = listOf("Frutas", "Verduras", "Lácteos", "Carnes", "Otros")
private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    onOpenScanner: (() -> Unit)? = null
) {
    val addItemState by viewModel.addItemState.collectAsState()

    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Frutas") }
    var quantity by rememberSaveable { mutableStateOf("") }
    var purchaseDate by rememberSaveable { mutableStateOf("") }
    var expiryDate by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }

    var nameError by rememberSaveable { mutableStateOf(false) }
    var quantityError by rememberSaveable { mutableStateOf(false) }
    var purchaseDateError by rememberSaveable { mutableStateOf(false) }
    var expiryDateError by rememberSaveable { mutableStateOf(false) }

    // Predicción automática de fecha de expiración
    var showPredictionTip by rememberSaveable { mutableStateOf(false) }
    var predictedExpiryDate by rememberSaveable { mutableStateOf("") }
    var storageTip by rememberSaveable { mutableStateOf("") }

    // Selectores de fecha
    var showPurchaseDatePicker by rememberSaveable { mutableStateOf(false) }
    var showExpiryDatePicker by rememberSaveable { mutableStateOf(false) }

    // Actualizar predicción cuando cambia categoría o fecha de compra
    LaunchedEffect(category, purchaseDate) {
        if (purchaseDate.isNotBlank() && expiryDate.isBlank()) {
            predictedExpiryDate = ShelfLifePredictor.predictExpiryDate(
                purchaseDateStr = purchaseDate,
                category = category
            )
            storageTip = ShelfLifePredictor.getStorageRecommendation(category)
            showPredictionTip = true
        }
    }

    // Navegar atrás al éxito (una sola vez)
    LaunchedEffect(addItemState) {
        if (addItemState is AddItemUiState.Success) {
            onNavigateBack()
            viewModel.resetAddItemState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Agregar alimento",
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
                    // Botón para escanear factura
                    if (onOpenScanner != null) {
                        IconButton(onClick = onOpenScanner) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = "Escanear factura",
                                    tint = GreenDark
                                )
                                Text(
                                    text = "Escanear",
                                    fontSize = 10.sp,
                                    color = GreenDark
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundColor
                )
            )
        },
        containerColor = BackgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Nombre
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text("Nombre *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                isError = nameError,
                supportingText = { if (nameError) Text("El nombre es requerido") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenDark,
                    focusedLabelColor = GreenDark
                ),
                singleLine = true
            )

            // Categoría dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenDark,
                        focusedLabelColor = GreenDark
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    CATEGORIES.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = { category = option; expanded = false }
                        )
                    }
                }
            }

            // Cantidad
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it; quantityError = false },
                label = { Text("Cantidad *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                isError = quantityError,
                supportingText = { if (quantityError) Text("La cantidad debe ser un número válido (ej: 2, 1.5)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenDark,
                    focusedLabelColor = GreenDark
                ),
                singleLine = true
            )

            // Fecha de compra con botón de calendario
            OutlinedTextField(
                value = purchaseDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha de compra *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                isError = purchaseDateError,
                supportingText = { if (purchaseDateError) Text("La fecha de compra es requerida") },
                trailingIcon = {
                    IconButton(onClick = { showPurchaseDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Seleccionar fecha")
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenDark,
                    focusedLabelColor = GreenDark
                ),
                singleLine = true
            )

            // Dialog selector de fecha de compra
            if (showPurchaseDatePicker) {
                var selectedYear by rememberSaveable { mutableStateOf(LocalDate.now().year) }
                var selectedMonth by rememberSaveable { mutableStateOf(LocalDate.now().monthValue - 1) }
                var selectedDay by rememberSaveable { mutableStateOf(LocalDate.now().dayOfMonth) }
                
                AlertDialog(
                    onDismissRequest = { showPurchaseDatePicker = false },
                    title = { Text("Fecha de compra") },
                    text = {
                        android.widget.DatePicker(
                            androidx.compose.ui.platform.LocalContext.current,
                            null,
                            android.R.attr.datePickerStyle
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
                                val date = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                                purchaseDate = date
                                purchaseDateError = false
                                showPredictionTip = false
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

            // Tip de predicción
            if (showPredictionTip && predictedExpiryDate.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE8F5E9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GreenDark.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "💡 Predicción de vida útil",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = GreenDark
                        )
                        Text(
                            text = "Fecha de expiración estimada: $predictedExpiryDate",
                            fontSize = 13.sp,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            text = "Almacenamiento recomendado: ${storageTip.ifBlank { "ambiente" }}",
                            fontSize = 13.sp,
                            color = Color(0xFF555555)
                        )
                        Text(
                            text = "Puedes editar la fecha manualmente si lo prefieres",
                            fontSize = 12.sp,
                            color = Color(0xFF888888),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }

            // Fecha de vencimiento con botón de calendario
            OutlinedTextField(
                value = expiryDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha de vencimiento *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                isError = expiryDateError,
                supportingText = { if (expiryDateError) Text("La fecha de vencimiento es requerida") },
                trailingIcon = {
                    IconButton(onClick = { showExpiryDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Seleccionar fecha")
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenDark,
                    focusedLabelColor = GreenDark
                ),
                singleLine = true
            )

            // Dialog selector de fecha de expiración
            if (showExpiryDatePicker) {
                var expYear by rememberSaveable { mutableStateOf(LocalDate.now().year) }
                var expMonth by rememberSaveable { mutableStateOf(LocalDate.now().monthValue - 1) }
                var expDay by rememberSaveable { mutableStateOf(LocalDate.now().dayOfMonth) }
                
                AlertDialog(
                    onDismissRequest = { showExpiryDatePicker = false },
                    title = { Text("Fecha de vencimiento") },
                    text = {
                        android.widget.DatePicker(
                            androidx.compose.ui.platform.LocalContext.current,
                            null,
                            android.R.attr.datePickerStyle
                        ).apply {
                            updateDate(expYear, expMonth, expDay)
                            setOnDateChangedListener { _, year, month, day ->
                                expYear = year
                                expMonth = month
                                expDay = day
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val date = String.format("%04d-%02d-%02d", expYear, expMonth + 1, expDay)
                                expiryDate = date
                                expiryDateError = false
                                showPredictionTip = false
                                showExpiryDatePicker = false
                            }
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExpiryDatePicker = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // Error general
            if (addItemState is AddItemUiState.Error) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFEBEE)
                ) {
                    Text(
                        text = (addItemState as AddItemUiState.Error).message,
                        color = Color(0xFFB71C1C),
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp
                    )
                }
            }

            // Botón guardar
            Button(
                onClick = {
                    nameError = name.isBlank()
                    quantityError = quantity.toDoubleOrNull() == null
                    purchaseDateError = purchaseDate.isBlank()
                    
                    // Usar predicción si no hay fecha de expiración
                    val finalExpiryDate = if (expiryDate.isBlank() && predictedExpiryDate.isNotBlank()) {
                        predictedExpiryDate
                    } else {
                        expiryDateError = expiryDate.isBlank()
                        expiryDate
                    }

                    if (!nameError && !quantityError && !purchaseDateError && !expiryDateError) {
                        viewModel.createInventoryItem(
                            name = name.trim(),
                            category = category,
                            quantity = quantity.toDouble(),
                            purchaseDate = purchaseDate.trim(),
                            expiryDate = finalExpiryDate
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenDark),
                enabled = addItemState !is AddItemUiState.Loading
            ) {
                if (addItemState is AddItemUiState.Loading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Guardar alimento",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
