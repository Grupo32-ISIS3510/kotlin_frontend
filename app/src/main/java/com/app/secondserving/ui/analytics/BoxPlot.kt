package com.app.secondserving.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Box plot horizontal dibujado a mano con Canvas (curso 12.ProgrammingComponents).
 * No usamos librerías externas como MPAndroidChart porque no están en el
 * material del curso; las primitivas de Compose alcanzan para esta visual.
 *
 * Cada fila representa una categoría:
 *   - "Bigote" izquierdo desde min hasta q1 (línea fina).
 *   - "Caja" entre q1 y q3 (rectángulo relleno).
 *   - Línea vertical en la mediana.
 *   - "Bigote" derecho desde q3 hasta max.
 *
 * Si la categoría tiene 1 sola observación se dibuja como un punto (no hay
 * distribución que mostrar). Si tiene 2 observaciones, q1=min y q3=max y
 * la mediana es el promedio — sigue siendo un box plot válido aunque
 * degenerado.
 */
data class BoxPlotStats(
    val min: Double,
    val q1: Double,
    val median: Double,
    val q3: Double,
    val max: Double,
    val count: Int
) {
    companion object {
        fun from(values: List<Double>): BoxPlotStats? {
            if (values.isEmpty()) return null
            val sorted = values.sorted()
            val n = sorted.size
            val median = quantile(sorted, 0.5)
            val q1 = if (n >= 2) quantile(sorted, 0.25) else median
            val q3 = if (n >= 2) quantile(sorted, 0.75) else median
            return BoxPlotStats(
                min = sorted.first(),
                q1 = q1,
                median = median,
                q3 = q3,
                max = sorted.last(),
                count = n
            )
        }

        private fun quantile(sorted: List<Double>, p: Double): Double {
            if (sorted.isEmpty()) return 0.0
            if (sorted.size == 1) return sorted[0]
            val pos = p * (sorted.size - 1)
            val lo = pos.toInt()
            val hi = (lo + 1).coerceAtMost(sorted.size - 1)
            val frac = pos - lo
            return sorted[lo] + (sorted[hi] - sorted[lo]) * frac
        }
    }
}

@Composable
fun BoxPlotRow(
    label: String,
    stats: BoxPlotStats,
    axisMax: Double,
    boxColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Etiqueta a la izquierda
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1A1A1A),
            modifier = Modifier.width(96.dp)
        )

        // Canvas del box plot
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
        ) {
            val w = size.width
            val h = size.height
            val centerY = h / 2
            val safeMax = if (axisMax <= 0.0) 1.0 else axisMax

            fun xOf(v: Double) = ((v / safeMax).coerceIn(0.0, 1.0) * w).toFloat()

            val minX = xOf(stats.min)
            val q1X = xOf(stats.q1)
            val medianX = xOf(stats.median)
            val q3X = xOf(stats.q3)
            val maxX = xOf(stats.max)

            // Línea base sutil (eje)
            drawLine(
                color = Color(0xFFE5E7EB),
                start = Offset(0f, centerY),
                end = Offset(w, centerY),
                strokeWidth = 1f
            )

            if (stats.count == 1) {
                // Solo un dato: punto en el valor
                drawCircle(
                    color = accentColor,
                    radius = 6f,
                    center = Offset(medianX, centerY)
                )
                return@Canvas
            }

            // Bigote izquierdo (min → q1)
            drawLine(
                color = accentColor,
                start = Offset(minX, centerY),
                end = Offset(q1X, centerY),
                strokeWidth = 2f
            )
            // Bigote derecho (q3 → max)
            drawLine(
                color = accentColor,
                start = Offset(q3X, centerY),
                end = Offset(maxX, centerY),
                strokeWidth = 2f
            )
            // Tapas verticales en min y max
            val capHalf = h * 0.20f
            drawLine(
                color = accentColor,
                start = Offset(minX, centerY - capHalf),
                end = Offset(minX, centerY + capHalf),
                strokeWidth = 2f
            )
            drawLine(
                color = accentColor,
                start = Offset(maxX, centerY - capHalf),
                end = Offset(maxX, centerY + capHalf),
                strokeWidth = 2f
            )

            // Caja Q1..Q3
            val boxHeight = h * 0.55f
            val boxTop = centerY - boxHeight / 2
            val boxWidth = (q3X - q1X).coerceAtLeast(2f)
            drawRect(
                color = boxColor,
                topLeft = Offset(q1X, boxTop),
                size = Size(boxWidth, boxHeight)
            )
            // Borde de la caja
            drawRect(
                color = accentColor,
                topLeft = Offset(q1X, boxTop),
                size = Size(boxWidth, boxHeight),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
            // Línea de la mediana
            drawLine(
                color = accentColor,
                start = Offset(medianX, boxTop),
                end = Offset(medianX, boxTop + boxHeight),
                strokeWidth = 3f
            )
        }
    }
}
