package com.exchange.model

import java.time.Instant
import java.util.UUID

enum class PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}

data class Payment(
    val id: String = UUID.randomUUID().toString(),
    val quoteId: String,
    val customerReference: String,
    var status: PaymentStatus = PaymentStatus.PENDING,
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now()
)
