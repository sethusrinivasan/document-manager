package com.app.traveldocs.domain.repository

import com.app.traveldocs.domain.model.ParseResult

interface NaturalLanguageParser {
    fun parse(query: String): ParseResult
}
