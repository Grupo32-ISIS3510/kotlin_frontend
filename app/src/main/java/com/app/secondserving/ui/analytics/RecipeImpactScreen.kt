package com.app.secondserving.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import java.text.NumberFormat
import java.util.Locale

private val GreenDark = Color(0xFF386641)
private val GreenLight = Color(0xFFE8F5E9)
private val Beige = Color(0xFFF2E8CF)
private val Orange = Color(0xFFEB5E28)
private val BackgroundColor = Color(0xFFF7F7F2)
private val CardColor = Color.White
private val TextSecondary = Color(0xFF6B7280)

/**
 * Pantalla de la BQ T3.2 (impacto de recetas en reducción de waste por
 * categoría). Consume /analytics/waste?months=3 y dibuja un box plot por
 * categoría usando las observaciones mensuales de value_lost_cop.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeImpactScreen(
    viewModel: RecipeImpactViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Impacto en tu despensa",
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
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (val s = uiState) {
                is RecipeImpactUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = GreenDark
                )
                is RecipeImpactUiState.Error -> ErrorContent(
                    message = s.message,
                    onRetry = { viewModel.loadImpact() }
                )
                is RecipeImpactUiState.Success -> ImpactContent(s.byCategory)
            }
        }
    }
}

@Composable
private fun ImpactContent(byCategory: Map<String, List<Double>>) {
    if (byCategory.isEmpty()) {
        EmptyContent()
        return
    }

    // Calculamos stats por categoría y descartamos las que no tienen datos.
    val rows = byCategory
        .mapNotNull { (cat, values) ->
            BoxPlotStats.from(values)?.let { cat to it }
        }
        .sortedByDescending { it.second.median }

    val axisMax = rows.maxOf { it.second.max }.coerceAtLeast(1.0)
    val totalObservations = rows.sumOf { it.second.count }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HeaderCard(totalObservations = totalObservations)
        BoxPlotsCard(rows = rows, axisMax = axisMax)
        AxisLegendCard(axisMax = axisMax)
        FormulaCard()
    }
}

@Composable
private fun HeaderCard(totalObservations: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GreenLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(text = "📊", fontSize = 28.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Distribución del waste por categoría",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GreenDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Cada caja muestra la dispersión del valor perdido por categoría en los últimos 3 meses. " +
                    "Las cajas más anchas indican meses muy variables; las más altas en el eje horizontal, " +
                    "categorías donde se pierde más plata.",
                fontSize = 13.sp,
                color = Color(0xFF1A1A1A),
                lineHeight = 18.sp
            )
            if (totalObservations > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Basado en $totalObservations observaciones mensuales del backend.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun BoxPlotsCard(
    rows: List<Pair<String, BoxPlotStats>>,
    axisMax: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            rows.forEachIndexed { index, (cat, stats) ->
                val (boxColor, accent) = colorFor(stats.median, axisMax)
                BoxPlotRow(
                    label = cat,
                    stats = stats,
                    axisMax = axisMax,
                    boxColor = boxColor,
                    accentColor = accent
                )
                if (index < rows.lastIndex) {
                    HorizontalDivider(color = Color(0xFFF1F1EE), thickness = 1.dp)
                }
            }
        }
    }
}

/** Verde si la mediana está abajo del 33% del rango, naranja en medio, rojo arriba. */
private fun colorFor(median: Double, axisMax: Double): Pair<Color, Color> {
    val ratio = if (axisMax <= 0.0) 0.0 else median / axisMax
    return when {
        ratio < 0.33 -> Color(0xFFD7EBC8) to GreenDark
        ratio < 0.66 -> Color(0xFFFFE5B4) to Color(0xFFD97706)
        else -> Color(0xFFFFD2D2) to Color(0xFFB91C1C)
    }
}

@Composable
private fun AxisLegendCard(axisMax: Double) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Eje horizontal",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0", fontSize = 12.sp, color = TextSecondary)
                Text(
                    formatter.format(axisMax / 2),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    formatter.format(axisMax),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun FormulaCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Beige.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                text = "• La caja va desde el primer cuartil hasta el tercero (50% central de los meses).\n" +
                    "• La línea vertical dentro de la caja es la mediana.\n" +
                    "• Los “bigotes” se extienden hasta el mes mínimo y máximo.\n" +
                    "• Una caja angosta con bigotes cortos = waste estable; una caja ancha = mucha variabilidad.",
                fontSize = 12.sp,
                color = Color(0xFF1A1A1A),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun EmptyContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🌱", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Aún no hay datos suficientes",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Marca alimentos como consumidos o descartados para que el backend empiece a calcular tu impacto.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("No pudimos cargar el impacto", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = GreenDark)
        ) { Text("Reintentar") }
    }
}
