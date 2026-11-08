package com.mipatrimonio.app.core.backup

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cifrado/descifrado de copias de seguridad .mpbak con contraseña (T4.2, §13.2).
 * Deriva clave con PBKDF2-HMAC-SHA256 y cifra con AES-256-GCM.
 *
 * Cabecera en claro: [4B magic "MPBK"][1B version][16B salt][12B IV][ciphertext+tag]
 */
object BackupCrypto {

    private val MAGIC = byteArrayOf('M'.code.toByte(), 'P'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte())
    private const val FORMAT_VERSION: Byte = 1
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128
    internal const val HEADER_LENGTH = 4 + 1 + SALT_LENGTH + IV_LENGTH // 33
    internal const val DEFAULT_ITERATIONS = 600_000
    private const val BUFFER = 8192
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val CIPHER_ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "AES"

    private val random = SecureRandom()

    @Throws(IOException::class)
    fun encrypt(
        input: InputStream,
        output: OutputStream,
        password: CharArray,
        iterations: Int = DEFAULT_ITERATIONS,
    ) {
        require(password.isNotEmpty()) { "La contraseña no puede estar vacía" }
        require(iterations > 0) { "Iteraciones inválidas" }
        val salt = ByteArray(SALT_LENGTH).also { random.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { random.nextBytes(it) }
        try {
            val key = deriveKey(password, salt, iterations)
            val cipher = Cipher.getInstance(CIPHER_ALGORITHM).apply {
                init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, KEY_ALGORITHM), GCMParameterSpec(TAG_BITS, iv))
            }
            output.write(MAGIC)
            output.write(FORMAT_VERSION.toInt())
            output.write(salt)
            output.write(iv)
            val buffer = ByteArray(BUFFER)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                val enc = cipher.update(buffer, 0, read)
                if (enc != null) output.write(enc)
            }
            val final = cipher.doFinal()
            if (final.isNotEmpty()) output.write(final)
            output.flush()
        } catch (e: GeneralSecurityException) {
            throw IOException("Error al cifrar la copia", e)
        }
    }

    @Throws(IOException::class)
    fun decrypt(
        input: InputStream,
        output: OutputStream,
        password: CharArray,
        iterations: Int = DEFAULT_ITERATIONS,
    ) {
        require(password.isNotEmpty()) { "La contraseña no puede estar vacía" }
        val magic = ByteArray(4)
        readFully(input, magic, "Cabecera incompleta")
        if (!magic.contentEquals(MAGIC)) throw IOException("Archivo de copia no válido")
        val version = input.read()
        if (version != FORMAT_VERSION.toInt()) throw IOException("Versión de copia no compatible")
        val salt = ByteArray(SALT_LENGTH)
        readFully(input, salt, "Cabecera incompleta")
        val iv = ByteArray(IV_LENGTH)
        readFully(input, iv, "Cabecera incompleta")
        try {
            val key = deriveKey(password, salt, iterations)
            val cipher = Cipher.getInstance(CIPHER_ALGORITHM).apply {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(key, KEY_ALGORITHM), GCMParameterSpec(TAG_BITS, iv))
            }
            val buffer = ByteArray(BUFFER)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                val dec = cipher.update(buffer, 0, read)
                if (dec != null) output.write(dec)
            }
            val final = cipher.doFinal()
            if (final.isNotEmpty()) output.write(final)
            output.flush()
        } catch (e: GeneralSecurityException) {
            throw IOException("Contraseña incorrecta o archivo corrupto", e)
        } catch (e: IOException) {
            if (isAuthFailure(e)) throw IOException("Contraseña incorrecta o archivo corrupto", e)
            throw e
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun readFully(input: InputStream, buf: ByteArray, error: String) {
        var off = 0
        while (off < buf.size) {
            val n = input.read(buf, off, buf.size - off)
            if (n < 0) throw IOException(error)
            off += n
        }
    }

    private fun isAuthFailure(e: IOException): Boolean {
        val msg = e.message?.lowercase().orEmpty()
        return msg.contains("mac") || msg.contains("tag") || msg.contains("pad")
    }
}
