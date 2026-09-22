package com.app.paperstow.data.demo

import com.app.paperstow.domain.model.DocumentFormat
import com.app.paperstow.domain.model.DocumentType

const val DEMO_SYSTEM_TAG = "__DEMO"
const val DEMO_FOLDER_TAG = "Sample trip"

data class DemoAsset(
    val assetName: String,
    val fileName: String,
    val format: DocumentFormat,
    val type: DocumentType,
    val tags: List<String>
)

object DemoCatalog {
    val items: List<DemoAsset> = listOf(
        DemoAsset("john_doe_passport.pdf", "John_Doe_Passport.pdf", DocumentFormat.PDF, DocumentType.PASSPORT, listOf("Passport")),
        DemoAsset("john_doe_photo.png", "John_Doe_Photo.png", DocumentFormat.PNG, DocumentType.PASSPORT, listOf("Passport")),
        DemoAsset("jane_doe_photo.png", "Jane_Doe_Photo.png", DocumentFormat.PNG, DocumentType.PASSPORT, listOf("Passport")),
        DemoAsset("visa_letter.pdf", "Jane_Doe_Visa_Letter.pdf", DocumentFormat.PDF, DocumentType.VISA, listOf("Visa")),
        DemoAsset("visa_stamp.png", "Visa_Entry_Stamp.png", DocumentFormat.PNG, DocumentType.VISA, listOf("Visa")),
        DemoAsset("boarding_pass.pdf", "Boarding_Pass_PS101.pdf", DocumentFormat.PDF, DocumentType.TICKET, listOf("Tickets")),
        DemoAsset("boarding_pass_mobile.jpg", "Boarding_Pass_Mobile.jpg", DocumentFormat.JPG, DocumentType.TICKET, listOf("Tickets")),
        DemoAsset("hotel_confirmation.pdf", "Lakeside_Inn.pdf", DocumentFormat.PDF, DocumentType.HOTEL_BOOKING, listOf("Hotel")),
        DemoAsset("hotel_key_packet.gif", "Hotel_Key_Packet.gif", DocumentFormat.GIF, DocumentType.HOTEL_BOOKING, listOf("Hotel")),
        DemoAsset("travel_insurance.pdf", "Travel_Insurance.pdf", DocumentFormat.PDF, DocumentType.HEALTH_INSURANCE, listOf("Insurance")),
        DemoAsset("vaccination_record.pdf", "John_Doe_Vaccination.pdf", DocumentFormat.PDF, DocumentType.HEALTH_INSURANCE, listOf("Health")),
        DemoAsset("blood_test_report.pdf", "Jane_Doe_Blood_Test.pdf", DocumentFormat.PDF, DocumentType.HEALTH_INSURANCE, listOf("Health")),
        DemoAsset("jane_vaccine_card.bmp", "Jane_Doe_Vaccine_Card.bmp", DocumentFormat.BMP, DocumentType.HEALTH_INSURANCE, listOf("Health")),
        DemoAsset("emergency_contacts.txt", "Emergency_Contacts.txt", DocumentFormat.TEXT, DocumentType.UNKNOWN, listOf("Notes")),
        DemoAsset("packing_list.txt", "Packing_List.txt", DocumentFormat.TEXT, DocumentType.UNKNOWN, listOf("Notes")),
        DemoAsset("flight_itinerary.txt", "Flight_Itinerary.txt", DocumentFormat.TEXT, DocumentType.UNKNOWN, listOf("Notes", "Tickets")),
        DemoAsset("trip_plans.md", "Trip_Plans.md", DocumentFormat.MARKDOWN, DocumentType.UNKNOWN, listOf("Plans")),
        DemoAsset("sample_trail.gpx", "Sample_Trail.gpx", DocumentFormat.GPX, DocumentType.UNKNOWN, listOf("Trail"))
    )
}
