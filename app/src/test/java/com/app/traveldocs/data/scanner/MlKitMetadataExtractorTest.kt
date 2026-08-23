package com.app.traveldocs.data.scanner

import com.app.traveldocs.domain.model.DocumentType
import com.app.traveldocs.domain.model.ExtractedValue
import com.app.traveldocs.domain.model.MetadataField
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("MlKitMetadataExtractor")
class MlKitMetadataExtractorTest {

    private lateinit var extractor: MlKitMetadataExtractor

    @BeforeEach
    fun setup() {
        extractor = MlKitMetadataExtractor()
    }

    @Nested
    @DisplayName("classifyFromText")
    inner class ClassifyFromText {

        @Test
        @DisplayName("classifies text with PASSPORT keyword as PASSPORT")
        fun classifiesPassport() {
            val text = "REPUBLIC OF COUNTRY\nPASSPORT\nSurname: SMITH"
            assertEquals(DocumentType.PASSPORT, extractor.classifyFromText(text))
        }

        @Test
        @DisplayName("classifies text with PASSEPORT keyword as PASSPORT")
        fun classifiesPasseport() {
            val text = "REPUBLIQUE FRANCAISE\nPASSEPORT\nNom: DUPONT"
            assertEquals(DocumentType.PASSPORT, extractor.classifyFromText(text))
        }

        @Test
        @DisplayName("classifies text with VISA keyword as VISA")
        fun classifiesVisa() {
            val text = "ENTRY VISA\nVisa Number: AB1234567\nIssue Date: 01/01/2024"
            assertEquals(DocumentType.VISA, extractor.classifyFromText(text))
        }

        @Test
        @DisplayName("classifies text with BOOKING keyword as TICKET")
        fun classifiesBooking() {
            val text = "E-TICKET / BOOKING CONFIRMATION\nBooking Ref: ABC123"
            assertEquals(DocumentType.TICKET, extractor.classifyFromText(text))
        }

        @Test
        @DisplayName("classifies text with FLIGHT keyword as TICKET")
        fun classifiesFlight() {
            val text = "FLIGHT ITINERARY\nFlight No: AA 1234\nDate: 15/03/2024"
            assertEquals(DocumentType.TICKET, extractor.classifyFromText(text))
        }

        @Test
        @DisplayName("classifies text with TICKET keyword as TICKET")
        fun classifiesTicket() {
            val text = "ELECTRONIC TICKET RECEIPT\nPassenger: John Smith"
            assertEquals(DocumentType.TICKET, extractor.classifyFromText(text))
        }

        @Test
        @DisplayName("classifies text with BOARDING keyword as TICKET")
        fun classifiesBoarding() {
            val text = "BOARDING PASS\nGate: A15\nSeat: 12A"
            assertEquals(DocumentType.TICKET, extractor.classifyFromText(text))
        }

        @Test
        @DisplayName("classifies text with HOTEL keyword as HOTEL_BOOKING")
        fun classifiesHotel() {
            val text = "HOTEL RESERVATION CONFIRMATION\nGuest: John Smith"
            assertEquals(DocumentType.HOTEL_BOOKING, extractor.classifyFromText(text))
        }

        @Test
        @DisplayName("classifies text with RESERVATION keyword as HOTEL_BOOKING")
        fun classifiesReservation() {
            val text = "RESERVATION DETAILS\nCheck-in: 15/03/2024"
            assertEquals(DocumentType.HOTEL_BOOKING, extractor.classifyFromText(text))
        }

        @Test
        @DisplayName("classifies text with CHECK-IN keyword as HOTEL_BOOKING")
        fun classifiesCheckIn() {
            val text = "Your CHECK-IN details\nRoom: 305"
            assertEquals(DocumentType.HOTEL_BOOKING, extractor.classifyFromText(text))
        }

        @Test
        @DisplayName("classifies text with INSURANCE keyword as HEALTH_INSURANCE")
        fun classifiesInsurance() {
            val text = "TRAVEL INSURANCE CERTIFICATE\nPolicy No: TI12345"
            assertEquals(DocumentType.HEALTH_INSURANCE, extractor.classifyFromText(text))
        }

        @Test
        @DisplayName("classifies text with POLICY keyword as HEALTH_INSURANCE")
        fun classifiesPolicy() {
            val text = "POLICY DOCUMENT\nCoverage: Worldwide\nMember ID: M12345"
            assertEquals(DocumentType.HEALTH_INSURANCE, extractor.classifyFromText(text))
        }

        @Test
        @DisplayName("classifies unrecognized text as UNKNOWN")
        fun classifiesUnknown() {
            val text = "Some random text with no meaningful keywords"
            assertEquals(DocumentType.UNKNOWN, extractor.classifyFromText(text))
        }

        @Test
        @DisplayName("classifies empty text as UNKNOWN")
        fun classifiesEmptyText() {
            assertEquals(DocumentType.UNKNOWN, extractor.classifyFromText(""))
        }

        @Test
        @DisplayName("PASSPORT takes priority over VISA when both present")
        fun passportPriorityOverVisa() {
            val text = "PASSPORT\nVISA attached\nName: Smith"
            assertEquals(DocumentType.PASSPORT, extractor.classifyFromText(text))
        }

        @Test
        @DisplayName("classification is case-insensitive")
        fun caseInsensitive() {
            val text = "passport information\nsurname: smith"
            assertEquals(DocumentType.PASSPORT, extractor.classifyFromText(text))
        }
    }

    @Nested
    @DisplayName("extractPassportNumber")
    inner class ExtractPassportNumber {

        @Test
        @DisplayName("extracts passport number with label 'Passport No'")
        fun extractsWithLabel() {
            val text = "Passport No: AB1234567\nSurname: SMITH"
            val result = extractor.extractPassportNumber(text)
            assertNotNull(result)
            assertEquals("AB1234567", result!!.value)
            assertEquals(0.95f, result.confidence)
        }

        @Test
        @DisplayName("extracts passport number with label 'passport number'")
        fun extractsWithNumberLabel() {
            val text = "passport number: CD9876543"
            val result = extractor.extractPassportNumber(text)
            assertNotNull(result)
            assertEquals("CD9876543", result!!.value)
        }

        @Test
        @DisplayName("extracts passport number by pattern (letter + digits)")
        fun extractsByPattern() {
            val text = "Republic of Country\nA1234567\nSurname: JOHNSON"
            val result = extractor.extractPassportNumber(text)
            assertNotNull(result)
            assertEquals("A1234567", result!!.value)
            assertEquals(0.7f, result.confidence)
        }

        @Test
        @DisplayName("returns null when no passport number found")
        fun returnsNullWhenNotFound() {
            val text = "No passport number here just some text"
            val result = extractor.extractPassportNumber(text)
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("extractVisaNumber")
    inner class ExtractVisaNumber {

        @Test
        @DisplayName("extracts visa number with label")
        fun extractsWithLabel() {
            val text = "Visa Number: VN12345678\nIssue Date: 01/01/2024"
            val result = extractor.extractVisaNumber(text)
            assertNotNull(result)
            assertEquals("VN12345678", result!!.value)
            assertEquals(0.9f, result.confidence)
        }

        @Test
        @DisplayName("extracts visa number with 'Visa No' label")
        fun extractsWithNoLabel() {
            val text = "Visa No: AB123456"
            val result = extractor.extractVisaNumber(text)
            assertNotNull(result)
            assertEquals("AB123456", result!!.value)
        }

        @Test
        @DisplayName("returns null when no visa number found")
        fun returnsNullWhenNotFound() {
            val text = "Entry permit granted"
            val result = extractor.extractVisaNumber(text)
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("extractName")
    inner class ExtractName {

        @Test
        @DisplayName("extracts name with surname label")
        fun extractsWithSurnameLabel() {
            val text = "Surname: SMITH\nGiven Name: JOHN"
            val result = extractor.extractName(text)
            assertNotNull(result)
            assertEquals("SMITH", result!!.value)
            assertEquals(0.9f, result.confidence)
        }

        @Test
        @DisplayName("extracts name with 'Name' label")
        fun extractsWithNameLabel() {
            val text = "Name: John Williams\nDate of Birth: 01/01/1990"
            val result = extractor.extractName(text)
            assertNotNull(result)
            assertEquals("John Williams", result!!.value)
        }

        @Test
        @DisplayName("extracts name with holder label")
        fun extractsWithHolderLabel() {
            val text = "Holder: Jane Doe\nPassport No: AB123456"
            val result = extractor.extractName(text)
            assertNotNull(result)
            assertEquals("Jane Doe", result!!.value)
            assertEquals(0.75f, result.confidence)
        }

        @Test
        @DisplayName("returns null when no name found")
        fun returnsNullWhenNotFound() {
            val text = "12345 some numbers only"
            val result = extractor.extractName(text)
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("extractDate")
    inner class ExtractDate {

        @Test
        @DisplayName("extracts expiry date in DD/MM/YYYY format")
        fun extractsExpiryDateDDMMYYYY() {
            val text = "Expiry Date: 15/06/2025\nIssue Date: 01/01/2020"
            val result = extractor.extractDate(text, MlKitMetadataExtractor.EXPIRY_PATTERNS)
            assertNotNull(result)
            assertEquals("15/06/2025", result!!.value)
            assertEquals(0.9f, result.confidence)
        }

        @Test
        @DisplayName("extracts expiry date in YYYY-MM-DD format")
        fun extractsExpiryDateYYYYMMDD() {
            val text = "Valid Until: 2025-06-15"
            val result = extractor.extractDate(text, MlKitMetadataExtractor.EXPIRY_PATTERNS)
            assertNotNull(result)
            assertEquals("2025-06-15", result!!.value)
        }

        @Test
        @DisplayName("extracts issue date")
        fun extractsIssueDate() {
            val text = "Issue Date: 01/03/2020\nExpiry: 01/03/2030"
            val result = extractor.extractDate(text, MlKitMetadataExtractor.ISSUE_DATE_PATTERNS)
            assertNotNull(result)
            assertEquals("01/03/2020", result!!.value)
        }

        @Test
        @DisplayName("returns null when no date matches patterns")
        fun returnsNullWhenNoMatch() {
            val text = "No dates here just text"
            val result = extractor.extractDate(text, MlKitMetadataExtractor.EXPIRY_PATTERNS)
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("extractBookingReference")
    inner class ExtractBookingReference {

        @Test
        @DisplayName("extracts booking reference with label")
        fun extractsWithLabel() {
            val text = "Booking Ref: ABC123\nFlight: BA 456"
            val result = extractor.extractBookingReference(text)
            assertNotNull(result)
            assertEquals("ABC123", result!!.value)
            assertEquals(0.9f, result.confidence)
        }

        @Test
        @DisplayName("extracts PNR code")
        fun extractsPNR() {
            val text = "PNR: XYZABC\nPassenger: Smith"
            val result = extractor.extractBookingReference(text)
            assertNotNull(result)
            assertEquals("XYZABC", result!!.value)
            assertEquals(0.9f, result.confidence)
        }

        @Test
        @DisplayName("extracts confirmation code")
        fun extractsConfirmationCode() {
            val text = "Confirmation Code: HJ7K9M"
            val result = extractor.extractBookingReference(text)
            assertNotNull(result)
            assertEquals("HJ7K9M", result!!.value)
        }

        @Test
        @DisplayName("returns null when no booking reference found")
        fun returnsNullWhenNotFound() {
            val text = "no codes here"
            val result = extractor.extractBookingReference(text)
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("extractFlightDetails")
    inner class ExtractFlightDetails {

        @Test
        @DisplayName("extracts flight number with label")
        fun extractsWithLabel() {
            val text = "Flight No: BA 1234\nDeparture: 10:00"
            val result = extractor.extractFlightDetails(text)
            assertNotNull(result)
            assertEquals("BA 1234", result!!.value)
            assertEquals(0.9f, result.confidence)
        }

        @Test
        @DisplayName("extracts flight number by pattern")
        fun extractsByPattern() {
            val text = "Your flight AA456 departs at gate 5"
            val result = extractor.extractFlightDetails(text)
            assertNotNull(result)
            assertEquals("AA456", result!!.value)
            assertEquals(0.7f, result.confidence)
        }

        @Test
        @DisplayName("returns null when no flight details found")
        fun returnsNullWhenNotFound() {
            val text = "no flight info here"
            val result = extractor.extractFlightDetails(text)
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("extractHotelName")
    inner class ExtractHotelName {

        @Test
        @DisplayName("extracts hotel name with leading keyword")
        fun extractsWithLeadingKeyword() {
            val text = "Hotel: Grand Hyatt Singapore\nCheck-in: 15/03/2024"
            val result = extractor.extractHotelName(text)
            assertNotNull(result)
            assertEquals("Grand Hyatt Singapore", result!!.value)
            assertEquals(0.85f, result.confidence)
        }

        @Test
        @DisplayName("extracts hotel name with trailing keyword")
        fun extractsWithTrailingKeyword() {
            val text = "Marriott Hotel\nReservation confirmed"
            val result = extractor.extractHotelName(text)
            assertNotNull(result)
            assertTrue(result!!.value.contains("Marriott"))
        }

        @Test
        @DisplayName("returns null when no hotel name found")
        fun returnsNullWhenNotFound() {
            val text = "just some random text without hotel names"
            val result = extractor.extractHotelName(text)
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("extractPolicyNumber")
    inner class ExtractPolicyNumber {

        @Test
        @DisplayName("extracts policy number with label")
        fun extractsWithLabel() {
            val text = "Policy Number: TI12345678\nHolder: John Smith"
            val result = extractor.extractPolicyNumber(text)
            assertNotNull(result)
            assertEquals("TI12345678", result!!.value)
            assertEquals(0.9f, result.confidence)
        }

        @Test
        @DisplayName("extracts member ID")
        fun extractsMemberId() {
            val text = "Member ID: MEM9876543\nCoverage: Full"
            val result = extractor.extractPolicyNumber(text)
            assertNotNull(result)
            assertEquals("MEM9876543", result!!.value)
        }

        @Test
        @DisplayName("returns null when no policy number found")
        fun returnsNullWhenNotFound() {
            val text = "no policy info"
            val result = extractor.extractPolicyNumber(text)
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("extractCoveragePeriod")
    inner class ExtractCoveragePeriod {

        @Test
        @DisplayName("extracts coverage period with labeled range")
        fun extractsLabeledRange() {
            val text = "Coverage: 01/01/2024 to 31/12/2024\nPolicy: TI123"
            val result = extractor.extractCoveragePeriod(text)
            assertNotNull(result)
            assertEquals("01/01/2024 to 31/12/2024", result!!.value)
            assertEquals(0.85f, result.confidence)
        }

        @Test
        @DisplayName("extracts validity period with from-to pattern")
        fun extractsFromTo() {
            val text = "From: 15/03/2024 to 15/09/2024\nInsurer: ABC"
            val result = extractor.extractCoveragePeriod(text)
            assertNotNull(result)
            assertEquals("15/03/2024 to 15/09/2024", result!!.value)
        }

        @Test
        @DisplayName("returns null when no coverage period found")
        fun returnsNullWhenNotFound() {
            val text = "no date ranges"
            val result = extractor.extractCoveragePeriod(text)
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("extractDestination")
    inner class ExtractDestination {

        @Test
        @DisplayName("extracts destination with label")
        fun extractsWithLabel() {
            val text = "Destination: Singapore\nDeparture: New York"
            val result = extractor.extractDestination(text)
            assertNotNull(result)
            assertEquals("Singapore", result!!.value)
            assertEquals(0.85f, result.confidence)
        }

        @Test
        @DisplayName("extracts destination with 'to' keyword")
        fun extractsWithToKeyword() {
            val text = "New York to London\nFlight: BA178"
            val result = extractor.extractDestination(text)
            assertNotNull(result)
            assertEquals("London", result!!.value)
        }

        @Test
        @DisplayName("extracts destination with arrow notation")
        fun extractsWithArrow() {
            val text = "JFK → Tokyo Narita\nGate: B12"
            val result = extractor.extractDestination(text)
            assertNotNull(result)
            assertEquals("Tokyo Narita", result!!.value)
            assertEquals(0.75f, result.confidence)
        }

        @Test
        @DisplayName("returns null when no destination found")
        fun returnsNullWhenNotFound() {
            val text = "no destination info 12345"
            val result = extractor.extractDestination(text)
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("extractMetadata")
    inner class ExtractMetadata {

        @Test
        @DisplayName("extracts passport metadata fields")
        fun extractsPassportMetadata() {
            val text = """
                PASSPORT
                Surname: SMITH
                Passport No: AB1234567
                Expiry Date: 15/06/2030
            """.trimIndent()

            val result = extractor.extractMetadata(text, DocumentType.PASSPORT)

            assertTrue(result.containsKey(MetadataField.ID_NUMBER))
            assertTrue(result.containsKey(MetadataField.HOLDER_NAME))
            assertTrue(result.containsKey(MetadataField.EXPIRY_DATE))
            assertEquals("AB1234567", result[MetadataField.ID_NUMBER]?.value)
            assertEquals("SMITH", result[MetadataField.HOLDER_NAME]?.value)
            assertEquals("15/06/2030", result[MetadataField.EXPIRY_DATE]?.value)
        }

        @Test
        @DisplayName("extracts visa metadata fields")
        fun extractsVisaMetadata() {
            val text = """
                VISA
                Visa Number: VN12345678
                Issue Date: 01/03/2024
                Expiry Date: 01/03/2025
                Destination: Japan
            """.trimIndent()

            val result = extractor.extractMetadata(text, DocumentType.VISA)

            assertTrue(result.containsKey(MetadataField.VISA_NUMBER))
            assertTrue(result.containsKey(MetadataField.ISSUE_DATE))
            assertTrue(result.containsKey(MetadataField.EXPIRY_DATE))
            assertTrue(result.containsKey(MetadataField.DESTINATION))
        }

        @Test
        @DisplayName("extracts ticket metadata fields")
        fun extractsTicketMetadata() {
            val text = """
                E-TICKET
                Booking Ref: ABC123
                Flight No: BA 456
                Destination: London
            """.trimIndent()

            val result = extractor.extractMetadata(text, DocumentType.TICKET)

            assertTrue(result.containsKey(MetadataField.BOOKING_REFERENCE))
            assertTrue(result.containsKey(MetadataField.FLIGHT_DETAILS))
            assertTrue(result.containsKey(MetadataField.DESTINATION))
        }

        @Test
        @DisplayName("extracts hotel booking metadata fields")
        fun extractsHotelMetadata() {
            val text = """
                HOTEL RESERVATION
                Booking Ref: HTL789
                Hotel: Grand Plaza
                Destination: Paris
            """.trimIndent()

            val result = extractor.extractMetadata(text, DocumentType.HOTEL_BOOKING)

            assertTrue(result.containsKey(MetadataField.BOOKING_REFERENCE))
            assertTrue(result.containsKey(MetadataField.HOTEL_NAME))
            assertTrue(result.containsKey(MetadataField.DESTINATION))
        }

        @Test
        @DisplayName("extracts health insurance metadata fields")
        fun extractsInsuranceMetadata() {
            val text = """
                HEALTH INSURANCE
                Policy Number: TI12345
                Coverage: 01/01/2024 to 31/12/2024
            """.trimIndent()

            val result = extractor.extractMetadata(text, DocumentType.HEALTH_INSURANCE)

            assertTrue(result.containsKey(MetadataField.POLICY_NUMBER))
            assertTrue(result.containsKey(MetadataField.COVERAGE_PERIOD))
        }

        @Test
        @DisplayName("returns empty map for UNKNOWN document type")
        fun returnsEmptyForUnknown() {
            val result = extractor.extractMetadata("any text here", DocumentType.UNKNOWN)
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("calculateConfidence")
    inner class CalculateConfidence {

        @Test
        @DisplayName("returns 1.0 when all expected fields extracted with full confidence")
        fun fullConfidenceAllFields() {
            val metadata = mapOf(
                MetadataField.ID_NUMBER to ExtractedValue("AB123456", 1.0f),
                MetadataField.HOLDER_NAME to ExtractedValue("Smith", 1.0f),
                MetadataField.EXPIRY_DATE to ExtractedValue("01/01/2030", 1.0f)
            )
            val confidence = extractor.calculateConfidence(metadata, DocumentType.PASSPORT)
            assertEquals(1.0f, confidence, 0.01f)
        }

        @Test
        @DisplayName("returns 0.0 for empty metadata")
        fun zeroConfidenceEmptyMetadata() {
            val confidence = extractor.calculateConfidence(emptyMap(), DocumentType.PASSPORT)
            assertEquals(0.0f, confidence, 0.01f)
        }

        @Test
        @DisplayName("returns 0.0 for UNKNOWN document type")
        fun zeroConfidenceForUnknown() {
            val confidence = extractor.calculateConfidence(emptyMap(), DocumentType.UNKNOWN)
            assertEquals(0.0f, confidence, 0.01f)
        }

        @Test
        @DisplayName("partial extraction yields partial confidence")
        fun partialConfidence() {
            val metadata = mapOf(
                MetadataField.ID_NUMBER to ExtractedValue("AB123456", 0.9f)
            )
            // 1 of 3 fields = 0.333 ratio
            // avg confidence = 0.9
            // weighted = 0.6 * 0.333 + 0.4 * 0.9 = 0.2 + 0.36 = 0.56
            val confidence = extractor.calculateConfidence(metadata, DocumentType.PASSPORT)
            assertTrue(confidence > 0.4f)
            assertTrue(confidence < 0.7f)
        }

        @Test
        @DisplayName("confidence is capped at 1.0")
        fun cappedAtOne() {
            val metadata = mapOf(
                MetadataField.POLICY_NUMBER to ExtractedValue("TI123", 1.0f),
                MetadataField.COVERAGE_PERIOD to ExtractedValue("01/01/2024 to 31/12/2024", 1.0f)
            )
            val confidence = extractor.calculateConfidence(metadata, DocumentType.HEALTH_INSURANCE)
            assertTrue(confidence <= 1.0f)
        }
    }

    @Nested
    @DisplayName("getExpectedFieldCount")
    inner class GetExpectedFieldCount {

        @Test
        @DisplayName("PASSPORT expects 3 fields")
        fun passportExpectsThree() {
            assertEquals(3, extractor.getExpectedFieldCount(DocumentType.PASSPORT))
        }

        @Test
        @DisplayName("VISA expects 4 fields")
        fun visaExpectsFour() {
            assertEquals(4, extractor.getExpectedFieldCount(DocumentType.VISA))
        }

        @Test
        @DisplayName("TICKET expects 3 fields")
        fun ticketExpectsThree() {
            assertEquals(3, extractor.getExpectedFieldCount(DocumentType.TICKET))
        }

        @Test
        @DisplayName("HOTEL_BOOKING expects 3 fields")
        fun hotelExpectsThree() {
            assertEquals(3, extractor.getExpectedFieldCount(DocumentType.HOTEL_BOOKING))
        }

        @Test
        @DisplayName("HEALTH_INSURANCE expects 2 fields")
        fun insuranceExpectsTwo() {
            assertEquals(2, extractor.getExpectedFieldCount(DocumentType.HEALTH_INSURANCE))
        }

        @Test
        @DisplayName("UNKNOWN expects 0 fields")
        fun unknownExpectsZero() {
            assertEquals(0, extractor.getExpectedFieldCount(DocumentType.UNKNOWN))
        }
    }
}
