package com.app.secondserving.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.secondserving.data.network.DiscardReasons
import kotlin.math.abs

private val GreenDark = Color(0xFF386641)
private val BackgroundColor = Color(0xFFF5F5F0)
private val UrgencyRed = Color(0xFFE53935)
private val UrgencyYellow = Color(0xFFFB8C00)
private val UrgencyGreen = Color(0xFF6A994E)

/**
 * Pantalla de detalle de un item de inventario.
 *
 * Permite al usuario marcar el item como **consumido** (lo aprovechó) o
 * **descartado** (se perdió, con razón). El backend recibe llamadas distintas
 * para cada caso y eso alimenta la BQ T3.2 (impacto en reducción de waste).
 *
 * Recibe callbacks en lugar del ViewModel para no acoplar la pantalla al
 * VM concreto (sigue MVVM clásico: la View no decide la lógica, solo la
 * dispara hacia arriba).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    item: InventoryItemUi,
    storageTip: String,
    weatherState: WeatherUiState,
    actionState: ItemActionState = ItemActionState.Idle,
    onConsume: (itemId: String) -> Unit = {},
    onDiscard: (itemId: String, reason: String) -> Unit = { _, _ -> },
    onActionConsumed: () -> Unit = {},
    onNavigateBack: () -> Unit
) {
    val urgencyColor = when (item.urgency) {
        Urgency.RED -> UrgencyRed
        Urgency.YELLOW -> UrgencyYellow
        Urgency.GREEN -> UrgencyGreen
    }
    val daysLabel = when {
        item.daysRemaining < 0L -> "Vencido hace ${abs(item.daysRemaining)} días"
        item.daysRemaining == 0L -> "Vence hoy"
        item.daysRemaining == 1L -> "Vence mañana"
        else -> "Vence en ${item.daysRemaining} días"
    }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Reaccionamos al estado de la acción: éxito cierra la pantalla y avisa
    // al caller para que recargue inventario; error muestra alerta.
    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is ItemActionState.Success -> {
                onActionConsumed()
                onNavigateBack()
            }
            is ItemActionState.Error -> {
                errorMessage = s.message
                onActionConsumed()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundColor)
            )
        },
        containerColor = BackgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Imagen placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color(0xFFF0F0F0), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = Color(0xFFCCCCCC),
                    modifier = Modifier.size(64.dp)
                )
            }

            // Info básica
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Categoría", color = Color.Gray, fontSize = 14.sp)
                        Text(item.category, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cantidad", color = Color.Gray, fontSize = 14.sp)
                        Text(
                            "${item.quantity} ${if (item.quantity == 1) "unidad" else "unidades"}",
                            fontWeight = FontWeight.Medium, fontSize = 14.sp
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Estado", color = Color.Gray, fontSize = 14.sp)
                        Surface(shape = RoundedCornerShape(8.dp), color = urgencyColor) {
                            Text(
                                daysLabel,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Recomendación de almacenamiento
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Recomendación de almacenamiento",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GreenDark
                    )
                    Text(
                        storageTip,
                        fontSize = 15.sp,
                        color = Color(0xFF2E2E2E),
                        lineHeight = 22.sp
                    )

                    if (weatherState is WeatherUiState.Success) {
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = Color(0xFFB2DFDB))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "📍 ${weatherState.weather.city}  🌡️ ${weatherState.weather.temperature.toInt()}°C  💧 ${weatherState.weather.humidity}% humedad",
                            fontSize = 12.sp,
                            color = Color(0xFF555555)
                        )
                    }
                }
            }

            ActionsCard(
                isLoading = actionState is ItemActionState.Loading,
                onConsume = { onConsume(item.id) },
                onDiscardClick = { showDiscardDialog = true }
            )
        }
    }

    if (showDiscardDialog) {
        DiscardReasonDialog(
            onDismiss = { showDiscardDialog = false },
            onConfirm = { reason ->
                showDiscardDialog = false
                onDiscard(item.id, reason)
            }
        )
    }

    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("No se pudo completar", fontWeight = FontWeight.Bold) },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("Entendido", color = GreenDark)
                }
            }
        )
    }
}

@Composable
private fun ActionsCard(
    isLoading: Boolean,
    onConsume: () -> Unit,
    onDiscardClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "¿Qué pasó con este alimento?",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = GreenDark
            )
            Text(
                "Decirnos si lo consumiste o si se perdió nos ayuda a calcular tu impacto real.",
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 16.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onConsume,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenDark,
                        contentColor = Color.White
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.RestaurantMenu,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lo consumí", fontSize = 13.sp)
                    }
                }
                OutlinedButton(
                    onClick = onDiscardClick,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = UrgencyRed)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Se dañó", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun DiscardReasonDialog(
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var selectedReason by remember { mutableStateOf(DiscardReasons.EXPIRED) }

    val options = listOf(
        DiscardReasons.EXPIRED to "Se venció",
        DiscardReasons.BAD_STORAGE to "Mal almacenamiento",
        DiscardReasons.OVER_PURCHASE to "Compré demasiado",
        DiscardReasons.OTHER to "Otra razón"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Por qué se perdió?", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Esto nos ayuda a entender el desperdicio. Tus respuestas alimentan las analíticas de la app.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
                options.forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == key,
                            onClick = { selectedReason = key },
                            colors = RadioButtonDefaults.colors(selectedColor = GreenDark)
                        )
                        Text(label, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedReason) },
                colors = ButtonDefaults.buttonColors(containerColor = GreenDark)
            ) { Text("Confirmar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) }
        }
    )
}
