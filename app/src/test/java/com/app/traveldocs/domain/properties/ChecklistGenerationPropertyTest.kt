package com.app.traveldocs.domain.properties

import com.app.traveldocs.data.nlp.BasicDocumentChecklistGenerator
import com.app.traveldocs.domain.model.DocumentType
import com.app.traveldocs.domain.model.TravelParameters
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Property 20: Checklist generation for valid travel parameters
 *
 * For any TravelParameters with familySize and international destination,
 * the checklist always includes passports and visas. Uses the real
 * BasicDocumentChecklistGenerator.
 *
 * **Validates: Requirements 8.6**
 */
@DisplayName("Property 20: Checklist generation")
@Tag("Feature: travel-document-manager, Property 20: Checklist generation")
class ChecklistGenerationPropertyTest {

    private lateinit var generator: BasicDocumentChecklistGenerator

    private val countries = listOf(
        "US", "UK", "Singapore", "Japan", "France", "Germany",
        "Australia", "Canada", "India", "Brazil", "Mexico", "Italy",
        "Spain", "Thailand", "China", "South Korea"
    )

    @BeforeEach
    fun setUp() {
        generator = BasicDocumentChecklistGenerator()
    }

    /**
     * Generator for TravelParameters with non-null familySize and an international
     * destination (origin != destination). This ensures we test the property that
     * international travel always includes passports and visas.
     */
    private val arbInternationalTravelParams: Arb<TravelParameters> = arbitrary {
        val familySize = Arb.int(1..10).bind()
        val origin = Arb.element(countries).bind()
        val destination = Arb.element(countries.filter { it != origin }).bind()
        val durationDays = Arb.int(1..90).bind()
        val rawQuery = Arb.string(minSize = 5, maxSize = 50).bind()

        TravelParameters(
            familySize = familySize,
            origin = origin,
            destination = destination,
            durationDays = durationDays,
            rawQuery = rawQuery
        )
    }

    /**
     * Generator for TravelParameters with origin and destination specified
     * (may be same or different) to test the general non-empty checklist property.
     */
    private val arbTravelParamsWithOriginAndDestination: Arb<TravelParameters> = arbitrary {
        val familySize = Arb.int(1..10).bind()
        val origin = Arb.element(countries).bind()
        val destination = Arb.element(countries).bind()
        val durationDays = Arb.int(1..90).bind()
        val rawQuery = Arb.string(minSize = 5, maxSize = 50).bind()

        TravelParameters(
            familySize = familySize,
            origin = origin,
            destination = destination,
            durationDays = durationDays,
            rawQuery = rawQuery
        )
    }

    @Test
    @DisplayName("For any TravelParameters with origin and destination, checklist is non-empty")
    fun `generateChecklist returns non-empty checklist for any params with origin and destination`() = runTest {
        checkAll(100, arbTravelParamsWithOriginAndDestination) { params ->
            val checklist = generator.generateChecklist(params)

            assertTrue(
                checklist.requiredDocuments.isNotEmpty(),
                "Checklist should be non-empty for params with origin='${params.origin}' " +
                    "and destination='${params.destination}', familySize=${params.familySize}"
            )
            assertTrue(
                checklist.totalCount > 0,
                "Total count should be positive for params with origin='${params.origin}' " +
                    "and destination='${params.destination}'"
            )
        }
    }

    @Test
    @DisplayName("For international travel, checklist always includes passports")
    fun `generateChecklist always includes passports for international travel`() = runTest {
        checkAll(100, arbInternationalTravelParams) { params ->
            val checklist = generator.generateChecklist(params)

            val passportRequirement = checklist.requiredDocuments.find { it.type == DocumentType.PASSPORT }

            assertTrue(
                passportRequirement != null,
                "Checklist should include passports for international travel " +
                    "(origin='${params.origin}', destination='${params.destination}')"
            )
            assertTrue(
                passportRequirement!!.countNeeded >= 1,
                "Passport countNeeded should be at least 1, " +
                    "but was ${passportRequirement.countNeeded}"
            )
        }
    }

    @Test
    @DisplayName("For international travel, checklist always includes visas")
    fun `generateChecklist always includes visas for international travel`() = runTest {
        checkAll(100, arbInternationalTravelParams) { params ->
            val checklist = generator.generateChecklist(params)

            val visaRequirement = checklist.requiredDocuments.find { it.type == DocumentType.VISA }

            assertTrue(
                visaRequirement != null,
                "Checklist should include visas for international travel " +
                    "(origin='${params.origin}', destination='${params.destination}')"
            )
            assertTrue(
                visaRequirement!!.countNeeded >= 1,
                "Visa countNeeded should be at least 1, " +
                    "but was ${visaRequirement.countNeeded}"
            )
        }
    }

    @Test
    @DisplayName("For international travel with familySize, passports and visas scale by family size")
    fun `generateChecklist scales passports and visas by familySize for international travel`() = runTest {
        checkAll(100, arbInternationalTravelParams) { params ->
            val checklist = generator.generateChecklist(params)
            val expectedFamilySize = params.familySize ?: 1

            val passportRequirement = checklist.requiredDocuments.find { it.type == DocumentType.PASSPORT }
            val visaRequirement = checklist.requiredDocuments.find { it.type == DocumentType.VISA }

            assertTrue(
                passportRequirement != null && passportRequirement.countNeeded == expectedFamilySize,
                "Passport countNeeded should equal familySize ($expectedFamilySize) " +
                    "but was ${passportRequirement?.countNeeded}"
            )
            assertTrue(
                visaRequirement != null && visaRequirement.countNeeded == expectedFamilySize,
                "Visa countNeeded should equal familySize ($expectedFamilySize) " +
                    "but was ${visaRequirement?.countNeeded}"
            )
        }
    }
}
