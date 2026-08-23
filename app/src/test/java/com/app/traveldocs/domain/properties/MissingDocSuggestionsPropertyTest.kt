package com.app.traveldocs.domain.properties

import com.app.traveldocs.data.nlp.BasicDocumentChecklistGenerator
import com.app.traveldocs.domain.model.Document
import com.app.traveldocs.domain.model.DocumentFormat
import com.app.traveldocs.domain.model.DocumentType
import com.app.traveldocs.domain.model.RequiredDocument
import com.app.traveldocs.domain.model.Tag
import com.app.traveldocs.domain.model.TravelDocumentChecklist
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag as JUnitTag
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertTrue

/**
 * Property 24: Every missing document has a non-empty actionable suggestion
 */
@DisplayName("Property 24: Missing document suggestions")
@JUnitTag("Feature: travel-document-manager, Property 24: Missing document suggestions")
class MissingDocSuggestionsPropertyTest {

    private lateinit var generator: BasicDocumentChecklistGenerator

    @BeforeEach
    fun setUp() { generator = BasicDocumentChecklistGenerator() }

    private val arbChecklist: Arb<TravelDocumentChecklist> = arbitrary {
        val types = listOf(DocumentType.PASSPORT, DocumentType.VISA, DocumentType.TICKET, DocumentType.HOTEL_BOOKING, DocumentType.HEALTH_INSURANCE).shuffled().take(Arb.int(1..5).bind())
        val docs = types.map { RequiredDocument(it, Arb.int(1..4).bind(), "Required ${it.name}") }
        TravelDocumentChecklist(docs, docs.sumOf { it.countNeeded })
    }

    @Test
    @DisplayName("Every missing document entry has a non-blank suggestion string")
    fun `every missing document has non-blank suggestion`() = runTest {
        checkAll(100, arbChecklist) { checklist ->
            val missing = generator.detectMissing(checklist, emptyList())
            for (m in missing) {
                assertTrue(m.suggestion.isNotBlank(), "Missing doc ${m.required.type} should have non-blank suggestion")
            }
        }
    }

    @Test
    @DisplayName("Suggestions are specific to document type")
    fun `suggestions are type-specific`() = runTest {
        checkAll(100, arbChecklist) { checklist ->
            val missing = generator.detectMissing(checklist, emptyList())
            for (m in missing) {
                when (m.required.type) {
                    DocumentType.PASSPORT -> assertTrue(m.suggestion.contains("passport", ignoreCase = true))
                    DocumentType.VISA -> assertTrue(m.suggestion.contains("visa", ignoreCase = true))
                    DocumentType.HEALTH_INSURANCE -> assertTrue(m.suggestion.contains("vaccination", ignoreCase = true) || m.suggestion.contains("health", ignoreCase = true))
                    else -> assertTrue(m.suggestion.isNotBlank())
                }
            }
        }
    }
}
