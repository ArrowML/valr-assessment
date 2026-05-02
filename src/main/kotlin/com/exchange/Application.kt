package com.exchange

import com.exchange.client.ValrClient
import com.exchange.config.AppConfig
import com.exchange.handler.PaymentHandler
import com.exchange.handler.QuoteHandler
import com.exchange.repository.payment.InMemoryPaymentRepository
import com.exchange.repository.quote.InMemoryQuoteRepository
import com.exchange.router.PaymentRouter
import com.exchange.service.PaymentService
import com.exchange.service.QuoteService
import com.exchange.validation.PaymentValidator
import com.exchange.validation.QuoteValidator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.vertx.core.Vertx
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Application")

fun main() {
    val vertx = Vertx.vertx()

    val config = AppConfig.load()

    val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    val exchangeClient = ValrClient(config.exchange.baseUrl)
    val paymentRepository = InMemoryPaymentRepository()
    val quoteRepository = InMemoryQuoteRepository()

    val paymentValidator = PaymentValidator()
    val quoteValidator = QuoteValidator(config.quotes.supportedCurrencyPairs)

    val quoteService = QuoteService(
        quoteRepository,
        exchangeClient,
        brokerageFeePercent = config.brokerage.feePercent,
        ttlSeconds = config.quotes.ttlSeconds,
    )
    val paymentService = PaymentService(paymentRepository, quoteRepository)

    val quoteHandler = QuoteHandler(quoteService, objectMapper, quoteValidator)
    val paymentHandler = PaymentHandler(paymentService, objectMapper, paymentValidator)

    val router = PaymentRouter.create(vertx, quoteHandler, paymentHandler, objectMapper)

    vertx.createHttpServer()
        .requestHandler(router)
        .listen(config.server.port)

    logger.info("Server started on port {}", config.server.port)
}
