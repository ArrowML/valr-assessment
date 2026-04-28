package com.exchange.repository.payment

import com.exchange.model.Payment

interface PaymentRepository {
    fun savePayment(payment: Payment)
    fun findPayment(id: String): Payment?
    fun updatePayment(payment: Payment)
}