package com.app.secondserving.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Scanner híbrido OCR que soporta modo offline (ML Kit) y online (Cloud Vision API).
 *
 * - Offline: Rápido, gratis, sin internet (precisión ~80-85%)
 * - Online: Más lento, requiere API Key, mejor precisión (~95%)
 */
class ReceiptScannerHybrid(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // API Key de Google Cloud Vision (en producción usar desde backend seguro)
    private val cloudVisionApiKey = BuildConfig.GOOGLE_CLOUD_VISION_API_KEY
    private val useOnlineOCR = cloudVisionApiKey.isNotBlank()

    /**
     * Escanea una imagen con OCR híbrido.
     * @param imageUri URI de la imagen
     * @param preferOnline Si true, intenta OCR online primero si hay conexión
     */
    suspend fun scanReceipt(imageUri: Uri, preferOnline: Boolean = false): ReceiptScanResult {
        return try {
            val bitmap = MediaUtils.bitmapFromUri(context, imageUri)
            
            // Preprocesar imagen para mejorar nitidez
            val preprocessedBitmap = preprocessBitmap(bitmap)
            
            if (preferOnline && useOnlineOCR && isNetworkAvailable()) {
                Log.d("ReceiptScanner", "Usando OCR Online (Cloud Vision API)")
                scanReceiptOnline(preprocessedBitmap)
            } else {
                Log.d("ReceiptScanner", "Usando OCR Offline (ML Kit)")
                processBitmapOffline(preprocessedBitmap)
            }
        } catch (e: Exception) {
            Log.e("ReceiptScanner", "Error scanning receipt", e)
            // Fallback a offline si online falla
            try {
                val bitmap = MediaUtils.bitmapFromUri(context, imageUri)
                val preprocessedBitmap = preprocessBitmap(bitmap)
                processBitmapOffline(preprocessedBitmap)
            } catch (e2: Exception) {
                ReceiptScanResult(error = "Error procesando la imagen: ${e2.message}")
            }
        }
    }

    /**
     * Verifica si hay conexión de red disponible.
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        return connectivityManager?.activeNetworkInfo?.isConnectedOrConnecting == true
    }

    /**
     * Preprocesa el bitmap para mejorar la calidad del OCR.
     * - Escala a resolución óptima
     * - Convierte a escala de grises
     * - Aumenta contraste
     * - Aplica umbral binario
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
        
        // 3. Aplicar umbral adaptativo (mejora para texto)
        for (i in pixels.indices) {
            val gray = Color.red(pixels[i])
            val enhanced = lookupTable[gray]
            
            // Umbral binario suave para texto
            val threshold = if (enhanced < 180) 0 else 255
            pixels[i] = Color.rgb(threshold, threshold, threshold)
        }
        
        enhancedBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return enhancedBitmap
    }

    /**
     * Procesa bitmap con ML Kit (offline).
     */
    private suspend fun processBitmapOffline(bitmap: Bitmap): ReceiptScanResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        val visionText: Text = recognizer.process(image).await()

        val fullText = visionText.text
        val lines = visionText.textBlocks.flatMap { it.lines }.map { it.text }

        return parseReceiptText(fullText, lines)
    }

    /**
     * Procesa bitmap con Google Cloud Vision API (online).
     * Requiere API Key configurada en BuildConfig.
     */
    private suspend fun scanReceiptOnline(bitmap: Bitmap): ReceiptScanResult = withContext(Dispatchers.IO) {
        try {
            // Convertir bitmap a Base64
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val imageBytes = byteArrayOutputStream.toByteArray()
            val base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)

            // Crear request para Cloud Vision API
            val requestBody = mapOf(
                "requests" to listOf(
                    mapOf(
                        "image" to mapOf("content" to base64Image),
                        "features" to listOf(
                            mapOf("type" to "TEXT_DETECTION", "maxResults" to 1)
                        )
                    )
                )
            )

            // Hacer request HTTP a Cloud Vision API
            val url = "https://vision.googleapis.com/v1/images:annotate?key=$cloudVisionApiKey"
            
            val jsonBody = com.google.gson.Gson().toJson(requestBody)
            
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            val request = okhttp3.Request.Builder()
                .url(url)
                .post(okhttp3.RequestBody.create(
                    okhttp3.MediaType.parse("application/json"),
                    jsonBody
                ))
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e("ReceiptScanner", "Cloud Vision API error: ${response.code}")
                // Fallback a offline
                return@withContext processBitmapOffline(bitmap)
            }
            
            val responseBody = response.body?.string() ?: run {
                return@withContext processBitmapOffline(bitmap)
            }
            
            // Parsear respuesta de Cloud Vision
            val jsonResponse = com.google.gson.JsonParser.parseString(responseBody).asJsonObject
            val responses = jsonResponse.getAsJsonArray("responses")
            
            if (responses.size() == 0) {
                return@withContext processBitmapOffline(bitmap)
            }
            
            val result = responses[0].asJsonObject
            val fullTextAnnotation = result.getAsJsonObject("fullTextAnnotation")
            
            if (fullTextAnnotation == null || !fullTextAnnotation.has("text")) {
                return@withContext processBitmapOffline(bitmap)
            }
            
            val fullText = fullTextAnnotation.get("text").asString
            val lines = fullText.split("\n").filter { it.isNotBlank() }
            
            Log.d("ReceiptScanner", "Cloud Vision OCR exito. Texto: ${fullText.take(100)}...")
            
            parseReceiptText(fullText, lines)
            
        } catch (e: Exception) {
            Log.e("ReceiptScanner", "Error en OCR online, fallback a offline", e)
            processBitmapOffline(bitmap)
        }
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
            if (line.contains("TOTAL", ignoreCase = true) ||
                line.contains("Total", ignoreCase = true) ||
                line.contains("total", ignoreCase = true)) {
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
 * Helper para verificar si un string contiene alguna de las palabras.
 */
private fun String.containsAny(vararg words: String): Boolean {
    return words.any { this.contains(it, ignoreCase = true) }
}
