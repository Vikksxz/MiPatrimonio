package com.mipatrimonio.app.core.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.SecureRandom

/**
 * Tests instrumentados de CryptoManager (T2.1).
 * Requieren dispositivo/emulador Android para acceder a Keystore.
 */
@RunWith(AndroidJUnit4::class)
class CryptoManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext<Context>()
    private val cryptoManager = CryptoManager(context)

    @Test
    fun encryptDecryptRoundTrip() {
        val original = "Contenido de prueba para cifrado AES-256-GCM".toByteArray(StandardCharsets.UTF_8)
        val input = ByteArrayInputStream(original)
        val output = ByteArrayOutputStream()

        cryptoManager.encrypt(input, output)

        val encrypted = output.toByteArray()
        assertTrue("Debe contener IV (12 bytes) + ciphertext", encrypted.size >= 12 + original.size)

        val decrypted = ByteArrayOutputStream()
        cryptoManager.decrypt(ByteArrayInputStream(encrypted)).use { decryptedStream ->
            decryptedStream.copyTo(decrypted)
        }

        assertArrayEquals(original, decrypted.toByteArray())
    }

    @Test
    fun encryptDecryptEmptyInput() {
        val original = ByteArray(0)
        val input = ByteArrayInputStream(original)
        val output = ByteArrayOutputStream()

        cryptoManager.encrypt(input, output)

        val encrypted = output.toByteArray()
        assertTrue("Debe contener al menos IV + auth tag", encrypted.size >= 12)

        val decrypted = ByteArrayOutputStream()
        cryptoManager.decrypt(ByteArrayInputStream(encrypted)).use { decryptedStream ->
            decryptedStream.copyTo(decrypted)
        }

        assertArrayEquals(original, decrypted.toByteArray())
    }

    @Test
    fun encryptDecryptLargeInput() {
        // 100 KB de datos aleatorios
        val original = ByteArray(100_000).also { SecureRandom().nextBytes(it) }
        val input = ByteArrayInputStream(original)
        val output = ByteArrayOutputStream()

        cryptoManager.encrypt(input, output)

        val decrypted = ByteArrayOutputStream()
        cryptoManager.decrypt(ByteArrayInputStream(output.toByteArray())).use { decryptedStream ->
            decryptedStream.copyTo(decrypted)
        }

        assertArrayEquals(original, decrypted.toByteArray())
    }

    @Test
    fun decryptToTempFileRoundTrip() {
        val original = "PDF simulado para visor %PDF-1.4...".toByteArray(StandardCharsets.UTF_8)

        // Cifrar a archivo
        val encryptedFile = File.createTempFile("enc_", ".bin", context.cacheDir)
        try {
            ByteArrayInputStream(original).use { fis ->
                FileOutputStream(encryptedFile).use { fos ->
                    cryptoManager.encrypt(fis, fos)
                }
            }

            // Descifrar a archivo temporal
            val tempFile = cryptoManager.decryptToTempFile(encryptedFile)
            try {
                assertTrue(tempFile.exists())
                assertTrue(tempFile.length() > 0)

                val decrypted = tempFile.readBytes()
                assertArrayEquals(original, decrypted)
            } finally {
                tempFile.delete()
            }
        } finally {
            encryptedFile.delete()
        }
    }

    @Test
    fun ivIsRandomPerEncryption() {
        val original = "Mismo contenido".toByteArray(StandardCharsets.UTF_8)

        val encrypted1 = ByteArrayOutputStream().apply { cryptoManager.encrypt(ByteArrayInputStream(original), this) }.toByteArray()
        val encrypted2 = ByteArrayOutputStream().apply { cryptoManager.encrypt(ByteArrayInputStream(original), this) }.toByteArray()

        // Los primeros 12 bytes (IV) deben ser diferentes
        val iv1 = encrypted1.copyOfRange(0, 12)
        val iv2 = encrypted2.copyOfRange(0, 12)
        assertTrue("IV debe ser único por cifrado", !iv1.contentEquals(iv2))
    }

    @Test(expected = IOException::class)
    fun decryptCorruptedFileThrows() {
        val corrupted = ByteArray(20).also { SecureRandom().nextBytes(it) }
        cryptoManager.decrypt(ByteArrayInputStream(corrupted)).use { it.readAllBytes() }
    }

    @Test
    fun keyPersistsAcrossInstances() {
        // Crear una segunda instancia y verificar que usa la misma clave
        val cryptoManager2 = CryptoManager(context)

        val original = "Persistencia de clave".toByteArray(StandardCharsets.UTF_8)
        val encrypted = ByteArrayOutputStream().apply { cryptoManager.encrypt(ByteArrayInputStream(original), this) }.toByteArray()

        val decrypted = ByteArrayOutputStream()
        cryptoManager2.decrypt(ByteArrayInputStream(encrypted)).use { decryptedStream ->
            decryptedStream.copyTo(decrypted)
        }

        assertArrayEquals(original, decrypted.toByteArray())
    }
}