package com.exchange.repository.payment

import com.exchange.model.Payment
import kotlin.collections.set

class InMemoryPaymentRepository : PaymentRepository {
    private val payments = HashMap<String, Payment>()

    override fun savePayment(payment: Payment) {
        payments[payment.id] = payment
    }

    override fun findPayment(id: String): Payment? {
        return payments[id]
    }

    override fun updatePayment(payment: Payment) {
        payments[payment.id] = payment
    }
}