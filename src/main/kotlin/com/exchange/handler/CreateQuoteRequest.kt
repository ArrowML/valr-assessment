package com.exchange.handler

data class CreateQuoteRequest(
    val currencyPair: String? = null,
    val payAmount: String? = null,
    val payCurrency: String? = null,
    val side: String? = null,
)
