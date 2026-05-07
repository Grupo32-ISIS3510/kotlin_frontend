package com.app.secondserving.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.secondserving.ui.components.OfflineBanner
import com.app.secondserving.ui.inventory.InventoryItemUi
import com.app.secondserving.ui.inventory.InventoryUiState
import com.app.secondserving.ui.inventory.InventoryViewModel
import com.app.secondserving.ui.inventory.Urgency
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// Paleta oficial de la identidad de la app
private val GreenDark = Color(0xFF386641)
private val GreenMedium = Color(0xFF6A994E)
private val GreenLight = Color(0xFFA7C957)
private val Beige = Color(0xFFF2E8CF)
private val Orange = Color(0xFFEB5E28)

private val UrgencyRed = Color(0xFFE53935)
private val UrgencyYellow = Color(0xFFFB8C00)
private val UrgencyGreen = GreenMedium

private val BackgroundColor = Color(0xFFF7F7F2)
private val CardColor = Color.White
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF6B7280)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    inventoryViewModel: InventoryViewModel,
    userName: String,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSegment: () -> Unit = {},
    onNavigateToImpact: () -> Unit = {},
    isOnline: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsState()
    val inventoryState by inventoryViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSavings()
        inventoryViewModel.loadInventory(showLoading = false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        OfflineBanner(isOnline = isOnline)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            GreetingHeader(userName = userName, onNavigateToProfile = onNavigateToProfile)

        SavingsCard(state = uiState, onRetry = { viewModel.loadSavings() })

        UserSegmentTeaserCard(onClick = onNavigateToSegment)

        RecipeImpactTeaserCard(onClick = onNavigateToImpact)

            ComerPrimeroSection(
                state = inventoryState,
                onRetry = { inventoryViewModel.loadInventory() }
            )

            PlanDeHoySection()
        }
    }
}

// Card teaser que abre la pantalla de impacto de waste por categoría
// (BQ T3.2). La pantalla destino hace su propio fetch.
@Composable
private fun RecipeImpactTeaserCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFFE0B2),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "📈", fontSize = 22.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Impacto en tu despensa",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Distribución del waste por categoría",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Text(
                text = "›",
                fontSize = 22.sp,
                color = GreenDark,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Card teaser que abre la pantalla completa de UserSegment (BQ T4.1).
// No hace fetch propio: la pantalla destino lo hace al montarse.
@Composable
private fun UserSegmentTeaserCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Beige,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "📊", fontSize = 22.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tu perfil de impacto",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Ver tu segmento como usuario",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Text(
                text = "›",
                fontSize = 22.sp,
                color = GreenDark,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GreetingHeader(userName: String, onNavigateToProfile: () -> Unit) {
    val today = LocalDate.now()
    val locale = Locale("es", "CO")
    val dayName = today.dayOfWeek
        .getDisplayName(TextStyle.FULL, locale)
        .replaceFirstChar { it.uppercase(locale) }
    val monthName = today.month
        .getDisplayName(TextStyle.FULL, locale)
        .lowercase(locale)
    val formattedDate = "$dayName, ${today.dayOfMonth} de $monthName"
    val firstName = userName.trim().split(" ").firstOrNull()?.ifBlank { "Usuario" } ?: "Usuario"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Hola, $firstName \uD83D\uDC4B",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formattedDate,
                fontSize = 14.sp,
                color = GreenMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Surface(
            onClick = onNavigateToProfile,
            shape = CircleShape,
            color = Beige,
            shadowElevation = 2.dp,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Ir al perfil",
                    tint = GreenDark
                )
            }
        }
    }
}

@Composable
private fun SavingsCard(state: HomeUiState, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        when (state) {
            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GreenDark, strokeWidth = 2.dp)
                }
            }
            is HomeUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No se pudo cargar el ahorro",
                        fontSize = 14.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.message,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenDark)
                    ) { Text("Reintentar") }
                }
            }
            is HomeUiState.Success -> {
                val analytics = state.analytics
                var showInfoDialog by remember { mutableStateOf(false) }

                if (showInfoDialog) {
                    AlertDialog(
                        onDismissRequest = { showInfoDialog = false },
                        title = {
                            Text(
                                text = "¿Qué significa este valor?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        },
                        text = {
                            Text(
                                text = "Es la suma del valor de los alimentos que consumiste " +
                                    "cuando les quedaban 3 días o menos para vencer.\n\n" +
                                    "No representa un ahorro en sentido estricto, sino el valor " +
                                    "de lo que aprovechaste antes de que se perdiera.",
                                fontSize = 14.sp,
                                color = TextPrimary,
                                lineHeight = 20.sp
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { showInfoDialog = false }) {
                                Text("Entendido", color = GreenDark, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        containerColor = CardColor,
                        shape = RoundedCornerShape(20.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SavingsColumn(
                        label = "AHORRADO ESTE MES",
                        value = formatCop(analytics.saved_cop),
                        modifier = Modifier.weight(1f),
                        onInfoClick = { showInfoDialog = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavingsColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueFontSize: androidx.compose.ui.unit.TextUnit = 20.sp,
    onInfoClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
            if (onInfoClick != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Más información",
                    tint = GreenMedium,
                    modifier = Modifier
                        .size(14.dp)
                        .clickable(onClick = onInfoClick)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = valueFontSize,
            fontWeight = FontWeight.ExtraBold,
            color = GreenDark
        )
    }
}

@Composable
private fun ComerPrimeroSection(
    state: InventoryUiState,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Comer Primero \uD83D\uDD25",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Ver todo",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = GreenMedium
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        when (state) {
            is InventoryUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GreenDark, strokeWidth = 2.dp)
                }
            }
            is InventoryUiState.Error -> {
                ErrorPlaceholder(
                    message = "No se pudo cargar tu inventario",
                    onRetry = onRetry
                )
            }
            is InventoryUiState.Success -> {
                val urgentItems = state.items
                    .filter { it.daysRemaining >= 0L }
                    .sortedBy { it.daysRemaining }
                    .take(8)

                if (urgentItems.isEmpty()) {
                    EmptyComerPrimero()
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(end = 4.dp)
                    ) {
                        items(urgentItems) { item ->
                            ComerPrimeroCard(item = item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComerPrimeroCard(item: InventoryItemUi) {
    val urgencyColor = when (item.urgency) {
        Urgency.RED -> UrgencyRed
        Urgency.YELLOW -> UrgencyYellow
        Urgency.GREEN -> UrgencyGreen
    }
    val daysLabel = when {
        item.daysRemaining == 0L -> "Hoy"
        item.daysRemaining == 1L -> "1 día"
        else -> "${item.daysRemaining} días"
    }
    val badgeBg = when (item.urgency) {
        Urgency.RED -> Color(0xFFFFE5E5)
        Urgency.YELLOW -> Color(0xFFFFF3CD)
        Urgency.GREEN -> Color(0xFFE8F5E9)
    }
    val progress = (maxOf(0L, item.daysRemaining) / 14f).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .width(150.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color(0xFFF3F4F6))
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.Center)
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = daysLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = urgencyColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = urgencyColor,
                    trackColor = Color(0xFFE5E7EB)
                )
            }
        }
    }
}

@Composable
private fun EmptyComerPrimero() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        shape = RoundedCornerShape(18.dp),
        color = CardColor,
        shadowElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "\uD83C\uDF31",
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "No hay alimentos urgentes por consumir",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ErrorPlaceholder(message: String, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CardColor,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = message, color = TextSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = GreenDark)
            ) { Text("Reintentar") }
        }
    }
}

@Composable
private fun PlanDeHoySection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Plan de hoy",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        WeekStrip()
        Spacer(modifier = Modifier.height(14.dp))
        RecipeSuggestionCard()
    }
}

@Composable
private fun WeekStrip() {
    val today = LocalDate.now()
    val locale = Locale("es", "CO")
    val monday = today.with(DayOfWeek.MONDAY)
    val days = (0..6).map { monday.plusDays(it.toLong()) }
    val dayFormatter = DateTimeFormatter.ofPattern("EEE", locale)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { date ->
            val isToday = date == today
            val letter = dayFormatter.format(date)
                .take(1)
                .uppercase(locale)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = letter,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = CircleShape,
                    color = if (isToday) GreenDark else Color.Transparent,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isToday) Color.White else TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeSuggestionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Beige,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = GreenDark
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Próximamente: sugerencias de recetas",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Basado en tu despensa",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Text(
                text = "›",
                fontSize = 22.sp,
                color = GreenDark,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatCop(value: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    formatter.maximumFractionDigits = 0
    formatter.minimumFractionDigits = 0
    return formatter.format(value)
}
