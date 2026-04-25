package com.exchange.repository

import com.exchange.model.Payment
import com.exchange.model.Quote

/**
 * In-memory repository for payments and quotes.
 *
 */
class PaymentRepository {

    private val quotes = HashMap<String, Quote>()
    private val payments = HashMap<String, Payment>()

    fun saveQuote(quote: Quote) {
        quotes[quote.id] = quote
    }

    fun findQuote(id: String): Quote? {
        return quotes[id]
    }

    fun savePayment(payment: Payment) {
        payments[payment.id] = payment
    }

    fun findPayment(id: String): Payment? {
        return payments[id]
    }

    fun updatePayment(payment: Payment) {
        payments[payment.id] = payment
    }
}
