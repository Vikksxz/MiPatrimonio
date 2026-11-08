package com.mipatrimonio.app.core.storage

import android.content.Context
import com.mipatrimonio.app.core.security.CryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Almacena documentos cifrados en `filesDir/documents/<uuid>.bin` (§11).
 * El nombre original y MIME se guardan en la base de datos (DocumentEntity).
 */
@Singleton
class EncryptedDocumentStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager,
) {

    private val documentsDir: File
        get() = File(context.filesDir, "documents").also { it.mkdirs() }

    /** Cifra [input] y lo guarda como un nuevo archivo. Devuelve la ruta relativa (`documents/<id>.bin`). */
    @Throws(IOException::class)
    fun save(input: InputStream): String {
        val id = UUID.randomUUID().toString()
        val file = File(documentsDir, "$id.bin")
        try {
            FileOutputStream(file).use { out ->
                cryptoManager.encrypt(input, out)
            }
        } catch (e: IOException) {
            file.delete()
            throw e
        } catch (e: RuntimeException) {
            file.delete()
            throw e
        }
        return "documents/$id.bin"
    }

    /** Resuelve la ruta relativa guardada en BD a un archivo absoluto. */
    fun resolve(relativePath: String): File = File(context.filesDir, relativePath)

    /** Abre el documento descifrado como stream (el llamador debe cerrarlo). */
    @Throws(IOException::class)
    fun openDecrypted(relativePath: String): InputStream {
        val file = resolve(relativePath)
        return cryptoManager.decrypt(FileInputStream(file))
    }

    /** Descifra a un temporal en `cacheDir` para visores (PdfRenderer). El llamador lo borra. */
    @Throws(IOException::class)
    fun decryptToTemp(relativePath: String): File {
        val file = resolve(relativePath)
        return cryptoManager.decryptToTempFile(file, prefix = "doc_", suffix = ".pdf")
    }

    /** Borra el archivo cifrado. No falla si no existe. */
    fun delete(relativePath: String) {
        resolve(relativePath).delete()
    }
}