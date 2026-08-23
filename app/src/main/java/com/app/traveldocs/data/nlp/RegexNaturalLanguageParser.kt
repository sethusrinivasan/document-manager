package com.app.traveldocs.data.nlp

import com.app.traveldocs.domain.model.ParseResult
import com.app.traveldocs.domain.model.QueryIntent
import com.app.traveldocs.domain.model.TravelParameters
import com.app.traveldocs.domain.repository.NaturalLanguageParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegexNaturalLanguageParser @Inject constructor() : NaturalLanguageParser {

    companion object {
        // Intent detection patterns
        private val TRAVEL_CHECKLIST_PATTERNS = listOf(
            Regex("what documents? do i need", RegexOption.IGNORE_CASE),
            Regex("\\bchecklist\\b", RegexOption.IGNORE_CASE),
            Regex("requirements? for", RegexOption.IGNORE_CASE),
            Regex("documents? (?:needed|required)", RegexOption.IGNORE_CASE)
        )

        private val MISSING_DOCUMENTS_PATTERNS = listOf(
            Regex("what am i missing", RegexOption.IGNORE_CASE),
            Regex("missing documents?", RegexOption.IGNORE_CASE),
            Regex("what(?:'s|\\s+is) missing", RegexOption.IGNORE_CASE),
            Regex("do i have everything", RegexOption.IGNORE_CASE)
        )

        private val DOCUMENT_SEARCH_PATTERNS = listOf(
            Regex("\\bfind\\b", RegexOption.IGNORE_CASE),
            Regex("\\bshow\\b", RegexOption.IGNORE_CASE),
            Regex("\\bsearch\\b", RegexOption.IGNORE_CASE)
        )

        // Travel parameter patterns
        private val FAMILY_SIZE_PATTERNS = listOf(
            Regex("family of (\\d+)", RegexOption.IGNORE_CASE),
            Regex("(\\d+) people", RegexOption.IGNORE_CASE),
            Regex("(\\d+) travelers?", RegexOption.IGNORE_CASE),
            Regex("(\\d+) persons?", RegexOption.IGNORE_CASE)
        )

        private val FAMILY_KEYWORD_PATTERN = Regex("\\bfamily\\b", RegexOption.IGNORE_CASE)

        private val DESTINATION_PATTERNS = listOf(
            Regex("(?:trip|travel(?:ing)?|go(?:ing)?|fly(?:ing)?) to ([A-Z][a-zA-Z]+(?:\\s+[A-Z][a-zA-Z]+)*)", RegexOption.IGNORE_CASE),
            Regex("to ([A-Z][a-zA-Z]+(?:\\s+[A-Z][a-zA-Z]+)*)\\b(?!\\s+(?:do|have|get|need))", RegexOption.IGNORE_CASE),
            Regex("for ([A-Z][a-zA-Z]+(?:\\s+[A-Z][a-zA-Z]+)*)\\s+(?:trip|travel|vacation|holiday)", RegexOption.IGNORE_CASE)
        )

        private val ORIGIN_PATTERNS = listOf(
            Regex("(?:living|based|reside|residing) in ([A-Z][a-zA-Z]+(?:\\s+[A-Z][a-zA-Z]+)*)", RegexOption.IGNORE_CASE),
            Regex("from ([A-Z][a-zA-Z]+(?:\\s+[A-Z][a-zA-Z]+)*)", RegexOption.IGNORE_CASE)
        )

        private val DURATION_PATTERNS = listOf(
            Regex("(\\d+)\\s*days?", RegexOption.IGNORE_CASE),
            Regex("(\\d+)\\s*weeks?", RegexOption.IGNORE_CASE),
            Regex("(\\d+)\\s*nights?", RegexOption.IGNORE_CASE),
            Regex("(\\d+)\\s*months?", RegexOption.IGNORE_CASE)
        )

        private val DURATION_KEYWORDS = mapOf(
            Regex("\\ba\\s+week\\b", RegexOption.IGNORE_CASE) to 7,
            Regex("\\bone\\s+week\\b", RegexOption.IGNORE_CASE) to 7,
            Regex("\\btwo\\s+weeks?\\b", RegexOption.IGNORE_CASE) to 14,
            Regex("\\bthree\\s+weeks?\\b", RegexOption.IGNORE_CASE) to 21,
            Regex("\\ba\\s+month\\b", RegexOption.IGNORE_CASE) to 30,
            Regex("\\bone\\s+month\\b", RegexOption.IGNORE_CASE) to 30,
            Regex("\\btwo\\s+months?\\b", RegexOption.IGNORE_CASE) to 60,
            Regex("\\bweekend\\b", RegexOption.IGNORE_CASE) to 3,
            Regex("\\bweek\\b", RegexOption.IGNORE_CASE) to 7
        )

        // Common words to exclude from search terms
        private val STOP_WORDS = setOf(
            "a", "an", "the", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "shall", "can", "need", "dare", "ought",
            "used", "to", "of", "in", "for", "on", "with", "at", "by", "from",
            "as", "into", "through", "during", "before", "after", "above", "below",
            "between", "out", "off", "over", "under", "again", "further", "then",
            "once", "here", "there", "when", "where", "why", "how", "all", "both",
            "each", "few", "more", "most", "other", "some", "such", "no", "nor",
            "not", "only", "own", "same", "so", "than", "too", "very", "just",
            "because", "but", "and", "or", "if", "while", "about", "what", "which",
            "who", "whom", "this", "that", "these", "those", "am", "i", "my", "me",
            "we", "our", "you", "your", "it", "its", "they", "their", "his", "her",
            "documents", "document", "trip", "travel", "going", "living"
        )
    }

    override fun parse(query: String): ParseResult {
        val intent = detectIntent(query)
        val travelParams = extractTravelParameters(query)
        val searchTerms = extractSearchTerms(query)

        return ParseResult(
            intent = intent,
            travelParams = travelParams,
            searchTerms = searchTerms
        )
    }

    private fun detectIntent(query: String): QueryIntent {
        // Check for TRAVEL_CHECKLIST intent first (most specific)
        if (TRAVEL_CHECKLIST_PATTERNS.any { it.containsMatchIn(query) }) {
            return QueryIntent.TRAVEL_CHECKLIST
        }

        // Check for MISSING_DOCUMENTS intent
        if (MISSING_DOCUMENTS_PATTERNS.any { it.containsMatchIn(query) }) {
            return QueryIntent.MISSING_DOCUMENTS
        }

        // Check for DOCUMENT_SEARCH intent
        if (DOCUMENT_SEARCH_PATTERNS.any { it.containsMatchIn(query) }) {
            return QueryIntent.DOCUMENT_SEARCH
        }

        // Default to DOCUMENT_SEARCH
        return QueryIntent.DOCUMENT_SEARCH
    }

    private fun extractTravelParameters(query: String): TravelParameters? {
        val familySize = extractFamilySize(query)
        val origin = extractOrigin(query)
        val destination = extractDestination(query)
        val durationDays = extractDuration(query)

        // Only return TravelParameters if at least one parameter was extracted
        if (familySize == null && origin == null && destination == null && durationDays == null) {
            return null
        }

        return TravelParameters(
            familySize = familySize,
            origin = origin,
            destination = destination,
            durationDays = durationDays,
            rawQuery = query
        )
    }

    private fun extractFamilySize(query: String): Int? {
        // Check for explicit family size patterns first
        for (pattern in FAMILY_SIZE_PATTERNS) {
            val match = pattern.find(query)
            if (match != null) {
                return match.groupValues[1].toIntOrNull()
            }
        }

        // Check for generic "family" keyword → default to 4
        if (FAMILY_KEYWORD_PATTERN.containsMatchIn(query)) {
            return 4
        }

        return null
    }

    private fun extractOrigin(query: String): String? {
        for (pattern in ORIGIN_PATTERNS) {
            val match = pattern.find(query)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        return null
    }

    private fun extractDestination(query: String): String? {
        for (pattern in DESTINATION_PATTERNS) {
            val match = pattern.find(query)
            if (match != null) {
                val destination = match.groupValues[1].trim()
                // Avoid matching common non-destination words
                if (destination.lowercase() !in STOP_WORDS) {
                    return destination
                }
            }
        }
        return null
    }

    private fun extractDuration(query: String): Int? {
        // Check keyword-based durations first (e.g., "a week", "two weeks")
        for ((pattern, days) in DURATION_KEYWORDS) {
            if (pattern.containsMatchIn(query)) {
                return days
            }
        }

        // Check numeric patterns
        for ((index, pattern) in DURATION_PATTERNS.withIndex()) {
            val match = pattern.find(query)
            if (match != null) {
                val number = match.groupValues[1].toIntOrNull() ?: continue
                return when (index) {
                    0 -> number          // days
                    1 -> number * 7      // weeks
                    2 -> number          // nights (treat as days)
                    3 -> number * 30     // months
                    else -> number
                }
            }
        }

        return null
    }

    private fun extractSearchTerms(query: String): List<String> {
        return query
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it !in STOP_WORDS }
            .distinct()
    }
}
