package com.exchange.model

import java.time.Instant
import java.util.UUID

enum class PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUND_PROCESSING,
    REFUNDED
}

enum class PaymentEventType {
    CREATED,
    PROCESSING_STARTED,
    COMPLETED,
    FAILED,
    REFUND_REQUESTED,
    REFUND_COMPLETED,
}

data class PaymentEvent(
    val id: String = UUID.randomUUID().toString(),
    val paymentId: String,
    val eventType: PaymentEventType,
    val fromStatus: PaymentStatus?,
    val toStatus: PaymentStatus,
    val timestamp: Instant = Instant.now(),
)

class IllegalPaymentTransition(
    paymentId: String,
    from: PaymentStatus,
    to: PaymentStatus,
) : RuntimeException("Payment $paymentId cannot transition from $from to $to")

private val transitions: Map<Pair<PaymentStatus, PaymentStatus>, PaymentEventType> = mapOf(
    (PaymentStatus.PENDING to PaymentStatus.PROCESSING) to PaymentEventType.PROCESSING_STARTED,
    (PaymentStatus.PENDING to PaymentStatus.FAILED) to PaymentEventType.FAILED,
    (PaymentStatus.PROCESSING to PaymentStatus.COMPLETED) to PaymentEventType.COMPLETED,
    (PaymentStatus.PROCESSING to PaymentStatus.FAILED) to PaymentEventType.FAILED,
    (PaymentStatus.COMPLETED to PaymentStatus.REFUND_PROCESSING) to PaymentEventType.REFUND_REQUESTED,
    (PaymentStatus.REFUND_PROCESSING to PaymentStatus.REFUNDED) to PaymentEventType.REFUND_COMPLETED,
    (PaymentStatus.REFUND_PROCESSING to PaymentStatus.FAILED) to PaymentEventType.FAILED,
)

data class Payment(
    val id: String = UUID.randomUUID().toString(),
    val quoteId: String,
    val customerReference: String,
    var status: PaymentStatus = PaymentStatus.PENDING,
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now()
) {
    fun transitionTo(newStatus: PaymentStatus): PaymentEvent {
        val eventType = transitions[status to newStatus]
            ?: throw IllegalPaymentTransition(id, status, newStatus)

        val event = PaymentEvent(
            paymentId = id,
            eventType = eventType,
            fromStatus = status,
            toStatus = newStatus,
        )
        status = newStatus
        updatedAt = event.timestamp
        return event
    }
}