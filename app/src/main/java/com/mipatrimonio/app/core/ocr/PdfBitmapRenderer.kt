package com.mipatrimonio.app.core.ocr

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.IOException

/**
 * Renderiza páginas de un PDF a Bitmap para OCR (T2.3, §9.1).
 * Ancho objetivo ~1600 px para buena lectura OCR.
 */
object PdfBitmapRenderer {

    private const val TARGET_WIDTH = 1600

    @Throws(IOException::class)
    fun renderFirstPage(pdfFile: File, targetWidth: Int = TARGET_WIDTH): Bitmap {
        ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                if (renderer.pageCount <= 0) throw IOException("PDF sin páginas")
                renderer.openPage(0).use { page ->
                    val scale = if (page.width > 0) targetWidth.toFloat() / page.width.toFloat() else 1f
                    val width = targetWidth.coerceAtLeast(1)
                    val height = (page.height * scale).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    return bitmap
                }
            }
        }
    }
}