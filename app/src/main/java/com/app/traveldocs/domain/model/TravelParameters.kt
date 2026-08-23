package com.app.traveldocs.domain.model

data class TravelParameters(
    val familySize: Int?,
    val origin: String?,
    val destination: String?,
    val durationDays: Int?,
    val rawQuery: String
)
