package com.exchange.repository.quote

import com.exchange.model.Quote

interface QuoteRepository {
    fun saveQuote(quote: Quote)
    fun findQuote(id: String): Quote?
}