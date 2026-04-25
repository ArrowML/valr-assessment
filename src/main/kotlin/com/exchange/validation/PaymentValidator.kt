package com.exchange.validation

class PaymentValidator {

    fun validate(quoteId: String?, customerReference: String?): List<String> {
        val errors = mutableListOf<String>()

        // Incomplete validation — candidate should notice gaps here
        if (quoteId.isNullOrBlank()) {
            errors.add("quoteId is required")
        }

        if (customerReference.isNullOrBlank()) {
            errors.add("customerReference is required")
        }

        return errors
    }
}
