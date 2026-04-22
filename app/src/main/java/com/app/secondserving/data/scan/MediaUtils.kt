package com.app.secondserving.data.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.InputStream

/**
 * Utilidades para procesamiento de imágenes.
 */
object MediaUtils {

    /**
     * Convierte un URI a Bitmap de forma segura.
     */
    fun bitmapFromUri(context: Context, uri: Uri): Bitmap {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
        return bitmap ?: throw IllegalArgumentException("No se pudo decodificar la imagen desde el URI: $uri")
    }

    /**
     * Convierte un InputStream a Bitmap de forma segura.
     */
    fun bitmapFromStream(inputStream: InputStream): Bitmap {
        return BitmapFactory.decodeStream(inputStream)
            ?: throw IllegalArgumentException("No se pudo decodificar la imagen desde el stream")
    }

    /**
     * Escala un Bitmap a un tamaño máximo (para optimizar el rendimiento del OCR).
     * Esto reduce el consumo de memoria y acelera el procesamiento de ML Kit.
     */
    fun scaleBitmap(bitmap: Bitmap, maxSize: Int = 1024): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Calculamos el ratio de escalado para mantener la relación de aspecto
        val ratio = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)

        // Si la imagen ya es más pequeña que el máximo, no escalamos
        if (ratio >= 1f) return bitmap

        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
