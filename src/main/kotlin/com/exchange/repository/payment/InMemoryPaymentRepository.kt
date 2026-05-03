package com.exchange.repository.payment

import com.exchange.model.Payment
import com.exchange.model.PaymentEvent

class InMemoryPaymentRepository : PaymentRepository {
    private val payments = HashMap<String, Payment>()
    private val paymentEvents = HashMap<String, PaymentEvent>()

    override fun savePayment(payment: Payment, event: PaymentEvent) {
        payments[payment.id] = payment
        paymentEvents[event.id] = event
    }

    override fun findPayment(id: String): Payment? {
        return payments[id]
    }

    override fun updatePayment(payment: Payment, event: PaymentEvent) {
        payments[payment.id] = payment
        paymentEvents[event.id] = event
    }

    override fun getPaymentEvents(paymentId: String): List<PaymentEvent> {
        return paymentEvents.values.filter { it.paymentId == paymentId }.sortedBy { it.timestamp }
    }
}