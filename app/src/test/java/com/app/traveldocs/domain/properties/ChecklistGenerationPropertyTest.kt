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
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@DisplayName("Property Test")
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
            val pn = requireNotNull(passportRequirement) { "Passport requirement should not be null" }
            assertTrue(
                pn.countNeeded >= 1,
                "Passport countNeeded should be at least 1, but was ${pn.countNeeded}"
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
            val vn = requireNotNull(visaRequirement) { "Visa requirement should not be null" }
            assertTrue(
                vn.countNeeded >= 1,
                "Visa countNeeded should be at least 1, but was ${vn.countNeeded}"
            )
        }
    }
}
