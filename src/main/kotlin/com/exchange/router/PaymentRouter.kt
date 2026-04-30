package com.exchange.router

import com.exchange.handler.PaymentHandler
import com.exchange.handler.QuoteHandler
import com.exchange.model.ApiResponse
import com.exchange.service.InvalidPaymentStatus
import com.exchange.service.PaymentNotFoundException
import com.exchange.service.QuoteNotFoundException
import com.exchange.service.ValidationException
import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import org.slf4j.LoggerFactory

object PaymentRouter {
    private val logger = LoggerFactory.getLogger(PaymentRouter::class.java)

    fun create(
        vertx: Vertx,
        quoteHandler: QuoteHandler,
        paymentHandler: PaymentHandler,
        objectMapper: ObjectMapper,
    ): Router {
        val router = Router.router(vertx)

        router.route().handler(BodyHandler.create())

        router.post("/quotes").handler { ctx -> quoteHandler.createQuote(ctx) }

        router.post("/payments").handler { ctx -> paymentHandler.createPayment(ctx) }
        router.post("/payments/:id/execute").handler { ctx -> paymentHandler.executePayment(ctx) }
        router.get("/payments/:id").handler { ctx -> paymentHandler.getPayment(ctx) }

        // TODO: POST /payments/:id/refund — candidate implements this
        router.get("/payments/:id/status").handler { ctx -> paymentHandler.getPaymentStatus(ctx) }

        router.route().failureHandler { ctx -> handleFailure(ctx, objectMapper) }

        return router
    }

    private fun handleFailure(ctx: RoutingContext, objectMapper: ObjectMapper) {
        val failure = ctx.failure()
        logger.error("Request failed: ${ctx.request().method()} ${ctx.request().path()}", failure)

        val (status, body) = when (failure) {
            is QuoteNotFoundException ->
                404 to ApiResponse.error<Nothing>(failure.message ?: "Quote not found")
            is PaymentNotFoundException ->
                404 to ApiResponse.error<Nothing>(failure.message ?: "Payment not found")
            is ValidationException ->
                400 to ApiResponse.error<Nothing>(failure.errors.joinToString(", "))
            is InvalidPaymentStatus ->
                422 to ApiResponse.error<Nothing>(failure.message ?: "Payment already in progress")
            else ->
                500 to ApiResponse.error<Nothing>("Internal server error")
        }

        ctx.json(status, body, objectMapper)
    }
}