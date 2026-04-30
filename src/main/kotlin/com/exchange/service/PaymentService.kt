package com.exchange.service


import com.exchange.model.Payment
import com.exchange.model.PaymentStatus
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

        // TODO: Validated quote state

        val payment = Payment(
            quoteId = quoteId,
            customerReference = customerReference
        )
        paymentRepository.savePayment(payment)

        logger.info("Created payment {} for quote {}", payment.id, quoteId)
        return payment
    }

    fun executePayment(paymentId: String): Payment {
        val payment = paymentRepository.findPayment(paymentId)
            ?: throw PaymentNotFoundException(paymentId)

        when (payment.status) {
            PaymentStatus.COMPLETED -> return payment
            PaymentStatus.PENDING -> {
                payment.status = PaymentStatus.PROCESSING
                payment.updatedAt = Instant.now()
                paymentRepository.updatePayment(payment)

                // Simulate async processing — in real life this would be an async operation
                payment.status = PaymentStatus.COMPLETED
                payment.updatedAt = Instant.now()
                paymentRepository.updatePayment(payment)

                logger.info("Executed payment {}", paymentId)

                return payment
            }
            PaymentStatus.PROCESSING,
            PaymentStatus.FAILED,
            PaymentStatus.REFUND_PROCESSING,
            PaymentStatus.REFUNDED -> throw InvalidPaymentStatus(payment.id)
        }
    }

    fun getPayment(paymentId: String): Payment {
        return paymentRepository.findPayment(paymentId)
            ?: throw PaymentNotFoundException(paymentId)
    }
}