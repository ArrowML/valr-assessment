package com.exchange

import com.exchange.router.PaymentRouter
import com.exchange.handler.QuoteHandler
import com.exchange.handler.PaymentHandler
import com.exchange.client.ValrClient
import com.exchange.repository.payment.InMemoryPaymentRepository
import com.exchange.repository.quote.InMemoryQuoteRepository
import com.exchange.validation.PaymentValidator
import io.vertx.core.Vertx
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Application")

fun main() {
    val vertx = Vertx.vertx()

    val exchangeClient = ValrClient()
    val paymentRepository = InMemoryPaymentRepository()
    val paymentValidator = PaymentValidator()
    val quoteRepository = InMemoryQuoteRepository()

    val quoteHandler = QuoteHandler(exchangeClient, quoteRepository)
    val paymentHandler = PaymentHandler(paymentRepository, quoteRepository, paymentValidator)

    val router = PaymentRouter.create(vertx, quoteHandler, paymentHandler)

    vertx.createHttpServer()
        .requestHandler(router)
        .listen(8080)

    logger.info("Server started on port 8080")
}
