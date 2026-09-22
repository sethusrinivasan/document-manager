package com.app.paperstow.domain.model

data class ParseResult(
    val intent: QueryIntent,
    val travelParams: TravelParameters?,
    val searchTerms: List<String>
)
