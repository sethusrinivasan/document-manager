package com.app.traveldocs.domain.properties

import com.app.traveldocs.data.nlp.BasicDocumentChecklistGenerator
import com.app.traveldocs.domain.model.DocumentType
import com.app.traveldocs.domain.model.TravelParameters
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Property 21: Family size scales per-person document requirements
 *
 * For any TravelParameters with familySize = N, per-person documents in the
 * generated checklist (e.g., passports, tickets) should have countNeeded = N.
 *
 * **Validates: Requirements 8.7**
 */
@DisplayName("Property 21: Family size scaling")
@Tag("Feature: travel-document-manager, Property 21: Family size scaling")
class FamilySizeScalingPropertyTest {

    private lateinit var generator: BasicDocumentChecklistGenerator

    private val countries = listOf(
        "US", "UK", "Singapore", "Japan", "France", "Germany",
        "Australia", "Canada", "India", "Brazil", "Mexico", "Italy"
    )

    @BeforeEach
    fun setUp() {
        generator = BasicDocumentChecklistGenerator()
    }

    // Generator for random family sizes 1-10
    private val arbFamilySize: Arb<Int> = Arb.int(1..10)

    // Generator for TravelParameters with a specific family size and valid origin/destination
    private val arbTravelParamsWithFamilySize: Arb<Pair<Int, TravelParameters>> = arbitrary {
        val familySize = arbFamilySize.bind()
        val origin = Arb.element(countries).bind()
        val destination = Arb.element(countries.filter { it != origin }).bind()
        val duration = Arb.int(1..30).bind()

        val params = TravelParameters(
            familySize = familySize,
            origin = origin,
            destination = destination,
            durationDays = duration,
            rawQuery = "family of $familySize from $origin to $destination"
        )
        Pair(familySize, params)
    }

    @Test
    @DisplayName("Passport count in checklist equals family size N for any N in 1..10")
    fun `passport count equals family size`() = runTest {
        checkAll(100, arbTravelParamsWithFamilySize) { (familySize, params) ->
            val checklist = generator.generateChecklist(params)

            val passportRequirement = checklist.requiredDocuments.find { it.type == DocumentType.PASSPORT }

            assertTrue(
                passportRequirement != null,
                "Checklist should always contain a passport requirement. " +
                    "Family size: $familySize, Params: $params"
            )

            assertEquals(
                familySize,
                passportRequirement.countNeeded,
                "Passport countNeeded should equal family size $familySize, " +
                    "but got ${passportRequirement.countNeeded}. Params: $params"
            )
        }
    }

    @Test
    @DisplayName("Ticket count in checklist equals family size N for any N in 1..10")
    fun `ticket count equals family size`() = runTest {
        checkAll(100, arbTravelParamsWithFamilySize) { (familySize, params) ->
            val checklist = generator.generateChecklist(params)

            val ticketRequirement = checklist.requiredDocuments.find { it.type == DocumentType.TICKET }

            assertTrue(
                ticketRequirement != null,
                "Checklist should always contain a ticket requirement. " +
                    "Family size: $familySize, Params: $params"
            )

            assertEquals(
                familySize,
                ticketRequirement.countNeeded,
                "Ticket countNeeded should equal family size $familySize, " +
                    "but got ${ticketRequirement.countNeeded}. Params: $params"
            )
        }
    }

    @Test
    @DisplayName("All per-person documents scale with family size N")
    fun `all per-person documents scale with family size`() = runTest {
        checkAll(100, arbTravelParamsWithFamilySize) { (familySize, params) ->
            val checklist = generator.generateChecklist(params)

            // Per-person document types: PASSPORT, TICKET, and VISA (for international travel)
            val perPersonTypes = setOf(DocumentType.PASSPORT, DocumentType.TICKET, DocumentType.VISA)

            val perPersonDocuments = checklist.requiredDocuments.filter { it.type in perPersonTypes }

            assertTrue(
                perPersonDocuments.isNotEmpty(),
                "Checklist should contain at least one per-person document type. " +
                    "Family size: $familySize, Params: $params"
            )

            for (doc in perPersonDocuments) {
                assertEquals(
                    familySize,
                    doc.countNeeded,
                    "Per-person document ${doc.type} should have countNeeded=$familySize, " +
                        "but got ${doc.countNeeded}. Params: $params"
                )
            }
        }
    }

    @Test
    @DisplayName("Non-per-person documents do not scale with family size")
    fun `non-per-person documents have fixed count regardless of family size`() = runTest {
        checkAll(100, arbTravelParamsWithFamilySize) { (familySize, params) ->
            val checklist = generator.generateChecklist(params)

            // Non-per-person document types: HOTEL_BOOKING, HEALTH_INSURANCE
            val fixedTypes = setOf(DocumentType.HOTEL_BOOKING, DocumentType.HEALTH_INSURANCE)

            val fixedDocuments = checklist.requiredDocuments.filter { it.type in fixedTypes }

            for (doc in fixedDocuments) {
                assertEquals(
                    1,
                    doc.countNeeded,
                    "Non-per-person document ${doc.type} should have countNeeded=1, " +
                        "but got ${doc.countNeeded}. Family size: $familySize, Params: $params"
                )
            }
        }
    }
}
