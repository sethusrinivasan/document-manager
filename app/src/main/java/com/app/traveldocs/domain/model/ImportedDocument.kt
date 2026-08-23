package com.app.traveldocs.domain.model

data class ImportedDocument(
    val rawBytes: ByteArray,
    val format: DocumentFormat,
    val originalFileName: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImportedDocument

        if (!rawBytes.contentEquals(other.rawBytes)) return false
        if (format != other.format) return false
        if (originalFileName != other.originalFileName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rawBytes.contentHashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + (originalFileName?.hashCode() ?: 0)
        return result
    }
}
