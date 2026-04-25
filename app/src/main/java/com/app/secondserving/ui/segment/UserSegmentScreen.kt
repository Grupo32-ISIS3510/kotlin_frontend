package com.app.secondserving.ui.segment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.app.secondserving.data.network.UserSegmentResponse

private val GreenDark = Color(0xFF386641)
private val GreenMedium = Color(0xFF6A994E)
private val Beige = Color(0xFFF2E8CF)
private val Orange = Color(0xFFEB5E28)
private val BackgroundColor = Color(0xFFF7F7F2)
private val CardColor = Color.White
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF6B7280)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSegmentScreen(
    viewModel: UserSegmentViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tu perfil de impacto",
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
            when (val state = uiState) {
                is UserSegmentUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = GreenDark
                    )
                }
                is UserSegmentUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.loadSegment() }
                    )
                }
                is UserSegmentUiState.Success -> {
                    SegmentContent(data = state.data)
                }
            }
        }
    }
}

@Composable
private fun SegmentContent(data: UserSegmentResponse) {
    val info = segmentInfo(data.segment)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SegmentHeroCard(info = info)

        StatsRow(
            recipesCooked = data.recipes_cooked_last_30_days,
            openRate = data.open_rate
        )

        ClusterDiagram(currentSegment = data.segment)

        FormulaCard()
    }
}

@Composable
private fun SegmentHeroCard(info: SegmentVisual) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = info.bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = info.emoji, fontSize = 48.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Eres un usuario",
                fontSize = 14.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = info.label,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = info.accent
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = info.description,
                fontSize = 14.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun StatsRow(recipesCooked: Int, openRate: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            value = recipesCooked.toString(),
            label = "Recetas cocinadas\nen 30 días",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = "${(openRate * 100).toInt()}%",
            label = "Open rate de\nnotificaciones",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GreenDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun ClusterDiagram(currentSegment: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Segmentación de usuarios",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tu cluster está resaltado.",
                fontSize = 12.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClusterNode("passive", currentSegment)
                ClusterNode("neutral", currentSegment)
                ClusterNode("proactive", currentSegment)
            }
        }
    }
}

@Composable
private fun ClusterNode(segment: String, currentSegment: String) {
    val info = segmentInfo(segment)
    val isCurrent = segment.equals(currentSegment, ignoreCase = true)
    val size = if (isCurrent) 96.dp else 72.dp
    val borderWidth = if (isCurrent) 3.dp else 0.dp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier
                .size(size)
                .border(
                    width = borderWidth,
                    color = if (isCurrent) info.accent else Color.Transparent,
                    shape = CircleShape
                ),
            shape = CircleShape,
            color = if (isCurrent) info.bgColor else info.bgColor.copy(alpha = 0.4f),
            shadowElevation = if (isCurrent) 4.dp else 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = info.emoji,
                    fontSize = if (isCurrent) 32.sp else 24.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = info.label,
            fontSize = if (isCurrent) 14.sp else 12.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
            color = if (isCurrent) info.accent else TextSecondary
        )
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
                text = "¿Cómo se calcula?",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = GreenDark
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "El backend lee tus interacciones de los últimos 30 días:\n" +
                    "• Proactive: open_rate ≥ 60% y ≥ 3 recetas cocinadas.\n" +
                    "• Passive: open_rate < 20% y 0 recetas cocinadas.\n" +
                    "• Neutral: cualquier otro caso.",
                fontSize = 12.sp,
                color = TextPrimary,
                lineHeight = 18.sp
            )
        }
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
        Text(
            text = "No pudimos cargar tu perfil",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = GreenDark)
        ) { Text("Reintentar") }
    }
}

private data class SegmentVisual(
    val label: String,
    val emoji: String,
    val description: String,
    val accent: Color,
    val bgColor: Color
)

private fun segmentInfo(segment: String): SegmentVisual = when (segment.lowercase()) {
    "proactive" -> SegmentVisual(
        label = "Proactive",
        emoji = "🔥",
        description = "Abres las alertas con frecuencia y cocinas seguido lo que tienes en casa. Vas adelante de la app.",
        accent = GreenDark,
        bgColor = Color(0xFFE8F5E9)
    )
    "passive" -> SegmentVisual(
        label = "Passive",
        emoji = "💤",
        description = "Casi no respondes a las alertas y no estás cocinando lo que tienes. Hay margen para aprovechar más tu despensa.",
        accent = Orange,
        bgColor = Color(0xFFFDECEA)
    )
    else -> SegmentVisual(
        label = "Neutral",
        emoji = "👍",
        description = "Estás interactuando con la app de forma intermedia. Si abres más las alertas y cocinas un par de recetas más, pasas a Proactive.",
        accent = GreenMedium,
        bgColor = Color(0xFFFFF8E1)
    )
}
