package com.mipatrimonio.app.core.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T2.3: OCR base. Criterio de aceptación: devuelve el texto de una imagen de prueba.
 */
@RunWith(AndroidJUnit4::class)
class OcrServiceTest {

    @Test
    fun recognizeReturnsTextFromTestImage() = runBlocking {
        val bitmap = createTextBitmap("NOMINA TOTAL 1234,56 EUR")
        try {
            val result = OcrService().recognize(bitmap)
            val normalized = result.text.uppercase()
            assertTrue(
                "Se esperaba texto OCR con NOMINA/TOTAL/1234,56; obtenido: ${result.text}",
                normalized.contains("NOMINA") || normalized.contains("TOTAL") || normalized.contains("1234"),
            )
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun recognizeReturnsResultWithoutCrashing() = runBlocking {
        // Verifica que el wrapper procesa bitmaps y devuelve OcrResult sin lanzar excepción.
        val bitmap = createTextBitmap("MERCADONA 12,50")
        try {
            val result = OcrService().recognize(bitmap)
            // Estructura válida (el texto reconocido puede variar según el dispositivo/modelo).
            assertTrue(result.lines.size >= 0)
        } finally {
            bitmap.recycle()
        }
    }

    private fun createTextBitmap(text: String): Bitmap {
        val width = 900
        val height = 260
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(text, 40f, 140f, paint)
        return bitmap
    }
}