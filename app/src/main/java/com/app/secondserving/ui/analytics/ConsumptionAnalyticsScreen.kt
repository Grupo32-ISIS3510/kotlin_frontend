package com.app.secondserving.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.secondserving.data.ConsumptionCache

private val GreenDark       = Color(0xFF386641)
private val GreenLight      = Color(0xFFE8F5E9)
private val Beige           = Color(0xFFF2E8CF)
private val BackgroundColor = Color(0xFFF7F7F2)
private val CardColor       = Color.White
private val TextSecondary   = Color(0xFF6B7280)

/**
 * Pantalla BQ T4.2 — frecuencia de consumo y recompra por categoría.
 * Lee los contadores locales de ConsumptionCache (acumulados mientras
 * el usuario usa la app) y los muestra como ranking de barras.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumptionAnalyticsScreen(
    viewModel: ConsumptionAnalyticsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Frecuencia de consumo",
                        fontWeight = FontWeight.Bold,
                        color = GreenDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = GreenDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundColor)
            )
        },
        containerColor = BackgroundColor
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = uiState) {
                is ConsumptionUiState.Empty   -> EmptyConsumption()
                is ConsumptionUiState.Success -> ConsumptionContent(s.stats)
            }
        }
    }
}

@Composable
private fun ConsumptionContent(stats: List<ConsumptionCache.CategoryStats>) {
    val maxConsumed = stats.maxOfOrNull { it.consumedCount }?.coerceAtLeast(1) ?: 1

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = GreenLight),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "🛒", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "¿Qué consumes más?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ranking de categorías según cuántas veces marcaste alimentos como consumidos o descartados. " +
                               "Las barras muestran la frecuencia relativa.",
                        fontSize = 13.sp,
                        color = Color(0xFF1A1A1A),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardColor),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    stats.forEachIndexed { index, stat ->
                        CategoryRow(stat = stat, maxConsumed = maxConsumed, rank = index + 1)
                        if (index < stats.lastIndex) {
                            HorizontalDivider(
                                color = Color(0xFFF1F1EE),
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Beige.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "¿Cómo leerlo?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Consumidos: veces que marcaste un alimento de esta categoría como consumido o descartado.\n" +
                               "• Agregados: veces que registraste un alimento de esta categoría en tu despensa.\n" +
                               "• Una categoría con muchos consumos y pocos agregados puede indicar recompra frecuente.",
                        fontSize = 12.sp,
                        color = Color(0xFF1A1A1A),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    stat: ConsumptionCache.CategoryStats,
    maxConsumed: Int,
    rank: Int
) {
    val barFraction = stat.consumedCount.toFloat() / maxConsumed.toFloat()
    val barColor = when (rank) {
        1    -> Color(0xFF386641)
        2    -> Color(0xFF6A994E)
        3    -> Color(0xFFA7C957)
        else -> Color(0xFFCBD5E1)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#$rank",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.width(28.dp)
            )
            Text(
                text = stat.category.replaceFirstChar { it.uppercase() },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A1A),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${stat.consumedCount} consumidos · ${stat.addedCount} agregados",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color(0xFFF1F1EE), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(barFraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(barColor, RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun EmptyConsumption() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "📊", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Aún no hay datos",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Empieza a registrar y consumir alimentos para ver qué categorías usas más seguido.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}
