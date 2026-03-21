package com.app.secondserving.data

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
     * Convierte un URI a Bitmap.
     */
    fun bitmapFromUri(context: Context, uri: Uri): Bitmap {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            return BitmapFactory.decodeStream(inputStream)
        }
        throw IllegalArgumentException("No se pudo abrir la imagen: $uri")
    }

    /**
     * Convierte un InputStream a Bitmap.
     */
    fun bitmapFromStream(inputStream: InputStream): Bitmap {
        return BitmapFactory.decodeStream(inputStream)
            ?: throw IllegalArgumentException("No se pudo decodificar la imagen")
    }

    /**
     * Escala un Bitmap a un tamaño máximo (para optimizar OCR).
     */
    fun scaleBitmap(bitmap: Bitmap, maxSize: Int = 1024): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val ratio = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)

        if (ratio >= 1f) return bitmap

        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
