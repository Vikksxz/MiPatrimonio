package com.mipatrimonio.app.feature.wallet

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File

/**
 * Guarda la foto propia de una tarjeta en el almacenamiento privado de la app
 * (MiPatrimonio.md §5.5). Nunca se guardan números ni datos de la tarjeta.
 *
 * Se usa `noBackupFilesDir` para que la imagen no salga del dispositivo en copias
 * de seguridad, y todos los borrados se limitan a la carpeta `card_photos`.
 */
object CardPhotoStore {

    private const val DIRECTORY = "card_photos"

    /** Lado máximo de la imagen guardada. */
    private const val MAX_DIMENSION = 1280
    private const val JPEG_QUALITY = 85

    /**
     * Copia la imagen elegida con el selector del sistema a la carpeta privada,
     * aplicando la orientación EXIF y reduciéndola a [MAX_DIMENSION] px de lado mayor.
     * Devuelve la ruta absoluta del archivo copiado, o null si no se pudo leer.
     */
    fun copyToPrivate(context: Context, uri: Uri): String? = runCatching {
        val bitmap = loadScaledBitmap(context, uri) ?: return null
        val directory = photosDirectory(context).apply { mkdirs() }
        val file = File(directory, "card_${System.currentTimeMillis()}.jpg")
        try {
            file.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            }
        } finally {
            bitmap.recycle()
        }
        file.absolutePath
    }.getOrNull()

    /**
     * Borra una imagen del almacenamiento privado. Solo actúa dentro de la carpeta
     * `card_photos`, para que una ruta corrupta o restaurada no borre otros datos.
     */
    fun delete(context: Context, path: String?) {
        if (path.isNullOrBlank()) return
        runCatching {
            val directory = photosDirectory(context).canonicalFile
            val file = File(path).canonicalFile
            if (file.parentFile == directory) {
                file.delete()
            }
        }
    }

    /** Guarda bytes de imagen (JPEG) en la carpeta privada y devuelve la ruta absoluta (T4.2). */
    fun saveBytes(context: Context, bytes: ByteArray): String? = runCatching {
        val directory = photosDirectory(context).apply { mkdirs() }
        val file = File(directory, "card_${System.currentTimeMillis()}_${bytes.size}.jpg")
        file.outputStream().use { it.write(bytes) }
        file.absolutePath
    }.getOrNull()

    private fun photosDirectory(context: Context): File =
        File(context.noBackupFilesDir, DIRECTORY)

    private fun loadScaledBitmap(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: return null

        val oriented = applyExifOrientation(context, uri, decoded)
        return scaleDown(oriented, MAX_DIMENSION)
    }

    private fun sampleSizeFor(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        while (width / (sampleSize * 2) >= maxDimension || height / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun applyExifOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longestSide = maxOf(bitmap.width, bitmap.height)
        if (longestSide <= maxDimension) return bitmap
        val ratio = maxDimension.toFloat() / longestSide
        val width = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val height = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }
}
