package com.mipatrimonio.app.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CryptoManager (T2.1, MiPatrimonio.md §11):
 * AES-256-GCM con clave en Android Keystore.
 * Formato de archivo: [12 bytes IV][datos cifrados].
 * No requiere autenticación de usuario (para workers en segundo plano).
 */
@Singleton
class CryptoManager @Inject constructor(
    private val context: Context,
) {

    private val KEY_ALIAS = "MiPatrimonio_AES_256_GCM"
    private val ANDROID_KEYSTORE = "AndroidKeyStore"
    private val AES_GCM = "AES/GCM/NoPadding"
    private val IV_LENGTH = 12 // 96 bits, recomendado para GCM
    private val BUFFER_SIZE = 8192

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    private val secretKey: SecretKey = getOrCreateKey()

    /**
     * Cifra un stream de entrada y escribe el resultado en el stream de salida.
     * Formato: [IV (12 bytes)][datos cifrados con auth tag incluido].
     * El IV lo genera el Keystore (obligatorio con setRandomizedEncryptionRequired).
     */
    @Throws(IOException::class)
    fun encrypt(input: InputStream, output: OutputStream) {
        try {
            val cipher = Cipher.getInstance(AES_GCM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            require(iv != null && iv.size == IV_LENGTH) { "IV generado inválido" }

            // Escribir IV primero
            output.write(iv)

            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                val encrypted = cipher.update(buffer, 0, bytesRead)
                if (encrypted != null) output.write(encrypted)
            }
            val finalBytes = cipher.doFinal()
            if (finalBytes.isNotEmpty()) output.write(finalBytes)
        } catch (e: GeneralSecurityException) {
            throw IOException("Error al cifrar", e)
        }
    }

    /**
     * Descifra un stream de entrada (formato: [IV][ciphertext+tag]) y devuelve
     * un InputStream con los datos descifrados.
     * Útil para leer documentos sin escribir a disco.
     */
    @Throws(IOException::class)
    fun decrypt(input: InputStream): InputStream {
        val iv = ByteArray(IV_LENGTH)
        val ivRead = input.read(iv)
        if (ivRead != IV_LENGTH) {
            throw IOException("Archivo corrupto: IV incompleto")
        }

        try {
            val cipher = Cipher.getInstance(AES_GCM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))

            val output = ByteArrayOutputStream()
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                val decrypted = cipher.update(buffer, 0, bytesRead)
                if (decrypted != null) output.write(decrypted)
            }
            val finalBytes = cipher.doFinal()
            if (finalBytes.isNotEmpty()) output.write(finalBytes)

            return ByteArrayInputStream(output.toByteArray())
        } catch (e: GeneralSecurityException) {
            throw IOException("Archivo corrupto o manipulado", e)
        }
    }

    /**
     * Descifra un archivo cifrado a un archivo temporal en cacheDir.
     * Para visor de PDF (T2.2): descifra, abre con PdfRenderer, borra al cerrar.
     * El llamador es responsable de borrar el archivo devuelto.
     */
    @Throws(IOException::class)
    fun decryptToTempFile(encryptedFile: File, prefix: String = "view_", suffix: String = ".pdf"): File {
        val tempFile = File.createTempFile(prefix, suffix, context.cacheDir)
        FileInputStream(encryptedFile).use { fis ->
            FileOutputStream(tempFile).use { fos ->
                decrypt(fis).use { decrypted ->
                    decrypted.copyTo(fos)
                }
            }
        }
        return tempFile
    }

    /** Obtiene la clave del Keystore o la crea si no existe. */
    private fun getOrCreateKey(): SecretKey {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return keyStore.getKey(KEY_ALIAS, null) as SecretKey
        }
        return createKey()
    }

    /** Crea una clave AES-256 en Keystore sin autenticación de usuario. */
    private fun createKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false) // Workers en segundo plano
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Elimina la clave del Keystore (para pruebas o reset de usuario).
     * No se usa en flujo normal.
     */
    fun deleteKey() {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }

    companion object {
        /** Versión del formato de archivo para futuras migraciones. */
        const val FILE_FORMAT_VERSION = 1
    }
}