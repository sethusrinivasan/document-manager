package com.app.traveldocs.domain.model

data class SearchQuery(
    val tags: List<String> = emptyList(),
    val freeText: String? = null,
    val naturalLanguage: String? = null
)
