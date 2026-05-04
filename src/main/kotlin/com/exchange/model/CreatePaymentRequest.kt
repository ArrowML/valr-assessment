package com.exchange.model

data class CreatePaymentRequest(
    val quoteId: String? = null,
    val customerReference: String? = null,
)
