package com.app.traveldocs.data.nlp

import com.app.traveldocs.domain.model.Document
import com.app.traveldocs.domain.model.DocumentFormat
import com.app.traveldocs.domain.model.DocumentType
import com.app.traveldocs.domain.model.RequiredDocument
import com.app.traveldocs.domain.model.Tag
import com.app.traveldocs.domain.model.TravelDocumentChecklist
import com.app.traveldocs.domain.model.TravelParameters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class BasicDocumentChecklistGeneratorTest {

    private lateinit var generator: BasicDocumentChecklistGenerator

    @BeforeEach
    fun setUp() {
        generator = BasicDocumentChecklistGenerator()
    }

    // --- generateChecklist tests ---

    @Test
    fun `generateChecklist always includes passports scaled by family size`() {
        val params = TravelParameters(
            familySize = 4,
            origin = "US",
            destination = "Singapore",
            durationDays = 7,
            rawQuery = "trip for family of 4"
        )

        val checklist = generator.generateChecklist(params)

        val passportRequirement = checklist.requiredDocuments.find { it.type == DocumentType.PASSPORT }
        assertTrue(passportRequirement != null)
        assertEquals(4, passportRequirement!!.countNeeded)
    }

    @Test
    fun `generateChecklist adds visas for international travel scaled by family size`() {
        val params = TravelParameters(
            familySize = 3,
            origin = "US",
            destination = "Japan",
            durationDays = 7,
            rawQuery = "family trip"
        )

        val checklist = generator.generateChecklist(params)

        val visaRequirement = checklist.requiredDocuments.find { it.type == DocumentType.VISA }
        assertTrue(visaRequirement != null)
        assertEquals(3, visaRequirement!!.countNeeded)
    }

    @Test
    fun `generateChecklist does not add visas for domestic travel`() {
        val params = TravelParameters(
            familySize = 2,
            origin = "US",
            destination = "US",
            durationDays = 5,
            rawQuery = "domestic trip"
        )

        val checklist = generator.generateChecklist(params)

        val visaRequirement = checklist.requiredDocuments.find { it.type == DocumentType.VISA }
        assertTrue(visaRequirement == null)
    }

    @Test
    fun `generateChecklist adds hotel booking for trips longer than 3 days`() {
        val params = TravelParameters(
            familySize = 2,
            origin = "US",
            destination = "France",
            durationDays = 5,
            rawQuery = "5-day trip"
        )

        val checklist = generator.generateChecklist(params)

        val hotelRequirement = checklist.requiredDocuments.find { it.type == DocumentType.HOTEL_BOOKING }
        assertTrue(hotelRequirement != null)
        assertEquals(1, hotelRequirement!!.countNeeded)
    }

    @Test
    fun `generateChecklist does not add hotel booking for trips of 3 days or less`() {
        val params = TravelParameters(
            familySize = 2,
            origin = "US",
            destination = "Canada",
            durationDays = 3,
            rawQuery = "short trip"
        )

        val checklist = generator.generateChecklist(params)

        val hotelRequirement = checklist.requiredDocuments.find { it.type == DocumentType.HOTEL_BOOKING }
        assertTrue(hotelRequirement == null)
    }

    @Test
    fun `generateChecklist always includes health insurance`() {
        val params = TravelParameters(
            familySize = 1,
            origin = "US",
            destination = "UK",
            durationDays = 2,
            rawQuery = "quick trip"
        )

        val checklist = generator.generateChecklist(params)

        val healthRequirement = checklist.requiredDocuments.find { it.type == DocumentType.HEALTH_INSURANCE }
        assertTrue(healthRequirement != null)
        assertEquals(1, healthRequirement!!.countNeeded)
    }

    @Test
    fun `generateChecklist always includes tickets scaled by family size`() {
        val params = TravelParameters(
            familySize = 5,
            origin = "UK",
            destination = "Spain",
            durationDays = 7,
            rawQuery = "family vacation"
        )

        val checklist = generator.generateChecklist(params)

        val ticketRequirement = checklist.requiredDocuments.find { it.type == DocumentType.TICKET }
        assertTrue(ticketRequirement != null)
        assertEquals(5, ticketRequirement!!.countNeeded)
    }

    @Test
    fun `generateChecklist totalCount equals sum of all countNeeded`() {
        val params = TravelParameters(
            familySize = 4,
            origin = "US",
            destination = "Singapore",
            durationDays = 7,
            rawQuery = "family trip"
        )

        val checklist = generator.generateChecklist(params)

        val expectedTotal = checklist.requiredDocuments.sumOf { it.countNeeded }
        assertEquals(expectedTotal, checklist.totalCount)
    }

    @Test
    fun `generateChecklist defaults familySize to 1 when null`() {
        val params = TravelParameters(
            familySize = null,
            origin = "US",
            destination = "Mexico",
            durationDays = 5,
            rawQuery = "trip"
        )

        val checklist = generator.generateChecklist(params)

        val passportRequirement = checklist.requiredDocuments.find { it.type == DocumentType.PASSPORT }
        assertEquals(1, passportRequirement!!.countNeeded)
    }

    @Test
    fun `generateChecklist treats destination with no origin as international`() {
        val params = TravelParameters(
            familySize = 2,
            origin = null,
            destination = "Thailand",
            durationDays = 10,
            rawQuery = "going to Thailand"
        )

        val checklist = generator.generateChecklist(params)

        val visaRequirement = checklist.requiredDocuments.find { it.type == DocumentType.VISA }
        assertTrue(visaRequirement != null)
        assertEquals(2, visaRequirement!!.countNeeded)
    }

    @Test
    fun `generateChecklist with no destination does not add visa`() {
        val params = TravelParameters(
            familySize = 2,
            origin = "US",
            destination = null,
            durationDays = 5,
            rawQuery = "trip somewhere"
        )

        val checklist = generator.generateChecklist(params)

        val visaRequirement = checklist.requiredDocuments.find { it.type == DocumentType.VISA }
        assertTrue(visaRequirement == null)
    }

    // --- detectMissing tests ---

    @Test
    fun `detectMissing returns empty list when all documents are present`() {
        val checklist = TravelDocumentChecklist(
            requiredDocuments = listOf(
                RequiredDocument(DocumentType.PASSPORT, 2, "Valid passport"),
                RequiredDocument(DocumentType.HEALTH_INSURANCE, 1, "Health insurance")
            ),
            totalCount = 3
        )
        val existingDocs = listOf(
            createTestDocument(type = DocumentType.PASSPORT, id = "p1"),
            createTestDocument(type = DocumentType.PASSPORT, id = "p2"),
            createTestDocument(type = DocumentType.HEALTH_INSURANCE, id = "h1")
        )

        val missing = generator.detectMissing(checklist, existingDocs)

        assertTrue(missing.isEmpty())
    }

    @Test
    fun `detectMissing detects missing passport when count is insufficient`() {
        val checklist = TravelDocumentChecklist(
            requiredDocuments = listOf(
                RequiredDocument(DocumentType.PASSPORT, 4, "Valid passport")
            ),
            totalCount = 4
        )
        val existingDocs = listOf(
            createTestDocument(type = DocumentType.PASSPORT, id = "p1"),
            createTestDocument(type = DocumentType.PASSPORT, id = "p2")
        )

        val missing = generator.detectMissing(checklist, existingDocs)

        assertEquals(1, missing.size)
        assertEquals(DocumentType.PASSPORT, missing[0].required.type)
        assertTrue(missing[0].suggestion.contains("passport", ignoreCase = true))
    }

    @Test
    fun `detectMissing provides correct suggestion for missing visa`() {
        val checklist = TravelDocumentChecklist(
            requiredDocuments = listOf(
                RequiredDocument(DocumentType.VISA, 2, "Tourist visa")
            ),
            totalCount = 2
        )
        val existingDocs = emptyList<Document>()

        val missing = generator.detectMissing(checklist, existingDocs)

        assertEquals(1, missing.size)
        assertEquals("Apply for visa at embassy", missing[0].suggestion)
    }

    @Test
    fun `detectMissing provides correct suggestion for missing health insurance`() {
        val checklist = TravelDocumentChecklist(
            requiredDocuments = listOf(
                RequiredDocument(DocumentType.HEALTH_INSURANCE, 1, "Health insurance")
            ),
            totalCount = 1
        )
        val existingDocs = emptyList<Document>()

        val missing = generator.detectMissing(checklist, existingDocs)

        assertEquals(1, missing.size)
        assertEquals("Check vaccination requirements", missing[0].suggestion)
    }

    @Test
    fun `detectMissing provides correct suggestion for missing passport`() {
        val checklist = TravelDocumentChecklist(
            requiredDocuments = listOf(
                RequiredDocument(DocumentType.PASSPORT, 1, "Valid passport")
            ),
            totalCount = 1
        )
        val existingDocs = emptyList<Document>()

        val missing = generator.detectMissing(checklist, existingDocs)

        assertEquals(1, missing.size)
        assertEquals("Ensure passport validity is 6+ months", missing[0].suggestion)
    }

    @Test
    fun `detectMissing returns multiple missing when several document types are absent`() {
        val checklist = TravelDocumentChecklist(
            requiredDocuments = listOf(
                RequiredDocument(DocumentType.PASSPORT, 2, "Valid passport"),
                RequiredDocument(DocumentType.VISA, 2, "Tourist visa"),
                RequiredDocument(DocumentType.TICKET, 2, "Flight ticket"),
                RequiredDocument(DocumentType.HEALTH_INSURANCE, 1, "Health insurance")
            ),
            totalCount = 7
        )
        val existingDocs = listOf(
            createTestDocument(type = DocumentType.PASSPORT, id = "p1"),
            createTestDocument(type = DocumentType.PASSPORT, id = "p2")
        )

        val missing = generator.detectMissing(checklist, existingDocs)

        assertEquals(3, missing.size)
        val missingTypes = missing.map { it.required.type }
        assertTrue(DocumentType.VISA in missingTypes)
        assertTrue(DocumentType.TICKET in missingTypes)
        assertTrue(DocumentType.HEALTH_INSURANCE in missingTypes)
    }

    @Test
    fun `detectMissing does not flag document type when count meets requirement`() {
        val checklist = TravelDocumentChecklist(
            requiredDocuments = listOf(
                RequiredDocument(DocumentType.PASSPORT, 2, "Valid passport"),
                RequiredDocument(DocumentType.TICKET, 2, "Flight ticket")
            ),
            totalCount = 4
        )
        val existingDocs = listOf(
            createTestDocument(type = DocumentType.PASSPORT, id = "p1"),
            createTestDocument(type = DocumentType.PASSPORT, id = "p2"),
            createTestDocument(type = DocumentType.TICKET, id = "t1")
        )

        val missing = generator.detectMissing(checklist, existingDocs)

        assertEquals(1, missing.size)
        assertEquals(DocumentType.TICKET, missing[0].required.type)
    }

    // --- Helper ---

    private fun createTestDocument(
        id: String = "doc-1",
        type: DocumentType = DocumentType.PASSPORT
    ): Document {
        return Document(
            id = id,
            memberId = "member-1",
            type = type,
            format = DocumentFormat.PDF,
            originalFileName = "test.pdf",
            metadata = emptyMap(),
            tags = listOf(Tag("test", false)),
            createdAt = Instant.ofEpochMilli(1700000000000L),
            updatedAt = Instant.ofEpochMilli(1700000000000L),
            extractionConfidence = 0.95f,
            requiresManualReview = false
        )
    }
}
