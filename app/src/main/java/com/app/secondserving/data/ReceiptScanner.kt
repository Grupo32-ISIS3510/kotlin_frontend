package com.app.secondserving.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Scanner OCR para extraer información de facturas/tickets.
 * Feature (a): Sensor — Cámara para escanear alimentos.
 * 
 * Usa preprocesamiento de imagen para mejorar nitidez:
 * - Escala de grises
 * - Ecualización de histograma (contraste)
 * - Umbral binario adaptativo
 *
 * Extrae:
 * - Nombres de productos
 * - Fechas de compra
 * - Precios
 * - Categorías (basado en palabras clave)
 */
class ReceiptScanner(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Procesa una imagen (URI) y extrae texto usando OCR con preprocesamiento.
     */
    suspend fun scanReceipt(imageUri: Uri): ReceiptScanResult {
        return try {
            val bitmap = MediaUtils.bitmapFromUri(context, imageUri)
            val preprocessedBitmap = preprocessBitmap(bitmap)
            processBitmap(preprocessedBitmap)
        } catch (e: Exception) {
            Log.e("ReceiptScanner", "Error scanning receipt", e)
            ReceiptScanResult(error = "Error procesando la imagen: ${e.message}")
        }
    }

    /**
     * Procesa un Bitmap y extrae texto usando OCR con preprocesamiento.
     */
    suspend fun scanReceipt(bitmap: Bitmap): ReceiptScanResult {
        return try {
            val preprocessedBitmap = preprocessBitmap(bitmap)
            processBitmap(preprocessedBitmap)
        } catch (e: Exception) {
            Log.e("ReceiptScanner", "Error scanning receipt", e)
            ReceiptScanResult(error = "Error procesando la imagen: ${e.message}")
        }
    }

    /**
     * Preprocesa el bitmap para mejorar la calidad del OCR.
     * - Escala a resolución óptima
     * - Convierte a escala de grises
     * - Aumenta contraste con ecualización de histograma
     * - Aplica umbral binario para texto
     */
    private fun preprocessBitmap(bitmap: Bitmap): Bitmap {
        // 1. Escalar a resolución óptima para OCR (ancho máximo 1280px)
        val maxWidth = 1280
        val scale = if (bitmap.width > maxWidth) maxWidth.toFloat() / bitmap.width else 1f
        
        if (scale >= 1f && bitmap.config == Bitmap.Config.ARGB_8888) {
            // Ya está en buena resolución, aplicar solo filtros
            return applyImageEnhancement(bitmap)
        }
        
        val scaledWidth = (bitmap.width * scale).toInt()
        val scaledHeight = (bitmap.height * scale).toInt()
        
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        return applyImageEnhancement(scaledBitmap)
    }

    /**
     * Aplica mejoras a la imagen: escala de grises, contraste y umbral.
     */
    private fun applyImageEnhancement(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val enhancedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Calcular histograma para ecualización
        val histogram = IntArray(256)
        
        // 1. Convertir a escala de grises y calcular histograma
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            
            // Fórmula de luminosidad para escala de grises
            val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)
            histogram[gray]++
            
            pixels[i] = Color.rgb(gray, gray, gray)
        }
        
        // 2. Ecualización de histograma (mejora contraste)
        val cdf = IntArray(256)
        cdf[0] = histogram[0]
        for (i in 1 until 256) {
            cdf[i] = cdf[i - 1] + histogram[i]
        }
        
        val cdfMin = cdf.first { it > 0 }
        val totalPixels = width * height
        
        val lookupTable = IntArray(256)
        for (i in 0 until 256) {
            lookupTable[i] = ((cdf[i] - cdfMin) * 255 / (totalPixels - cdfMin)).coerceIn(0, 255)
        }
        
        // 3. Umbral binario suave para texto (mejora OCR)
        for (i in pixels.indices) {
            val gray = Color.red(pixels[i])
            val enhanced = lookupTable[gray]
            
            // Umbral: pixeles oscuros -> negro (texto), claros -> blanco (fondo)
            val threshold = if (enhanced < 180) 0 else 255
            pixels[i] = Color.rgb(threshold, threshold, threshold)
        }
        
        enhancedBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return enhancedBitmap
    }

    /**
     * Procesa el bitmap con ML Kit.
     */
    private suspend fun processBitmap(bitmap: Bitmap): ReceiptScanResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        val visionText: Text = recognizer.process(image).await()

        val fullText = visionText.text
        val lines = visionText.textBlocks.flatMap { it.lines }.map { it.text }

        return parseReceiptText(fullText, lines)
    }

    /**
     * Parsea el texto extraído para obtener información estructurada.
     */
    private fun parseReceiptText(fullText: String, lines: List<String>): ReceiptScanResult {
        val items = mutableListOf<ScannedItem>()
        var purchaseDate: String? = null
        var totalAmount: Double? = null

        // Patrones comunes en facturas
        val datePatterns = listOf(
            Regex("\\b(\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4})\\b"),
            Regex("\\b(\\d{4}[/\\-]\\d{1,2}[/\\-]\\d{1,2})\\b")
        )

        val pricePattern = Regex("\\$?\\s*(\\d+[.,]\\d{2})\\b")
        val itemPattern = Regex("^\\s*([A-Za-zÁÉÍÓÚáéíóúÑñ][A-Za-zÁÉÍÓÚáéíóúÑñ\\s]+)\\s+\\$?\\s*(\\d+[.,]\\d{2})", RegexOption.MULTILINE)

        // Buscar fecha
        for (line in lines) {
            for (pattern in datePatterns) {
                val match = pattern.find(line)
                if (match != null) {
                    purchaseDate = normalizeDate(match.groupValues[1])
                    break
                }
            }
            if (purchaseDate != null) break
        }

        // Buscar items (producto + precio)
        for (line in lines) {
            val itemMatch = itemPattern.find(line)
            if (itemMatch != null) {
                val name = itemMatch.groupValues[1].trim()
                val price = itemMatch.groupValues[2].replace(",", ".").toDoubleOrNull()

                if (name.isNotBlank() && name.length < 50 && price != null) {
                    items.add(
                        ScannedItem(
                            name = name,
                            price = price,
                            category = categorizeItem(name)
                        )
                    )
                }
            }
        }

        // Si no se encontraron items con patrón, intentar con líneas simples
        if (items.isEmpty()) {
            for (line in lines) {
                val trimmed = line.trim()
                // Ignorar líneas muy cortas o muy largas
                if (trimmed.length in 3..50 &&
                    !trimmed.contains(Regex("\\d{2}[/\\-]\\d{2}[/\\-]\\d{2}")) &&
                    !trimmed.contains(Regex("(TOTAL|SUBTOTAL|IMPUESTO|IVA|FACTURA)"))
                ) {
                    // Podría ser un nombre de producto
                    val priceMatch = pricePattern.find(trimmed)
                    if (priceMatch != null) {
                        val name = trimmed.replace(priceMatch.value, "").trim()
                        val price = priceMatch.groupValues[1].replace(",", ".").toDoubleOrNull()
                        if (name.isNotBlank() && price != null) {
                            items.add(
                                ScannedItem(
                                    name = name,
                                    price = price,
                                    category = categorizeItem(name)
                                )
                            )
                        }
                    } else if (trimmed.any { it.isLetter() }) {
                        // Línea que parece nombre de producto sin precio visible
                        items.add(
                            ScannedItem(
                                name = trimmed,
                                price = null,
                                category = categorizeItem(trimmed)
                            )
                        )
                    }
                }
            }
        }

        // Buscar total
        for (line in lines.reversed().take(10)) {
            if (line.contains("TOTAL", ignoreCase = true)) {
                val priceMatch = pricePattern.find(line)
                if (priceMatch != null) {
                    totalAmount = priceMatch.groupValues[1].replace(",", ".").toDoubleOrNull()
                    break
                }
            }
        }

        return ReceiptScanResult(
            items = items,
            purchaseDate = purchaseDate,
            totalAmount = totalAmount,
            fullText = fullText,
            rawLines = lines
        )
    }

    /**
     * Categoriza un item basado en su nombre.
     */
    private fun categorizeItem(name: String): String {
        val lowerName = name.lowercase()

        return when {
            lowerName.containsAny("leche", "yogurt", "queso", "mantequilla", "crema") -> "Lácteos"
            lowerName.containsAny("pollo", "res", "cerdo", "carne", "jamón", "salchicha") -> "Carnes"
            lowerName.containsAny("pescado", "atún", "salmón", "camarón", "marisco") -> "Pescados"
            lowerName.containsAny("manzana", "plátano", "naranja", "fruta", "fresa", "uva") -> "Frutas"
            lowerName.containsAny("lechuga", "tomate", "zanahoria", "verdura", "papa", "cebolla") -> "Verduras"
            lowerName.containsAny("arroz", "pasta", "pan", "galleta", "cereal", "trigo") -> "Granos"
            lowerName.containsAny("refresco", "jugo", "agua", "cerveza", "bebida") -> "Bebidas"
            lowerName.containsAny("salsa", "sal", "pimienta", "especia", "condimento") -> "Condimentos"
            lowerName.containsAny("lata", "envase", "conserva") -> "Enlatados"
            else -> "Otros"
        }
    }

    /**
     * Normaliza una fecha a formato yyyy-MM-dd.
     */
    private fun normalizeDate(dateStr: String): String {
        return try {
            // Intentar diferentes formatos
            val formats = listOf(
                "dd/MM/yyyy",
                "dd-MM-yyyy",
                "MM/dd/yyyy",
                "yyyy/MM/dd",
                "dd/MM/yy",
                "dd-MM-yy"
            )

            for (format in formats) {
                try {
                    val formatter = DateTimeFormatter.ofPattern(format)
                    val date = LocalDate.parse(dateStr, formatter)
                    return date.format(dateTimeFormatter)
                } catch (e: Exception) {
                    continue
                }
            }

            // Si ningún formato funciona, devolver la fecha original
            dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    /**
     * Libera recursos.
     */
    fun close() {
        recognizer.close()
    }
}

/**
 * Resultado del escaneo de una factura.
 */
data class ReceiptScanResult(
    val items: List<ScannedItem> = emptyList(),
    val purchaseDate: String? = null,
    val totalAmount: Double? = null,
    val fullText: String = "",
    val rawLines: List<String> = emptyList(),
    val error: String? = null
)

/**
 * Item escaneado de una factura.
 */
data class ScannedItem(
    val name: String,
    val price: Double?,
    val category: String
)

/**
 * Helper para verificar si un string contiene alguna de las palabras.
 */
private fun String.containsAny(vararg words: String): Boolean {
    return words.any { this.contains(it, ignoreCase = true) }
}
