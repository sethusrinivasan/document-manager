package com.app.paperstow.domain.model

data class TravelParameters(
    val familySize: Int?,
    val origin: String?,
    val destination: String?,
    val durationDays: Int?,
    val rawQuery: String
)
