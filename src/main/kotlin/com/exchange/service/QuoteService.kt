package com.exchange.service

import com.exchange.client.ExchangeClient
import com.exchange.repository.quote.QuoteRepository

class QuoteService (
    private val quoteRepository: QuoteRepository,
    private val exchangeClient: ExchangeClient
) {}