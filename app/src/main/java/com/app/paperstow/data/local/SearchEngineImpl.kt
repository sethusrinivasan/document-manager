package com.app.paperstow.data.local

import com.app.paperstow.debug.DebugLogger
import com.app.paperstow.domain.model.Document
import com.app.paperstow.domain.model.DocumentFormat
import com.app.paperstow.domain.model.QueryIntent
import com.app.paperstow.domain.model.SearchQuery
import com.app.paperstow.domain.model.SearchResult
import com.app.paperstow.domain.repository.DocumentChecklistGenerator
import com.app.paperstow.domain.repository.DocumentRepository
import com.app.paperstow.domain.repository.NaturalLanguageParser
import com.app.paperstow.domain.repository.SearchEngine
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchEngineImpl @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val naturalLanguageParser: NaturalLanguageParser,
    private val documentChecklistGenerator: DocumentChecklistGenerator
) : SearchEngine {

    override suspend fun searchByTags(memberId: String, tags: List<String>): List<Document> {
        DebugLogger.d("Search", "searchByTags: tags=$tags, member=$memberId")
        val query = SearchQuery(tags = tags)
        val results = documentRepository.search(memberId, query)
        DebugLogger.d("Search", "searchByTags: returned ${results.size} results")
        return results
    }

    override suspend fun searchFreeForm(memberId: String, query: String): List<Document> {
        DebugLogger.d("Search", "searchFreeForm: query=$query, member=$memberId")
        val searchQuery = SearchQuery(freeText = query)
        val results = documentRepository.search(memberId, searchQuery)
        DebugLogger.d("Search", "searchFreeForm: returned ${results.size} results")
        return results
    }

    override suspend fun searchNaturalLanguage(memberId: String, query: String): SearchResult {
        DebugLogger.i("Search", "searchNaturalLanguage: query=$query, member=$memberId")
        val parseResult = naturalLanguageParser.parse(query)
        DebugLogger.d("Search", "NLP parsed: intent=${parseResult.intent}, terms=${parseResult.searchTerms}, params=${parseResult.travelParams}")

        return when (parseResult.intent) {
            QueryIntent.DOCUMENT_SEARCH -> {
                val documents = searchFreeForm(memberId, query)
                DebugLogger.i("Search", "DOCUMENT_SEARCH result: ${documents.size} documents")
                SearchResult.DocumentResults(documents)
            }

            QueryIntent.TRAVEL_CHECKLIST -> {
                val travelParams = parseResult.travelParams
                if (travelParams == null) {
                    DebugLogger.w("Search", "TRAVEL_CHECKLIST: missing params, returning NeedMoreInfo")
                    SearchResult.NeedMoreInfo(listOf("origin", "destination"))
                } else {
                    val checklist = documentChecklistGenerator.generateChecklist(travelParams)
                    DebugLogger.i("Search", "TRAVEL_CHECKLIST: generated ${checklist.requiredDocuments.size} requirements, total=${checklist.totalCount}")
                    SearchResult.TravelChecklist(checklist)
                }
            }

            QueryIntent.MISSING_DOCUMENTS -> {
                val travelParams = parseResult.travelParams
                if (travelParams == null) {
                    DebugLogger.w("Search", "MISSING_DOCUMENTS: missing params, returning NeedMoreInfo")
                    SearchResult.NeedMoreInfo(listOf("origin", "destination"))
                } else {
                    val checklist = documentChecklistGenerator.generateChecklist(travelParams)
                    val existingDocuments = documentRepository.getAll(memberId).first()
                    val missingDocs = documentChecklistGenerator.detectMissing(checklist, existingDocuments)
                    DebugLogger.i("Search", "MISSING_DOCUMENTS: ${missingDocs.size} missing out of ${checklist.requiredDocuments.size} required")
                    val missingDocResults = missingDocs.map { missing ->
                        Document(
                            id = "",
                            memberId = memberId,
                            type = missing.required.type,
                            format = DocumentFormat.PDF,
                            originalFileName = null,
                            metadata = emptyMap(),
                            tags = emptyList(),
                            createdAt = Instant.now(),
                            updatedAt = Instant.now(),
                            extractionConfidence = null,
                            requiresManualReview = false
                        )
                    }
                    SearchResult.DocumentResults(missingDocResults)
                }
            }
        }
    }
}
