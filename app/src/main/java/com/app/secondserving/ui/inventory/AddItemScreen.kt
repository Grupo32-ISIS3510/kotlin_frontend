package com.app.secondserving.ui.inventory

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.app.secondserving.data.ReceiptScanner
import com.app.secondserving.data.ShelfLifePredictor
import kotlinx.coroutines.launch
import java.io.File
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
    onOpenScanner: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val addItemState by viewModel.addItemState.collectAsState()

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Frutas") }
    var quantity by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf(false) }
    var quantityError by remember { mutableStateOf(false) }
    var purchaseDateError by remember { mutableStateOf(false) }
    var expiryDateError by remember { mutableStateOf(false) }

    // Predicción automática de fecha de expiración
    var showPredictionTip by remember { mutableStateOf(false) }
    var predictedExpiryDate by remember { mutableStateOf("") }
    var storageTip by remember { mutableStateOf("") }

    // Selectores de fecha
    var showPurchaseDatePicker by remember { mutableStateOf(false) }
    var showExpiryDatePicker by remember { mutableStateOf(false) }

    // Estado para escáner OCR
    var isScanning by remember { mutableStateOf(false) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    val scanner = remember { ReceiptScanner(context) }

    var hasCameraPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    // Launcher para cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && capturedImageUri != null) {
            isScanning = true
            scope.launch {
                try {
                    val result = scanner.scanReceipt(capturedImageUri!!)
                    if (result.error != null) {
                        Toast.makeText(context, result.error, Toast.LENGTH_LONG).show()
                    } else if (result.items.isNotEmpty()) {
                        // Llenar campos con el primer producto detectado
                        val firstItem = result.items.first()
                        name = firstItem.name
                        category = firstItem.category.ifBlank { "Otros" }
                        quantity = "1"
                        purchaseDate = result.purchaseDate ?: LocalDate.now().toString()
                        predictedExpiryDate = ShelfLifePredictor.predictExpiryDate(
                            purchaseDateStr = purchaseDate,
                            category = category
                        )
                        storageTip = ShelfLifePredictor.getStorageRecommendation(category)
                        showPredictionTip = true
                        expiryDate = predictedExpiryDate
                    } else {
                        Toast.makeText(context, "No se detectaron productos en la imagen", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error al procesar: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isScanning = false
                }
            }
        }
    }

    // Launcher para galería
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isScanning = true
            scope.launch {
                try {
                    val result = scanner.scanReceipt(it)
                    if (result.error != null) {
                        Toast.makeText(context, result.error, Toast.LENGTH_LONG).show()
                    } else if (result.items.isNotEmpty()) {
                        val firstItem = result.items.first()
                        name = firstItem.name
                        category = firstItem.category.ifBlank { "Otros" }
                        quantity = "1"
                        purchaseDate = result.purchaseDate ?: LocalDate.now().toString()
                        predictedExpiryDate = ShelfLifePredictor.predictExpiryDate(
                            purchaseDateStr = purchaseDate,
                            category = category
                        )
                        storageTip = ShelfLifePredictor.getStorageRecommendation(category)
                        showPredictionTip = true
                        expiryDate = predictedExpiryDate
                    } else {
                        Toast.makeText(context, "No se detectaron productos en la imagen", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error al procesar: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isScanning = false
                }
            }
        }
    }

    // Launcher para permiso de cámara
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            // Abrir cámara directamente
            val photoFile = File(context.cacheDir, "receipt_capture.jpg")
            val photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            capturedImageUri = photoUri
            cameraLauncher.launch(photoUri)
        }
    }

    // Actualizar predicción cuando cambia categoría o fecha de compra
    LaunchedEffect(category, purchaseDate) {
        if (purchaseDate.isNotBlank() && expiryDate.isBlank() && !isScanning) {
            predictedExpiryDate = ShelfLifePredictor.predictExpiryDate(
                purchaseDateStr = purchaseDate,
                category = category
            )
            storageTip = ShelfLifePredictor.getStorageRecommendation(category)
            showPredictionTip = true
        }
    }

    // Navegar atrás al éxito
    LaunchedEffect(addItemState) {
        if (addItemState is AddItemUiState.Success) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Agregar alimento",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = GreenDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver a la despensa",
                            tint = GreenDark
                        )
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

            // Botón de escanear con cámara
            Card(
                onClick = onOpenScanner,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GreenDark),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Escanear con cámara",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Detecta productos automáticamente",
                            fontSize = 13.sp,
                            color = Color(0xFFE8F5E9)
                        )
                    }
                }
            }

            // Divider
            HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)

            // Título del formulario
            Text(
                text = "Datos del producto",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF333333)
            )

            // Nombre
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text("Nombre *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                isError = nameError,
                supportingText = { if (nameError) Text("Campo requerido") },
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
                supportingText = { if (quantityError) Text("Ingresa un número válido") },
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
                supportingText = { if (purchaseDateError) Text("Campo requerido") },
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
                var selectedYear by remember { mutableStateOf(LocalDate.now().year) }
                var selectedMonth by remember { mutableStateOf(LocalDate.now().monthValue - 1) }
                var selectedDay by remember { mutableStateOf(LocalDate.now().dayOfMonth) }
                
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
                supportingText = { if (expiryDateError) Text("Campo requerido") },
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
                var expYear by remember { mutableStateOf(LocalDate.now().year) }
                var expMonth by remember { mutableStateOf(LocalDate.now().monthValue - 1) }
                var expDay by remember { mutableStateOf(LocalDate.now().dayOfMonth) }
                
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
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
