package com.exchange.handler

import com.exchange.client.ValrClient
import com.exchange.repository.PaymentRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import com.exchange.router.PaymentRouter
import com.exchange.validation.PaymentValidator

@ExtendWith(VertxExtension::class)
class QuoteHandlerTest {

    private lateinit var client: WebClient

    @BeforeEach
    fun setUp(vertx: Vertx, testContext: VertxTestContext) {
        val valrClient = ValrClient()
        val repository = PaymentRepository()
        val validator = PaymentValidator()
        val quoteHandler = QuoteHandler(valrClient, repository)
        val paymentHandler = PaymentHandler(repository, validator)
        val router = PaymentRouter.create(vertx, quoteHandler, paymentHandler)

        client = WebClient.create(vertx)

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(0)
            .onComplete(testContext.succeeding { server ->
                client = WebClient.create(vertx)
                testContext.completeNow()
            })
    }

    @Test
    fun `should return 400 for missing currency pair`(vertx: Vertx, testContext: VertxTestContext) {
        // This test intentionally left with a basic structure
        // It connects to a real API so may be flaky — candidate should notice this
        testContext.completeNow()
    }

    @Test
    fun `should create quote for valid request`(vertx: Vertx, testContext: VertxTestContext) {
        // This test connects to VALR API — candidate should consider mocking
        testContext.completeNow()
    }
}
