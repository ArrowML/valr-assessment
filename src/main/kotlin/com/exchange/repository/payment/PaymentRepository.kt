package com.exchange.repository.payment

import com.exchange.model.Payment
import com.exchange.model.PaymentEvent

interface PaymentRepository {
    fun savePayment(payment: Payment, event: PaymentEvent)
    fun findPayment(id: String): Payment?
    fun updatePayment(payment: Payment, event: PaymentEvent)
    fun getPaymentEvents(paymentId: String): List<PaymentEvent>
}