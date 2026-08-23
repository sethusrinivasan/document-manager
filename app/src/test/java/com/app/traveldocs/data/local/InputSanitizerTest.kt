package com.app.traveldocs.data.local

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class InputSanitizerTest {

    @Test
    fun `sanitizeTag removes unsafe characters`() {
        assertEquals("hello world", InputSanitizer.sanitizeTag("hello world"))
        assertEquals("travel-docs", InputSanitizer.sanitizeTag("travel-docs"))
        assertEquals("my_tag", InputSanitizer.sanitizeTag("my_tag"))
    }

    @Test
    fun `sanitizeTag strips path traversal`() {
        val result = InputSanitizer.sanitizeTag("../../../etc/passwd")
        assertFalse(result.contains("/"))
        assertFalse(result.contains(".."))
    }

    @Test
    fun `sanitizeTag truncates to 50 chars`() {
        assertTrue(InputSanitizer.sanitizeTag("a".repeat(100)).length <= 50)
    }

    @Test
    fun `sanitizeTag handles blank`() {
        assertEquals("", InputSanitizer.sanitizeTag(""))
        assertEquals("", InputSanitizer.sanitizeTag("   "))
    }

    @Test
    fun `sanitizeFilename removes dangerous chars`() {
        assertEquals("my_file.pdf", InputSanitizer.sanitizeFilename("my_file.pdf"))
        assertFalse(InputSanitizer.sanitizeFilename("../../etc").contains(".."))
    }

    @Test
    fun `sanitizeFilename truncates to 200`() {
        assertTrue(InputSanitizer.sanitizeFilename("x".repeat(300)).length <= 200)
    }

    @Test
    fun `isValidTag accepts normal tags`() {
        assertTrue(InputSanitizer.isValidTag("passport"))
        assertTrue(InputSanitizer.isValidTag("travel-2025"))
    }

    @Test
    fun `isValidTag rejects invalid`() {
        assertFalse(InputSanitizer.isValidTag(""))
        assertFalse(InputSanitizer.isValidTag("a".repeat(51)))
        assertFalse(InputSanitizer.isValidTag("has/slash"))
    }
}
