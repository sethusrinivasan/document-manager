package com.app.traveldocs.domain.model

data class ParseResult(
    val intent: QueryIntent,
    val travelParams: TravelParameters?,
    val searchTerms: List<String>
)
