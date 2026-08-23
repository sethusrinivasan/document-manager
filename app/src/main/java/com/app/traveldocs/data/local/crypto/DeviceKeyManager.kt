package com.app.traveldocs.data.local.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the device-specific encryption key stored in Android KeyStore.
 *
 * The device key is a 256-bit AES key that is:
 * - Generated once on first launch
 * - Hardware-backed (never leaves the secure element)
 * - Not exportable
 * - Used as salt in HKDF key derivation for database encryption
 */
@Singleton
class DeviceKeyManager @Inject constructor() {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "travel_docs_device_key"
        private const val KEY_SIZE = 256
    }

    /**
     * Gets the existing device key or creates a new one if it doesn't exist.
     *
     * @return 32-byte (256-bit) device key material for use in key derivation.
     *         Since AndroidKeyStore keys are not directly exportable, this returns
     *         encoded key bytes from the KeyStore entry.
     */
    fun getOrCreateDeviceKey(): ByteArray {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
        keyStore.load(null)

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateDeviceKey()
        }

        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey.encoded ?: deriveKeyBytes(entry.secretKey)
    }

    private fun generateDeviceKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(KEY_SIZE)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(false)
            .build()

        keyGenerator.init(spec)
        keyGenerator.generateKey()
    }

    /**
     * On hardware-backed keystores, [SecretKey.getEncoded] may return null.
     * In that case, we use the key to encrypt a known value and use the
     * ciphertext as deterministic key material for derivation.
     */
    private fun deriveKeyBytes(secretKey: SecretKey): ByteArray {
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        val nonce = getOrCreateNonce()
        val fixedIv = javax.crypto.spec.GCMParameterSpec(128, nonce)
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey, fixedIv)
        // Encrypt a fixed known value to produce deterministic output
        val knownInput = "travel_docs_device_key_derivation".toByteArray(Charsets.UTF_8)
        val encrypted = cipher.doFinal(knownInput)
        // Take first 32 bytes of the ciphertext as the derived key material
        return encrypted.copyOf(32)
    }

    private fun getOrCreateNonce(): ByteArray {
        val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(null)
        // Fallback: use a fixed-but-non-zero nonce derived from the key alias
        return "travel_docs_nonce_v1".toByteArray(Charsets.UTF_8).copyOf(12)
    }
}
