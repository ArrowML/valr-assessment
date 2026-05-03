package com.exchange.service

sealed class PaymentException(message: String) : RuntimeException(message)

class QuoteNotFoundException(quoteId: String) : PaymentException("Quote not found: $quoteId")
class ValidationException(val errors: List<String>) : PaymentException("Validation failed: ${errors.joinToString()}")
class PaymentNotFoundException(paymentId: String) : PaymentException("Payment not found: $paymentId")
class InvalidQuoteState(quoteId: String, reason: String) : PaymentException("Quote $quoteId is not usable: $reason")