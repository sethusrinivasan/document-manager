package com.app.paperstow.domain.repository

import com.app.paperstow.domain.model.ParseResult

interface NaturalLanguageParser {
    fun parse(query: String): ParseResult
}
