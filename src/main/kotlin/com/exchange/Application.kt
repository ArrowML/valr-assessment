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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private val logger = LoggerFactory.getLogger("Application")

fun main() {
    val vertx = Vertx.vertx()

    val config = AppConfig.load()

    val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    val exchangeClient = ValrClient(config.exchange.baseUrl, vertx)
    val paymentRepository = InMemoryPaymentRepository()
    val quoteRepository = InMemoryQuoteRepository()

    val paymentValidator = PaymentValidator()
    val quoteValidator = QuoteValidator(config.quotes.supportedPairs)

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
        .onSuccess { server -> logger.info("Server started on port {}", server.actualPort()) }
        .onFailure { e ->
            logger.error("Failed to start server on port {}", config.server.port, e)
            vertx.close()
        }

    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Shutting down")
        val latch = CountDownLatch(1)
        vertx.close().onComplete { latch.countDown() }
        if (!latch.await(30, TimeUnit.SECONDS)) {
            logger.warn("Shutdown timed out after 30s")
        }
    })
}
