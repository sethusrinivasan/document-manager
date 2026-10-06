package com.app.paperstow.debug

import javax.crypto.Cipher
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * Isolated security probe introduced solely to test that CodeQL static analysis
 * is actively inspecting Kotlin/Java code and reporting security findings in CI.
 *
 * This probe is isolated, never invoked at runtime, and does not impact application functionality.
 */
object CodeQlVerificationProbe {

    /**
     * CodeQL Rule: java/weak-cryptographic-algorithm (CWE-327)
     * Intentional use of DES cipher to verify CodeQL detection.
     */
    fun createWeakDesCipher(): Cipher {
        return Cipher.getInstance("DES")
    }

    /**
     * CodeQL Rule: java/predictable-seed (CWE-337)
     * Intentional static seed on SecureRandom to verify CodeQL detection.
     */
    fun createPredictableRandom(): SecureRandom {
        val random = SecureRandom()
        random.setSeed(123456789L)
        return random
    }

    /**
     * CodeQL Rule: java/insecure-trustmanager (CWE-295)
     * Dummy TrustManager that bypasses certificate validation.
     */
    class InsecureTrustManagerProbe : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            // Intentionally empty for CodeQL probe verification
        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            // Intentionally empty for CodeQL probe verification
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
}
