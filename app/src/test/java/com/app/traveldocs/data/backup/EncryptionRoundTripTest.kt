package com.app.traveldocs.data.backup

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class EncryptionRoundTripTest {
    @Test
    fun `encrypt-decrypt preserves data`() {
        val key = genKey()
        val orig = "Hello document".toByteArray()
        assertArrayEquals(orig, decrypt(encrypt(orig, key), key))
    }
    @Test
    fun `binary round trip`() {
        val key = genKey()
        val orig = ByteArray(4096) { (it % 256).toByte() }
        assertArrayEquals(orig, decrypt(encrypt(orig, key), key))
    }
    @Test
    fun `encrypted differs from plaintext`() {
        val key = genKey()
        val orig = "Secret".toByteArray()
        assertFalse(encrypt(orig, key).contentEquals(orig))
    }
    @Test
    fun `wrong key fails`() {
        val k1 = genKey(); val k2 = genKey()
        val enc = encrypt("data".toByteArray(), k1)
        assertThrows(Exception::class.java) { decrypt(enc, k2) }
    }
    @Test
    fun `tampered ciphertext fails`() {
        val key = genKey()
        val enc = encrypt("data".toByteArray(), key)
        enc[enc.size - 3] = (enc[enc.size - 3].toInt() xor 0xFF).toByte()
        assertThrows(Exception::class.java) { decrypt(enc, key) }
    }
    @Test
    fun `empty plaintext works`() {
        val key = genKey()
        assertArrayEquals(ByteArray(0), decrypt(encrypt(ByteArray(0), key), key))
    }
    @Test
    fun `10MB round trip`() {
        val key = genKey()
        val orig = ByteArray(10 * 1024 * 1024) { (it % 199).toByte() }
        assertArrayEquals(orig, decrypt(encrypt(orig, key), key))
    }
    private fun genKey(): ByteArray { val kg = KeyGenerator.getInstance("AES"); kg.init(256); return kg.generateKey().encoded }
    private fun encrypt(p: ByteArray, k: ByteArray): ByteArray { val c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(k, "AES")); return c.iv + c.doFinal(p) }
    private fun decrypt(d: ByteArray, k: ByteArray): ByteArray { val c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE, SecretKeySpec(k, "AES"), GCMParameterSpec(128, d, 0, 12)); return c.doFinal(d, 12, d.size - 12) }
}
