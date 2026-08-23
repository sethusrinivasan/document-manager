package com.app.traveldocs.domain.model

sealed class SearchResult {
    data class DocumentResults(val documents: List<Document>) : SearchResult()
    data class TravelChecklist(val checklist: TravelDocumentChecklist) : SearchResult()
    data class NeedMoreInfo(val missingParams: List<String>) : SearchResult()
}
