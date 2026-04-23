package com.app.secondserving.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

private val GreenDark = Color(0xFF386641)
private val BackgroundColor = Color(0xFFF5F5F0)
private val UrgencyRed = Color(0xFFE53935)
private val UrgencyYellow = Color(0xFFFB8C00)
private val UrgencyGreen = Color(0xFF6A994E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    item: InventoryItemUi,
    storageTip: String,
    weatherState: WeatherUiState,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
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
                    .height(180.dp)
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
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = urgencyColor
                        ) {
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
                Column(modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

                    // Contexto del clima
                    if (weatherState is WeatherUiState.Success) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Divider(color = Color(0xFFB2DFDB))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "📍 ${weatherState.weather.city}  🌡️ ${weatherState.weather.temperature.toInt()}°C  💧 ${weatherState.weather.humidity}% humedad",
                            fontSize = 12.sp,
                            color = Color(0xFF555555)
                        )
                    }
                }
            }
        }
    }
}