package com.app.traveldocs.data.local

import android.content.Context
import com.app.traveldocs.debug.DebugLogger
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-document PIN protection.
 *
 * How it works:
 * - When user sets a PIN on a document, we derive a key from that PIN using PBKDF2
 * - The document's file bytes are re-encrypted with this PIN-derived key (on top of the
 *   existing device-level AES-256-GCM encryption)
 * - The PIN hash is stored in SharedPreferences so we can verify attempts
 * - The actual decryption key exists ONLY when the correct PIN is entered
 *
 * Important guarantees:
 * - Developer cannot recover PIN-protected documents (no backdoor, no master key)
 * - If user forgets the PIN, the document is permanently inaccessible
 * - This is a privacy feature — user bears full responsibility
 *
 * Tagged with system tag "__PIN_PROTECTED" for folder grouping on home screen.
 */
@Singleton
class SecureDocumentManager @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    companion object {
        const val SECURE_TAG = "__PIN_PROTECTED"
        const val SECURE_FOLDER_DISPLAY_NAME = "Secure Docs"
        private const val PREFS_NAME = "secure_doc_pins"
        private const val PBKDF2_ITERATIONS = 10000
        private const val KEY_LENGTH_BITS = 256
        private const val GCM_TAG_LENGTH = 128
    }

    private fun prefs() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Check if a document has a PIN set.
     */
    fun hasPinSet(documentId: String): Boolean {
        return prefs().contains("pin_hash_$documentId")
    }

    /**
     * Set a PIN on a document. Stores the hash for verification.
     * Returns the derived encryption key (caller uses it to re-encrypt the file).
     */
    fun setPin(documentId: String, pin: String): ByteArray {
        val salt = generateSalt()
        val pinHash = hashPin(pin, salt)
        prefs().edit()
            .putString("pin_hash_$documentId", pinHash)
            .putString("pin_salt_$documentId", salt.toHexString())
            .putLong("pin_set_time_$documentId", System.currentTimeMillis())
            .apply()
        DebugLogger.i("SecureDoc", "PIN set for document ${documentId.take(8)}...")
        return deriveKey(pin, salt)
    }

    /**
     * Verify a PIN attempt. Returns the derived key if correct, null if wrong.
     */
    fun verifyPin(documentId: String, pin: String): ByteArray? {
        val storedHash = prefs().getString("pin_hash_$documentId", null) ?: return null
        val saltHex = prefs().getString("pin_salt_$documentId", null) ?: return null
        val salt = saltHex.hexToByteArray()
        val attemptHash = hashPin(pin, salt)
        return if (attemptHash == storedHash) {
            deriveKey(pin, salt)
        } else {
            DebugLogger.w("SecureDoc", "PIN verification failed for ${documentId.take(8)}...")
            null
        }
    }

    /**
     * Remove PIN protection from a document.
     */
    fun removePin(documentId: String) {
        prefs().edit()
            .remove("pin_hash_$documentId")
            .remove("pin_salt_$documentId")
            .remove("pin_set_time_$documentId")
            .apply()
        DebugLogger.i("SecureDoc", "PIN removed for document ${documentId.take(8)}...")
    }

    /**
     * Get all document IDs that have PINs set.
     */
    fun getAllSecuredDocumentIds(): Set<String> {
        return prefs().all.keys
            .filter { it.startsWith("pin_hash_") }
            .map { it.removePrefix("pin_hash_") }
            .toSet()
    }

    /**
     * Encrypt bytes with a PIN-derived key (AES-256-GCM).
     */
    fun encryptWithPin(data: ByteArray, pinKey: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(pinKey, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        val iv = cipher.iv  // GCM generates a random IV
        val encrypted = cipher.doFinal(data)
        // Prepend IV (12 bytes) to ciphertext
        return iv + encrypted
    }

    /**
     * Decrypt bytes with a PIN-derived key (AES-256-GCM).
     */
    fun decryptWithPin(data: ByteArray, pinKey: ByteArray): ByteArray? {
        return try {
            if (data.size < 12) return null
            val iv = data.copyOfRange(0, 12)
            val ciphertext = data.copyOfRange(12, data.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val keySpec = SecretKeySpec(pinKey, "AES")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            DebugLogger.e("SecureDoc", "Decryption failed (wrong PIN or corrupted data)", e)
            null
        }
    }

    // --- Internal crypto helpers ---

    private fun deriveKey(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        val key = deriveKey(pin, salt)
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(key).toHexString()
    }

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        java.security.SecureRandom().nextBytes(salt)
        return salt
    }

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

    private fun String.hexToByteArray(): ByteArray {
        val len = length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(this[i], 16) shl 4) + Character.digit(this[i + 1], 16)).toByte()
        }
        return data
    }
}
