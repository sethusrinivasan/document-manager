package com.app.traveldocs.data.local

object InputSanitizer {
    private val TAG_REGEX = Regex("[a-zA-Z0-9 _\\-]+")
    private val FILENAME_UNSAFE = Regex("[/\\\\:*?\"<>|]")

    fun sanitizeTag(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return ""
        return trimmed.filter { it.toString().matches(Regex("[a-zA-Z0-9 _\\-]")) }.take(50)
    }

    fun sanitizeFilename(input: String): String {
        return FILENAME_UNSAFE.replace(input.trim(), "_").take(200)
    }

    fun isValidTag(input: String): Boolean {
        return input.isNotBlank() && input.length <= 50 && input.matches(TAG_REGEX)
    }
}
