package com.exchange.repository.quote

import com.exchange.model.Quote
import kotlin.collections.set

class InMemoryQuoteRepository : QuoteRepository {

    private val quotes = HashMap<String, Quote>()

    override fun saveQuote(quote: Quote) {
        quotes[quote.id] = quote
    }

    override fun findQuote(id: String): Quote? {
        return quotes[id]
    }

    override fun updateQuote(quote: Quote) {
        quotes[quote.id] = quote
    }

}