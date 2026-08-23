package com.app.traveldocs.data.local.storage

import android.content.Context
import com.app.traveldocs.domain.model.DocumentFormat
import com.app.traveldocs.domain.repository.DocumentFileStorage
import java.io.File
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * A test-friendly variant of [DocumentFileStorageImpl] that uses standard JCE
 * instead of Android KeyStore for encryption. All file storage and secure deletion
 * logic is identical to the production implementation.
 */
class TestableDocumentFileStorage(
    private val context: Context
) : DocumentFileStorage {

    companion object {
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val DOCS_DIR = "docs"
        private const val FILE_EXTENSION = ".enc"
    }

    private val secretKey: SecretKey = generateTestKey()

    private fun generateTestKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        return keyGenerator.generateKey()
    }

    override suspend fun store(
        memberId: String,
        fileData: ByteArray,
        format: DocumentFormat
    ): Result<String> = runCatching {
        val fileId = UUID.randomUUID().toString()
        val memberDir = getMemberDir(memberId)
        memberDir.mkdirs()

        val encryptedData = encrypt(fileData)
        val file = File(memberDir, "$fileId$FILE_EXTENSION")
        file.writeBytes(encryptedData)

        fileId
    }

    override suspend fun retrieve(fileId: String): Result<ByteArray> = runCatching {
        val file = findFile(fileId)
            ?: throw IllegalArgumentException("File not found: $fileId")

        val encryptedData = file.readBytes()
        decrypt(encryptedData)
    }

    override suspend fun secureDelete(fileId: String): Result<Unit> = runCatching {
        val file = findFile(fileId)
            ?: throw IllegalArgumentException("File not found: $fileId")

        // Overwrite with random bytes of the same size
        val random = SecureRandom()
        val randomBytes = ByteArray(file.length().toInt())
        random.nextBytes(randomBytes)
        file.writeBytes(randomBytes)

        // Delete the file
        if (!file.delete()) {
            throw IllegalStateException("Failed to delete file: $fileId")
        }
    }

    private fun encrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val ciphertext = cipher.doFinal(data)

        // Prepend IV to ciphertext: [IV (12 bytes)][ciphertext + GCM tag]
        return iv + ciphertext
    }

    private fun decrypt(encryptedData: ByteArray): ByteArray {
        require(encryptedData.size > GCM_IV_LENGTH) {
            "Encrypted data too short to contain IV"
        }

        val iv = encryptedData.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = encryptedData.copyOfRange(GCM_IV_LENGTH, encryptedData.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        return cipher.doFinal(ciphertext)
    }

    private fun getMemberDir(memberId: String): File {
        return File(context.filesDir, "$DOCS_DIR${File.separator}$memberId")
    }

    private fun findFile(fileId: String): File? {
        val fileName = "$fileId$FILE_EXTENSION"
        val docsRoot = File(context.filesDir, DOCS_DIR)
        if (!docsRoot.exists()) return null

        docsRoot.listFiles()?.forEach { memberDir ->
            if (memberDir.isDirectory) {
                val file = File(memberDir, fileName)
                if (file.exists()) return file
            }
        }
        return null
    }
}
