package com.exchange.validation

import com.exchange.model.CreatePaymentRequest

data class ValidatedCreatePayment(
    val quoteId: String,
    val customerReference: String,
)

class PaymentValidator {

    fun validate(request: CreatePaymentRequest): ValidationResult<ValidatedCreatePayment> {
        val errors = mutableListOf<String>()

        val quoteId = request.quoteId?.trim()?.takeIf { it.isNotBlank() }
        when {
            quoteId == null ->
                errors.add("quoteId is required")
            !UuidValidation.isValidUuid(quoteId) ->
                errors.add("quoteId must be a valid UUID")
        }

        val customerReference = request.customerReference?.trim()?.takeIf { it.isNotBlank() }
        if (customerReference == null) {
            errors.add("customerReference is required")
        }

        return if (errors.isEmpty()) {
            ValidationResult.Valid(
                ValidatedCreatePayment(
                    quoteId = quoteId!!,
                    customerReference = customerReference!!,
                )
            )
        } else {
            ValidationResult.Invalid(errors)
        }
    }
}
