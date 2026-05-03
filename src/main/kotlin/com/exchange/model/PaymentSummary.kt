package com.exchange.model

data class PaymentSummary(
    val status: PaymentStatus,
    val history: List<PaymentEvent>,
    val quoteDetails: Quote,
)