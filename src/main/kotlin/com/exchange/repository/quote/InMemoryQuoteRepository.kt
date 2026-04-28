package com.exchange.repository.quote

import com.exchange.model.Quote

class InMemoryQuoteRepository : QuoteRepository {

    private val quotes = HashMap<String, Quote>()

    override fun saveQuote(quote: Quote) {
        quotes[quote.id] = quote
    }

    override fun findQuote(id: String): Quote? {
        return quotes[id]
    }
}