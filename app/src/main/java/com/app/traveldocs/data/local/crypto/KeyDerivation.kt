package com.app.traveldocs.data.local.crypto

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Derives database encryption keys using HKDF (HMAC-based Key Derivation Function)
 * with SHA-256.
 *
 * The derived key combines the user's PIN with a hardware-backed device key,
 * ensuring the database can only be decrypted when both are available.
 */
@Singleton
class KeyDerivation @Inject constructor() {

    companion object {
        private const val DB_KEY_LENGTH = 32
        private val INFO = "travel_docs_db_key".toByteArray(Charsets.UTF_8)
    }

    /**
     * Derives a 32-byte database encryption key from the PIN and device key using HKDF.
     *
     * @param pin The user's PIN (used as input keying material)
     * @param deviceKey The hardware-backed device key (used as salt)
     * @return 32-byte derived key
     */
    fun deriveDbKey(pin: String, deviceKey: ByteArray): ByteArray {
        val hkdf = HKDFBytesGenerator(SHA256Digest())
        val params = HKDFParameters(
            pin.toByteArray(Charsets.UTF_8),  // Input keying material
            deviceKey,                         // Salt
            INFO                               // Context info
        )
        hkdf.init(params)

        val derivedKey = ByteArray(DB_KEY_LENGTH)
        hkdf.generateBytes(derivedKey, 0, DB_KEY_LENGTH)
        return derivedKey
    }
}
