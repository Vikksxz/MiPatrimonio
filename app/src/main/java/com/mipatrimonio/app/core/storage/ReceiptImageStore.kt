package com.mipatrimonio.app.core.storage

import android.content.Context
import android.graphics.Bitmap
import com.mipatrimonio.app.core.security.CryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda tickets escaneados como imágenes cifradas en `filesDir/receipts/<uuid>.bin` (T2.5, §11).
 * La imagen se comprime a JPEG antes de cifrar. Ruta relativa devuelta para `TransactionEntity.receiptPath`.
 */
@Singleton
class ReceiptImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager,
) {

    private val receiptsDir: File
        get() = File(context.filesDir, "receipts").also { it.mkdirs() }

    @Throws(IOException::class)
    fun save(bitmap: Bitmap, jpegQuality: Int = 85): String {
        val bytes = ByteArrayOutputStream().use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality.coerceIn(40, 95), out)) {
                throw IOException("No se pudo comprimir la imagen del ticket")
            }
            out.toByteArray()
        }
        val id = UUID.randomUUID().toString()
        val file = File(receiptsDir, "$id.bin")
        try {
            FileOutputStream(file).use { out ->
                cryptoManager.encrypt(ByteArrayInputStream(bytes), out)
            }
        } catch (e: IOException) {
            file.delete()
            throw e
        } catch (e: RuntimeException) {
            file.delete()
            throw e
        }
        return "receipts/$id.bin"
    }

    fun resolve(relativePath: String): File = File(context.filesDir, relativePath)

    fun delete(relativePath: String) {
        runCatching { resolve(relativePath).delete() }
    }
}
