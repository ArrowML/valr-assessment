package com.exchange.service

import com.exchange.model.IllegalPaymentTransition
import com.exchange.model.Payment
import com.exchange.model.PaymentEvent
import com.exchange.model.PaymentEventType
import com.exchange.model.PaymentStatus
import com.exchange.model.Quote
import com.exchange.model.QuoteStatus
import com.exchange.model.Side
import com.exchange.repository.payment.PaymentRepository
import com.exchange.repository.quote.QuoteRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.Instant

class PaymentServiceTest {

    private val paymentRepository: PaymentRepository = mock()
    private val quoteRepository: QuoteRepository = mock()

    private val service = PaymentService(paymentRepository, quoteRepository)

    private fun activeQuote(
        id: String = "quote-id",
        status: QuoteStatus = QuoteStatus.ACTIVE,
        expiresAt: Instant = Instant.now().plusSeconds(60),
    ) = Quote(
        id = id,
        currencyPair = "BTCZAR",
        price = BigDecimal("500000"),
        payAmount = BigDecimal("1000"),
        payCurrency = "ZAR",
        receiveAmount = BigDecimal("0.00197"),
        fee = BigDecimal("15"),
        side = Side.BUY,
        status = status,
        expiresAt = expiresAt,
    )

    private fun pendingPayment(quoteId: String = "quote-id") = Payment(
        quoteId = quoteId,
        customerReference = "ref-001",
    )

    @Nested
    inner class CreatePayment {

        @Test
        fun `should return a PENDING payment with correct fields`() {
            whenever(quoteRepository.findQuote("quote-id")).thenReturn(activeQuote())

            val payment = service.createPayment("quote-id", "ref-001")

            assertThat(payment.id).isNotBlank()
            assertThat(payment.quoteId).isEqualTo("quote-id")
            assertThat(payment.customerReference).isEqualTo("ref-001")
            assertThat(payment.status).isEqualTo(PaymentStatus.PENDING)
        }

        @Test
        fun `should claim the quote`() {
            val quote = activeQuote()
            whenever(quoteRepository.findQuote("quote-id")).thenReturn(quote)

            service.createPayment("quote-id", "ref-001")

            assertThat(quote.status).isEqualTo(QuoteStatus.CLAIMED)
            verify(quoteRepository).updateQuote(quote)
        }

        @Test
        fun `should save payment with a CREATED event`() {
            whenever(quoteRepository.findQuote("quote-id")).thenReturn(activeQuote())

            val payment = service.createPayment("quote-id", "ref-001")

            val eventCaptor = argumentCaptor<PaymentEvent>()
            verify(paymentRepository).savePayment(eq(payment), eventCaptor.capture())
            assertThat(eventCaptor.firstValue.eventType).isEqualTo(PaymentEventType.CREATED)
            assertThat(eventCaptor.firstValue.fromStatus).isNull()
            assertThat(eventCaptor.firstValue.toStatus).isEqualTo(PaymentStatus.PENDING)
        }

        @Test
        fun `should throw QuoteNotFoundException when quote does not exist`() {
            whenever(quoteRepository.findQuote("unknown")).thenReturn(null)

            assertThatThrownBy { service.createPayment("unknown", "ref-001") }
                .isInstanceOf(QuoteNotFoundException::class.java)
        }

        @Test
        fun `should throw InvalidQuoteState when quote is not ACTIVE`() {
            whenever(quoteRepository.findQuote("quote-id")).thenReturn(activeQuote(status = QuoteStatus.CLAIMED))

            assertThatThrownBy { service.createPayment("quote-id", "ref-001") }
                .isInstanceOf(InvalidQuoteState::class.java)
        }

        @Test
        fun `should throw InvalidQuoteState when quote is expired`() {
            whenever(quoteRepository.findQuote("quote-id")).thenReturn(activeQuote(expiresAt = Instant.now().minusSeconds(1)))

            assertThatThrownBy { service.createPayment("quote-id", "ref-001") }
                .isInstanceOf(InvalidQuoteState::class.java)
        }
    }

    @Nested
    inner class ExecutePayment {

        @Test
        fun `should transition payment to COMPLETED and marks quote COMPLETE`() {
            val payment = pendingPayment()
            whenever(paymentRepository.findPayment(payment.id)).thenReturn(payment)
            whenever(quoteRepository.findQuote(payment.quoteId)).thenReturn(activeQuote(status = QuoteStatus.CLAIMED))

            val result = service.executePayment(payment.id)

            assertThat(result.status).isEqualTo(PaymentStatus.COMPLETED)
        }

        @Test
        fun `should mark quote as COMPLETE`() {
            val payment = pendingPayment()
            val quote = activeQuote(status = QuoteStatus.CLAIMED)
            whenever(paymentRepository.findPayment(payment.id)).thenReturn(payment)
            whenever(quoteRepository.findQuote(payment.quoteId)).thenReturn(quote)

            service.executePayment(payment.id)

            assertThat(quote.status).isEqualTo(QuoteStatus.COMPLETE)
            verify(quoteRepository).updateQuote(quote)
        }

        @Test
        fun `should save PROCESSING_STARTED then COMPLETED events`() {
            val payment = pendingPayment()
            whenever(paymentRepository.findPayment(payment.id)).thenReturn(payment)
            whenever(quoteRepository.findQuote(payment.quoteId)).thenReturn(activeQuote(status = QuoteStatus.CLAIMED))

            service.executePayment(payment.id)

            val eventCaptor = argumentCaptor<PaymentEvent>()
            verify(paymentRepository, times(2)).updatePayment(eq(payment), eventCaptor.capture())
            assertThat(eventCaptor.firstValue.eventType).isEqualTo(PaymentEventType.PROCESSING_STARTED)
            assertThat(eventCaptor.secondValue.eventType).isEqualTo(PaymentEventType.COMPLETED)
        }

        @Test
        fun `should be idempotent when payment is already COMPLETED`() {
            val payment = pendingPayment().also {
                it.transitionTo(PaymentStatus.PROCESSING)
                it.transitionTo(PaymentStatus.COMPLETED)
            }
            whenever(paymentRepository.findPayment(payment.id)).thenReturn(payment)

            val result = service.executePayment(payment.id)

            assertThat(result.status).isEqualTo(PaymentStatus.COMPLETED)
            verify(paymentRepository, never()).updatePayment(any(), any())
        }

        @Test
        fun `should throw PaymentNotFoundException when payment does not exist`() {
            whenever(paymentRepository.findPayment("unknown")).thenReturn(null)

            assertThatThrownBy { service.executePayment("unknown") }
                .isInstanceOf(PaymentNotFoundException::class.java)
        }

        @Test
        fun `should throw QuoteNotFoundException when quote does not exist`() {
            val payment = pendingPayment()
            whenever(paymentRepository.findPayment(payment.id)).thenReturn(payment)
            whenever(quoteRepository.findQuote(payment.quoteId)).thenReturn(null)

            assertThatThrownBy { service.executePayment(payment.id) }
                .isInstanceOf(QuoteNotFoundException::class.java)
        }

        @Test
        fun `should throw InvalidQuoteState when quote is not CLAIMED`() {
            val payment = pendingPayment()
            whenever(paymentRepository.findPayment(payment.id)).thenReturn(payment)
            whenever(quoteRepository.findQuote(payment.quoteId)).thenReturn(activeQuote(status = QuoteStatus.ACTIVE))

            assertThatThrownBy { service.executePayment(payment.id) }
                .isInstanceOf(InvalidQuoteState::class.java)
        }

        @Test
        fun `should throw InvalidQuoteState when quote has expired`() {
            val payment = pendingPayment()
            whenever(paymentRepository.findPayment(payment.id)).thenReturn(payment)
            whenever(quoteRepository.findQuote(payment.quoteId)).thenReturn(
                activeQuote(status = QuoteStatus.CLAIMED, expiresAt = Instant.now().minusSeconds(1))
            )

            assertThatThrownBy { service.executePayment(payment.id) }
                .isInstanceOf(InvalidQuoteState::class.java)
        }
    }

    @Nested
    inner class GetPayment {

        @Test
        fun `should return payment by id`() {
            val payment = pendingPayment()
            whenever(paymentRepository.findPayment(payment.id)).thenReturn(payment)

            assertThat(service.getPayment(payment.id)).isEqualTo(payment)
        }

        @Test
        fun `should throw PaymentNotFoundException when payment does not exist`() {
            whenever(paymentRepository.findPayment("unknown")).thenReturn(null)

            assertThatThrownBy { service.getPayment("unknown") }
                .isInstanceOf(PaymentNotFoundException::class.java)
        }
    }

    @Nested
    inner class RefundPayment {

        private fun completedPayment() = pendingPayment().also {
            it.transitionTo(PaymentStatus.PROCESSING)
            it.transitionTo(PaymentStatus.COMPLETED)
        }

        @Test
        fun `should transition COMPLETED payment to REFUNDED`() {
            val payment = completedPayment()
            whenever(paymentRepository.findPayment(payment.id)).thenReturn(payment)

            val result = service.refundPayment(payment.id)

            assertThat(result.status).isEqualTo(PaymentStatus.REFUNDED)
        }

        @Test
        fun `should save REFUND_REQUESTED then REFUND_COMPLETED events`() {
            val payment = completedPayment()
            whenever(paymentRepository.findPayment(payment.id)).thenReturn(payment)

            service.refundPayment(payment.id)

            val eventCaptor = argumentCaptor<PaymentEvent>()
            verify(paymentRepository, times(2)).updatePayment(eq(payment), eventCaptor.capture())
            assertThat(eventCaptor.firstValue.eventType).isEqualTo(PaymentEventType.REFUND_REQUESTED)
            assertThat(eventCaptor.secondValue.eventType).isEqualTo(PaymentEventType.REFUND_COMPLETED)
        }

        @Test
        fun `should throw PaymentNotFoundException when payment does not exist`() {
            whenever(paymentRepository.findPayment("unknown")).thenReturn(null)

            assertThatThrownBy { service.refundPayment("unknown") }
                .isInstanceOf(PaymentNotFoundException::class.java)
        }

        @Test
        fun `should throw IllegalPaymentTransition when payment is not COMPLETED`() {
            val payment = pendingPayment()
            whenever(paymentRepository.findPayment(payment.id)).thenReturn(payment)

            assertThatThrownBy { service.refundPayment(payment.id) }
                .isInstanceOf(IllegalPaymentTransition::class.java)
        }
    }

    @Nested
    inner class GetPaymentStatus {

        @Test
        fun `should return status, history and quote details`() {
            val payment = pendingPayment()
            val quote = activeQuote()
            val events = listOf(
                PaymentEvent(
                    paymentId = payment.id,
                    eventType = PaymentEventType.CREATED,
                    fromStatus = null,
                    toStatus = PaymentStatus.PENDING,
                )
            )
            whenever(paymentRepository.findPayment(payment.id)).thenReturn(payment)
            whenever(quoteRepository.findQuote(payment.quoteId)).thenReturn(quote)
            whenever(paymentRepository.getPaymentEvents(payment.id)).thenReturn(events)

            val result = service.getPaymentStatus(payment.id)

            assertThat(result.status).isEqualTo(PaymentStatus.PENDING)
            assertThat(result.history).isEqualTo(events)
            assertThat(result.quoteDetails).isEqualTo(quote)
        }

        @Test
        fun `should throw PaymentNotFoundException when payment does not exist`() {
            whenever(paymentRepository.findPayment("unknown")).thenReturn(null)

            assertThatThrownBy { service.getPaymentStatus("unknown") }
                .isInstanceOf(PaymentNotFoundException::class.java)
        }

        @Test
        fun `should throw QuoteNotFoundException when quote does not exist`() {
            val payment = pendingPayment()
            whenever(paymentRepository.findPayment(payment.id)).thenReturn(payment)
            whenever(quoteRepository.findQuote(payment.quoteId)).thenReturn(null)

            assertThatThrownBy { service.getPaymentStatus(payment.id) }
                .isInstanceOf(QuoteNotFoundException::class.java)
        }
    }
}
