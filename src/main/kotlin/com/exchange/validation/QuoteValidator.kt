package com.exchange.validation

import com.exchange.handler.CreateQuoteRequest
import com.exchange.model.CurrencyPair
import com.exchange.model.Side
import java.math.BigDecimal

data class ValidatedCreateQuote(
    val currencyPair: String,
    val payAmount: BigDecimal,
    val side: Side,
)

class QuoteValidator(
    private val supportedPairs: Map<String, CurrencyPair>,
) {
    fun validate(request: CreateQuoteRequest): ValidationResult<ValidatedCreateQuote> {
        val errors = mutableListOf<String>()

        val currencyPair = request.currencyPair?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
        val pair = currencyPair?.let { supportedPairs[it] }
        when {
            currencyPair == null ->
                errors.add("currencyPair is required")
            pair == null ->
                errors.add("currencyPair '$currencyPair' is not supported. Supported: ${supportedPairs.keys.joinToString()}")
        }

        val payAmount = request.payAmount?.let { runCatching { BigDecimal(it) }.getOrNull() }
        when {
            request.payAmount.isNullOrBlank() ->
                errors.add("payAmount is required")
            payAmount == null ->
                errors.add("payAmount must be a valid decimal")
            payAmount <= BigDecimal.ZERO ->
                errors.add("payAmount must be positive")
        }

        val side = runCatching { Side.from(request.side) }.getOrNull()
        if (side == null) {
            errors.add("side must be one of ${Side.entries.joinToString()}")
        }

        val payCurrency = request.payCurrency?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
        if (payCurrency == null) {
            errors.add("payCurrency is required")
        }

        return if (errors.isEmpty()) {
            ValidationResult.Valid(
                ValidatedCreateQuote(
                    currencyPair = currencyPair!!,
                    payAmount = payAmount!!,
                    side = side!!,
                )
            )
        } else {
            ValidationResult.Invalid(errors)
        }
    }
}
