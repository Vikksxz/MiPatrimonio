package com.mipatrimonio.app.core.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** Resultado simple de OCR (T2.3). */
data class OcrResult(
    val text: String,
    val lines: List<String>,
)

/**
 * Wrapper de ML Kit Text Recognition (latino, en dispositivo) (T2.3, §9).
 * Aceptación T2.3: devuelve el texto de una imagen de prueba.
 */
@Singleton
class OcrService @Inject constructor() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(bitmap: Bitmap): OcrResult = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                if (cont.isActive) {
                    val lines = result.textBlocks.flatMap { block ->
                        block.lines.map { it.text }
                    }
                    cont.resume(OcrResult(text = result.text, lines = lines))
                }
            }
            .addOnFailureListener { e ->
                if (cont.isActive) cont.resumeWithException(e)
            }
    }

    fun close() {
        recognizer.close()
    }
}