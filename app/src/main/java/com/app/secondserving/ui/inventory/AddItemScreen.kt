package com.app.secondserving.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GreenDark = Color(0xFF386641)
private val BackgroundColor = Color(0xFFF5F5F0)
private val CATEGORIES = listOf("Frutas", "Verduras", "Lácteos", "Carnes", "Otros")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit
) {
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
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
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

            // Fecha de compra
            OutlinedTextField(
                value = purchaseDate,
                onValueChange = { purchaseDate = it; purchaseDateError = false },
                label = { Text("Fecha de compra * (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                isError = purchaseDateError,
                supportingText = { if (purchaseDateError) Text("Formato: 2025-04-01") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenDark,
                    focusedLabelColor = GreenDark
                ),
                singleLine = true
            )

            // Fecha de vencimiento
            OutlinedTextField(
                value = expiryDate,
                onValueChange = { expiryDate = it; expiryDateError = false },
                label = { Text("Fecha de vencimiento * (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                isError = expiryDateError,
                supportingText = { if (expiryDateError) Text("Formato: 2025-04-01") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenDark,
                    focusedLabelColor = GreenDark
                ),
                singleLine = true
            )

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
                    expiryDateError = expiryDate.isBlank()

                    if (!nameError && !quantityError && !purchaseDateError && !expiryDateError) {
                        viewModel.createInventoryItem(
                            name = name.trim(),
                            category = category,
                            quantity = quantity.toDouble(),
                            purchaseDate = purchaseDate.trim(),
                            expiryDate = expiryDate.trim()
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