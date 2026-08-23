package com.app.traveldocs.data.tags

import com.app.traveldocs.domain.model.DocumentType
import com.app.traveldocs.domain.model.ExtractedValue
import com.app.traveldocs.domain.model.MetadataField
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AutoTagGeneratorImplTest {

    private lateinit var generator: AutoTagGeneratorImpl

    @BeforeEach
    fun setUp() {
        generator = AutoTagGeneratorImpl()
    }

    @Test
    fun `PASSPORT type generates passport tag`() {
        val tags = generator.generateTags(DocumentType.PASSPORT, emptyMap())
        assertTrue(tags.contains("passport"))
    }

    @Test
    fun `VISA type generates visa tag`() {
        val tags = generator.generateTags(DocumentType.VISA, emptyMap())
        assertTrue(tags.contains("visa"))
    }

    @Test
    fun `TICKET type generates ticket tag`() {
        val tags = generator.generateTags(DocumentType.TICKET, emptyMap())
        assertTrue(tags.contains("ticket"))
    }

    @Test
    fun `HOTEL_BOOKING type generates accommodation tag`() {
        val tags = generator.generateTags(DocumentType.HOTEL_BOOKING, emptyMap())
        assertTrue(tags.contains("accommodation"))
    }

    @Test
    fun `HEALTH_INSURANCE type generates health tag`() {
        val tags = generator.generateTags(DocumentType.HEALTH_INSURANCE, emptyMap())
        assertTrue(tags.contains("health"))
    }

    @Test
    fun `UNKNOWN type generates no type tag`() {
        val tags = generator.generateTags(DocumentType.UNKNOWN, emptyMap())
        assertTrue(tags.isEmpty())
    }

    @Test
    fun `destination metadata generates lowercase destination tag`() {
        val metadata = mapOf(
            MetadataField.DESTINATION to ExtractedValue("Singapore", 0.95f)
        )
        val tags = generator.generateTags(DocumentType.UNKNOWN, metadata)
        assertTrue(tags.contains("singapore"))
    }

    @Test
    fun `destination with whitespace is trimmed and lowercased`() {
        val metadata = mapOf(
            MetadataField.DESTINATION to ExtractedValue("  Japan  ", 0.9f)
        )
        val tags = generator.generateTags(DocumentType.UNKNOWN, metadata)
        assertTrue(tags.contains("japan"))
    }

    @Test
    fun `blank destination does not generate tag`() {
        val metadata = mapOf(
            MetadataField.DESTINATION to ExtractedValue("   ", 0.5f)
        )
        val tags = generator.generateTags(DocumentType.UNKNOWN, metadata)
        assertTrue(tags.isEmpty())
    }

    @Test
    fun `expiry date generates expires-year tag`() {
        val metadata = mapOf(
            MetadataField.EXPIRY_DATE to ExtractedValue("2025-06-15", 0.9f)
        )
        val tags = generator.generateTags(DocumentType.UNKNOWN, metadata)
        assertTrue(tags.contains("expires-2025"))
    }

    @Test
    fun `issue date generates issued-year tag`() {
        val metadata = mapOf(
            MetadataField.ISSUE_DATE to ExtractedValue("2024-01-10", 0.9f)
        )
        val tags = generator.generateTags(DocumentType.UNKNOWN, metadata)
        assertTrue(tags.contains("issued-2024"))
    }

    @Test
    fun `both date fields generate both tags`() {
        val metadata = mapOf(
            MetadataField.EXPIRY_DATE to ExtractedValue("15/03/2026", 0.9f),
            MetadataField.ISSUE_DATE to ExtractedValue("10/03/2021", 0.9f)
        )
        val tags = generator.generateTags(DocumentType.UNKNOWN, metadata)
        assertTrue(tags.contains("expires-2026"))
        assertTrue(tags.contains("issued-2021"))
    }

    @Test
    fun `full metadata generates type, destination, and date tags`() {
        val metadata = mapOf(
            MetadataField.DESTINATION to ExtractedValue("France", 0.95f),
            MetadataField.EXPIRY_DATE to ExtractedValue("2025-12-31", 0.9f),
            MetadataField.ISSUE_DATE to ExtractedValue("2020-01-01", 0.85f)
        )
        val tags = generator.generateTags(DocumentType.PASSPORT, metadata)
        assertEquals(4, tags.size)
        assertTrue(tags.contains("passport"))
        assertTrue(tags.contains("france"))
        assertTrue(tags.contains("expires-2025"))
        assertTrue(tags.contains("issued-2020"))
    }

    @Test
    fun `date with no 4-digit year generates no date tag`() {
        val metadata = mapOf(
            MetadataField.EXPIRY_DATE to ExtractedValue("June 25", 0.5f)
        )
        val tags = generator.generateTags(DocumentType.UNKNOWN, metadata)
        assertTrue(tags.isEmpty())
    }

    @Test
    fun `empty metadata with known type returns only type tag`() {
        val tags = generator.generateTags(DocumentType.VISA, emptyMap())
        assertEquals(listOf("visa"), tags)
    }
}
