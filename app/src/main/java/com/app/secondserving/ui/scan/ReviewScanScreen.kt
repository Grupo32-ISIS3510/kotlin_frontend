package com.app.secondserving.ui.scan

import android.app.DatePickerDialog
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.secondserving.data.BackNavigationVerifier
import com.app.secondserving.data.ProductRegistryManager
import com.app.secondserving.data.ShelfLifePredictor
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

private val GreenDark = Color(0xFF386641)
private val BackgroundColor = Color(0xFFF5F5F0)
private val CATEGORIES = listOf("Frutas", "Verduras", "Lácteos", "Carnes", "Granos", "Bebidas", "Enlatados", "Otros")
private const val MAX_NAME_LENGTH = 35

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScanScreen(
    viewModel: ScanViewModel,
    onConfirm: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val reviewState by viewModel.reviewState.collectAsStateWithLifecycle()
    val items = reviewState.items
    val scope = rememberCoroutineScope()

    LaunchedEffect(reviewState.saveSuccess) {
        if (reviewState.saveSuccess) {
            ProductRegistryManager.markSuccess()
            onConfirm()
            viewModel.resetReviewState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Revisar compra", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = GreenDark)
                },
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch { BackNavigationVerifier.trackBackFromReviewScan() }
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = GreenDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundColor)
            )
        },
        containerColor = BackgroundColor
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    "Ajusta los detalles finales de tu compra.",
                    color = GreenDark,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(items) { index, item ->
                        ReviewItemCard(
                            item = item,
                            onUpdate = { updatedItem ->
                                viewModel.updateItem(index, updatedItem)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.saveScannedItems() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenDark),
                    enabled = items.isNotEmpty() && !reviewState.isSaving
                ) {
                    if (reviewState.isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Confirmar y Guardar (${items.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            
            reviewState.saveError?.let { error ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.resetReviewState() }) {
                            Text("OK", color = Color.White)
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewItemCard(
    item: EditableScannedItem,
    onUpdate: (EditableScannedItem) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = GreenDark,
        unfocusedBorderColor = GreenDark.copy(alpha = 0.5f),
        focusedLabelColor = GreenDark,
        unfocusedLabelColor = GreenDark,
        focusedTextColor = GreenDark,
        unfocusedTextColor = GreenDark,
        focusedTrailingIconColor = GreenDark,
        unfocusedTrailingIconColor = GreenDark,
        errorBorderColor = Color(0xFFC62828),
        errorLabelColor = Color(0xFFC62828)
    )

    // Función para mostrar el selector de fecha nativo
    val showDatePicker = {
        val currentDate = try {
            LocalDate.parse(item.expiryDate)
        } catch (e: Exception) {
            LocalDate.now()
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                onUpdate(item.copy(expiryDate = date))
            },
            currentDate.year,
            currentDate.monthValue - 1,
            currentDate.dayOfMonth
        ).show()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isError = item.name.length > MAX_NAME_LENGTH
                OutlinedTextField(
                    value = item.name,
                    onValueChange = { 
                        if (it.length <= MAX_NAME_LENGTH + 5) {
                            onUpdate(item.copy(name = it))
                        }
                    },
                    label = { Text("Nombre", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    singleLine = true,
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text(
                                "Máx. $MAX_NAME_LENGTH caracteres",
                                color = Color(0xFFC62828),
                                fontSize = 10.sp
                            )
                        } else {
                            Text(
                                "${item.name.length}/$MAX_NAME_LENGTH",
                                color = GreenDark.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }
                    }
                )
            }

            Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.weight(0.5f)
                ) {
                    OutlinedTextField(
                        value = item.category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría", fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor(),
                        colors = textFieldColors,
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        CATEGORIES.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category, color = GreenDark) },
                                onClick = {
                                    val newExpiry = ShelfLifePredictor.predictExpiryDate(item.purchaseDate, category)
                                    onUpdate(item.copy(category = category, expiryDate = newExpiry))
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.weight(0.5f).height(56.dp).background(BackgroundColor, RoundedCornerShape(8.dp)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    IconButton(onClick = { if (item.quantity > 1) onUpdate(item.copy(quantity = item.quantity - 1)) }) {
                        Icon(Icons.Default.Remove, contentDescription = "Menos", tint = GreenDark)
                    }
                    Text(text = item.quantity.toString(), fontWeight = FontWeight.Bold, color = GreenDark)
                    IconButton(onClick = { onUpdate(item.copy(quantity = item.quantity + 1)) }) {
                        Icon(Icons.Default.Add, contentDescription = "Más", tint = GreenDark)
                    }
                }
            }

            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clickable { showDatePicker() }
            ) {
                OutlinedTextField(
                    value = item.expiryDate,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false, // Deshabilitamos para que el click lo maneje el Box
                    label = { Text("Vencimiento", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker() }) {
                            Icon(
                                Icons.Default.CalendarToday, 
                                contentDescription = "Seleccionar fecha",
                                tint = GreenDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = GreenDark.copy(alpha = 0.5f),
                        disabledLabelColor = GreenDark,
                        disabledTextColor = GreenDark,
                        disabledTrailingIconColor = GreenDark
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                )
            }
        }
    }
}
