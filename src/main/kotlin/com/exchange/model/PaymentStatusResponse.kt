package com.exchange.model

data class PaymentStatusResponse(
    val status: PaymentStatus,
    val history: List<PaymentEvent>,
    val quoteDetails: Quote,
)