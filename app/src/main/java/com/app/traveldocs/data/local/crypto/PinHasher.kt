package com.app.traveldocs.data.local.crypto

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles PIN hashing using Argon2id via BouncyCastle.
 *
 * Argon2id parameters:
 * - Memory: 65536 KB (64 MB)
 * - Iterations: 3
 * - Parallelism: 1
 * - Output length: 32 bytes
 */
@Singleton
class PinHasher @Inject constructor() {

    companion object {
        private const val MEMORY_KB = 65536
        private const val ITERATIONS = 3
        private const val PARALLELISM = 1
        private const val HASH_LENGTH = 32
        private const val SALT_LENGTH = 16
    }

    /**
     * Hashes a PIN with the given salt using Argon2id.
     * @param pin The PIN to hash
     * @param salt A 16-byte salt (use [generateSalt] to create one)
     * @return Base64-encoded hash string
     */
    fun hashPin(pin: String, salt: ByteArray): String {
        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withMemoryAsKB(MEMORY_KB)
            .withIterations(ITERATIONS)
            .withParallelism(PARALLELISM)
            .withSalt(salt)
            .build()

        val generator = Argon2BytesGenerator()
        generator.init(params)

        val hash = ByteArray(HASH_LENGTH)
        generator.generateBytes(pin.toCharArray(), hash)

        return Base64.getEncoder().encodeToString(hash)
    }

    /**
     * Verifies a PIN against a stored hash.
     * @param pin The PIN to verify
     * @param salt The salt used when the hash was created
     * @param storedHash The Base64-encoded hash to compare against
     * @return true if the PIN matches the stored hash
     */
    fun verifyPin(pin: String, salt: ByteArray, storedHash: String): Boolean {
        val computedHash = hashPin(pin, salt)
        return constantTimeEquals(computedHash, storedHash)
    }

    /**
     * Generates a cryptographically secure 16-byte random salt.
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt
    }

    /**
     * Constant-time comparison to prevent timing attacks.
     */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}
