package com.exchange.service


import com.exchange.model.Payment
import com.exchange.model.PaymentEvent
import com.exchange.model.PaymentEventType
import com.exchange.model.PaymentStatus
import com.exchange.model.PaymentStatusResponse
import com.exchange.model.QuoteStatus
import com.exchange.repository.payment.PaymentRepository
import com.exchange.repository.quote.QuoteRepository
import org.slf4j.LoggerFactory
import java.time.Instant

class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val quoteRepository: QuoteRepository,
) {
    private val logger = LoggerFactory.getLogger(PaymentService::class.java)

    fun createPayment(quoteId: String, customerReference: String): Payment {
        val quote = quoteRepository.findQuote(quoteId)
            ?: throw QuoteNotFoundException(quoteId)

        if (quote.status != QuoteStatus.ACTIVE) {
            throw InvalidQuoteState(quoteId, "quote is ${quote.status}")
        }
        if (Instant.now().isAfter(quote.expiresAt)) {
            throw InvalidQuoteState(quoteId, "quote expired at ${quote.expiresAt}")
        }

        quote.status = QuoteStatus.CLAIMED
        quoteRepository.updateQuote(quote)

        val payment = Payment(
            quoteId = quoteId,
            customerReference = customerReference
        )
        val createdEvent = PaymentEvent(
            paymentId = payment.id,
            eventType = PaymentEventType.CREATED,
            fromStatus = null,
            toStatus = payment.status,
        )
        paymentRepository.savePayment(payment, createdEvent)

        logger.info("Created payment {} for quote {}", payment.id, quoteId)
        return payment
    }

    fun executePayment(paymentId: String): Payment {
        val payment = paymentRepository.findPayment(paymentId)
            ?: throw PaymentNotFoundException(paymentId)

        if (payment.status == PaymentStatus.COMPLETED) return payment

        val quote = quoteRepository.findQuote(payment.quoteId)
            ?: throw QuoteNotFoundException(payment.quoteId)

        if (quote.status != QuoteStatus.CLAIMED) {
            val processingEvent = payment.transitionTo(PaymentStatus.FAILED)
            paymentRepository.updatePayment(payment, processingEvent)

            throw InvalidQuoteState(quote.id, "quote is ${quote.status}")
        }

        if (Instant.now().isAfter(quote.expiresAt)) {
            val processingEvent = payment.transitionTo(PaymentStatus.FAILED)
            paymentRepository.updatePayment(payment, processingEvent)

            throw InvalidQuoteState(quote.id, "quote expired at ${quote.expiresAt}")
        }

        val processingEvent = payment.transitionTo(PaymentStatus.PROCESSING)
        paymentRepository.updatePayment(payment, processingEvent)

        // Simulate async processing — in real life this would be an async operation
        val completedEvent = payment.transitionTo(PaymentStatus.COMPLETED)
        paymentRepository.updatePayment(payment, completedEvent)

        quote.status = QuoteStatus.COMPLETE
        quoteRepository.updateQuote(quote)

        logger.info("Executed payment {}", paymentId)
        return payment
    }

    fun getPayment(paymentId: String): Payment {
        return paymentRepository.findPayment(paymentId)
            ?: throw PaymentNotFoundException(paymentId)
    }

    fun refundPayment(paymentId: String): Payment {
        val payment = paymentRepository.findPayment(paymentId)
            ?: throw PaymentNotFoundException(paymentId)

        val processingEvent = payment.transitionTo(PaymentStatus.REFUND_PROCESSING)
        paymentRepository.updatePayment(payment, processingEvent)

        // Simulate async processing — in real life this would be an async operation
        // It would probably have to calculate the slippage and check balances.
        val completedEvent = payment.transitionTo(PaymentStatus.REFUNDED)
        paymentRepository.updatePayment(payment, completedEvent)

        logger.info("Refunded payment {}", paymentId)

        return payment
    }

    fun getPaymentStatus(paymentId: String): PaymentStatusResponse {
        val payment = paymentRepository.findPayment(paymentId)
            ?: throw PaymentNotFoundException(paymentId)

        val quote = quoteRepository.findQuote(payment.quoteId)
            ?: throw QuoteNotFoundException(payment.quoteId)

        val paymentEvents = paymentRepository.getPaymentEvents(payment.id)

        return PaymentStatusResponse(
            status = payment.status,
            history = paymentEvents,
            quoteDetails = quote,
        )
    }
}