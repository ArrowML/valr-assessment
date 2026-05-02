package com.exchange.handler

data class CreatePaymentRequest(
    val quoteId: String? = null,
    val customerReference: String? = null,
)
