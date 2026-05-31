package com.exchange.repository.quote

import com.exchange.model.Quote
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

class InMemoryQuoteRepository : QuoteRepository {

    private val quotes = ConcurrentHashMap<String, Quote>()

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