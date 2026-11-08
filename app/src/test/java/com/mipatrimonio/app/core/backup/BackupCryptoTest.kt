package com.mipatrimonio.app.core.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupCryptoTest {

    // Iteraciones bajas solo para tests (velocidad); la app usa DEFAULT_ITERATIONS.
    private val testIterations = 1_000
    private val password = "contraseña-de-prueba-123".toCharArray()

    @Test
    fun roundTrip_smallPayload() {
        val plain = "hola MiPatrimonio €".toByteArray(Charsets.UTF_8)
        val enc = ByteArrayOutputStream()
        BackupCrypto.encrypt(ByteArrayInputStream(plain), enc, password, testIterations)
        val dec = ByteArrayOutputStream()
        BackupCrypto.decrypt(ByteArrayInputStream(enc.toByteArray()), dec, password, testIterations)
        assertArrayEquals(plain, dec.toByteArray())
    }

    @Test
    fun roundTrip_largePayload_streaming() {
        val plain = ByteArray(256 * 1024) { (it % 251).toByte() }
        val enc = ByteArrayOutputStream()
        BackupCrypto.encrypt(ByteArrayInputStream(plain), enc, password, testIterations)
        val dec = ByteArrayOutputStream()
        BackupCrypto.decrypt(ByteArrayInputStream(enc.toByteArray()), dec, password, testIterations)
        assertArrayEquals(plain, dec.toByteArray())
    }

    @Test
    fun header_hasMagicAndVersion() {
        val enc = ByteArrayOutputStream()
        BackupCrypto.encrypt(ByteArrayInputStream(byteArrayOf(1, 2, 3)), enc, password, testIterations)
        val bytes = enc.toByteArray()
        assertEquals('M'.code.toByte(), bytes[0])
        assertEquals('P'.code.toByte(), bytes[1])
        assertEquals('B'.code.toByte(), bytes[2])
        assertEquals('K'.code.toByte(), bytes[3])
        assertEquals(1.toByte(), bytes[4])
        assertTrue(bytes.size > BackupCrypto.HEADER_LENGTH)
    }

    @Test
    fun wrongPassword_fails() {
        val enc = ByteArrayOutputStream()
        BackupCrypto.encrypt(ByteArrayInputStream("secret".toByteArray()), enc, password, testIterations)
        try {
            BackupCrypto.decrypt(
                ByteArrayInputStream(enc.toByteArray()),
                ByteArrayOutputStream(),
                "otra".toCharArray(),
                testIterations,
            )
            fail("Debería fallar con contraseña incorrecta")
        } catch (e: IOException) {
            assertTrue(e.message?.contains("Contraseña incorrecta") == true || e.message?.contains("corrupto") == true)
        }
    }

    @Test
    fun tamperedCiphertext_fails() {
        val enc = ByteArrayOutputStream()
        BackupCrypto.encrypt(ByteArrayInputStream("secret".toByteArray()), enc, password, testIterations)
        val tampered = enc.toByteArray()
        tampered[tampered.size - 1] = (tampered[tampered.size - 1].toInt() xor 0x01).toByte()
        try {
            BackupCrypto.decrypt(ByteArrayInputStream(tampered), ByteArrayOutputStream(), password, testIterations)
            fail("Debería fallar con datos manipulados")
        } catch (_: IOException) {
            // esperado
        }
    }

    @Test
    fun invalidMagic_fails() {
        val bad = ByteArray(BackupCrypto.HEADER_LENGTH + 16) { 0 }
        bad[0] = 'X'.code.toByte()
        try {
            BackupCrypto.decrypt(ByteArrayInputStream(bad), ByteArrayOutputStream(), password, testIterations)
            fail("Debería rechazar magic inválido")
        } catch (e: IOException) {
            assertTrue(e.message?.contains("no válido") == true)
        }
    }
}
