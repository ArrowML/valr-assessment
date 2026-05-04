package com.exchange.handler

import com.exchange.model.ApiResponse
import com.exchange.model.CreateQuoteRequest
import com.exchange.model.Quote
import com.exchange.router.json
import com.exchange.service.QuoteService
import com.exchange.service.ValidationException
import com.exchange.validation.QuoteValidator
import com.exchange.validation.ValidationResult
import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.ext.web.RoutingContext
import java.util.concurrent.Callable

class QuoteHandler(
    private val quoteService: QuoteService,
    private val objectMapper: ObjectMapper,
    private val validator: QuoteValidator,
) {
    fun createQuote(ctx: RoutingContext) {
        val request = try {
            objectMapper.readValue(ctx.body().asString().orEmpty(), CreateQuoteRequest::class.java)
        } catch (e: JacksonException) {
            ctx.fail(ValidationException(listOf("Malformed JSON: ${e.originalMessage}")))
            return
        }

        val validated = when (val result = validator.validate(request)) {
            is ValidationResult.Invalid -> {
                ctx.fail(ValidationException(result.errors))
                return
            }
            is ValidationResult.Valid -> result.value
        }

        val createQuote = Callable<Quote> {
            quoteService.createQuote(
                validated.currencyPair,
                validated.payAmount,
                validated.payCurrency,
                validated.side,
            )
        }

        ctx.vertx().executeBlocking(createQuote, false)
            .onSuccess { quote -> ctx.json(201, ApiResponse.ok(quote), objectMapper) }
            .onFailure { e -> ctx.fail(e) }
    }
}
