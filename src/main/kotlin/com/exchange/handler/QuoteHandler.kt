package com.exchange.handler

import com.exchange.model.ApiResponse
import com.exchange.router.json
import com.exchange.service.QuoteService
import com.exchange.service.ValidationException
import com.exchange.validation.QuoteValidator
import com.exchange.validation.ValidationResult
import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.ext.web.RoutingContext

class QuoteHandler(
    private val quoteService: QuoteService,
    private val objectMapper: ObjectMapper,
    private val validator: QuoteValidator,
) {
    fun createQuote(ctx: RoutingContext) {
        try {
            val request = objectMapper.readValue(ctx.body().asString(), CreateQuoteRequest::class.java)

            val validated = when (val result = validator.validate(request)) {
                is ValidationResult.Invalid -> throw ValidationException(result.errors)
                is ValidationResult.Valid -> result.value
            }

            val quote = quoteService.createQuote(validated.currencyPair, validated.payAmount, validated.side)
            ctx.json(201, ApiResponse.ok(quote), objectMapper)

        } catch (e: Exception) {
            ctx.fail(e)
        }
    }
}
