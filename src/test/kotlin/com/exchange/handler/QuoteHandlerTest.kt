package com.exchange.handler

import com.exchange.client.ExchangeClient
import com.exchange.model.CurrencyPair
import com.exchange.repository.quote.InMemoryQuoteRepository
import com.exchange.router.PaymentRouter
import com.exchange.service.QuoteService
import com.exchange.validation.QuoteValidator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal

@ExtendWith(VertxExtension::class)
class QuoteHandlerTest {
    private lateinit var client: WebClient
    private var port: Int = 0
    private val exchangeClient: ExchangeClient = mock()
    private val paymentHandler: PaymentHandler = mock()

    @BeforeEach
    fun setUp(vertx: Vertx, testContext: VertxTestContext) {

        reset(exchangeClient)

        val objectMapper = ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        val quoteRepository = InMemoryQuoteRepository()

        val quoteValidator = QuoteValidator(
            supportedPairs = mapOf("BTCZAR" to CurrencyPair("BTC", "ZAR")),
        )

        val quoteService = QuoteService(
            quoteRepository = quoteRepository,
            exchangeClient = exchangeClient,
            brokerageFeePercent = BigDecimal("0.015"),
            ttlSeconds = 30,
        )

        val quoteHandler = QuoteHandler(quoteService, objectMapper, quoteValidator)

        val router = PaymentRouter.create(vertx, quoteHandler, paymentHandler, objectMapper)

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(0)
            .onComplete(testContext.succeeding { server ->
                port = server.actualPort()
                client = WebClient.create(vertx)
                testContext.completeNow()
            })
    }

    @Test
    fun `should return 400 for validation failure`(testContext: VertxTestContext) {
        val body = JsonObject()
            .put("payAmount", "1000")
            .put("payCurrency", "ZAR")
            .put("side", "BUY")

        client.post(port, "localhost", "/quotes")
            .sendJsonObject(body)
            .onComplete(testContext.succeeding { response ->
                testContext.verify {
                    assertThat(response.statusCode()).isEqualTo(400)
                    assertThat(response.bodyAsJsonObject().getString("error"))
                        .contains("currencyPair is required")
                }
                testContext.completeNow()
            })
    }

    @Test
    fun `should create quote for valid buy request`(vertx: Vertx, testContext: VertxTestContext) {
        val body = JsonObject()
            .put("payAmount", "1000")
            .put("currencyPair", "BTCZAR")
            .put("payCurrency", "ZAR")
            .put("side", "BUY")

        whenever(exchangeClient.getMarketPrice("BTCZAR")).thenReturn(BigDecimal("1000000"))

        client.post(port, "localhost", "/quotes")
            .sendJsonObject(body)
            .onComplete(testContext.succeeding { response ->
                testContext.verify {
                    verify(exchangeClient).getMarketPrice("BTCZAR")
                    assertThat(response.statusCode()).isEqualTo(201)

                    val json = response.bodyAsJsonObject()
                    assertThat(json.getBoolean("success")).isTrue()

                    val data = json.getJsonObject("data")
                    assertThat(data.getString("id")).isNotBlank()
                    assertThat(data.getString("currencyPair")).isEqualTo("BTCZAR")
                    assertThat(data.getString("payCurrency")).isEqualTo("ZAR")
                    assertThat(data.getString("side")).isEqualTo("BUY")
                    assertThat(data.getString("status")).isEqualTo("ACTIVE")
                    assertThat(BigDecimal(data.getValue("payAmount").toString())).isEqualByComparingTo("1000")
                    assertThat(BigDecimal(data.getValue("price").toString())).isEqualByComparingTo("1000000")
                    assertThat(BigDecimal(data.getValue("fee").toString())).isEqualByComparingTo("15")
                    assertThat(BigDecimal(data.getValue("receiveAmount").toString())).isEqualByComparingTo("0.000985")

                    verify(exchangeClient).getMarketPrice("BTCZAR")
                }
                testContext.completeNow()
            })
    }

    @Test
    fun `should return 500 for exchange failure`(testContext: VertxTestContext) {
        val body = JsonObject()
            .put("payAmount", "1000")
            .put("currencyPair", "BTCZAR")
            .put("payCurrency", "ZAR")
            .put("side", "BUY")

        whenever(exchangeClient.getMarketPrice("BTCZAR")).thenThrow(RuntimeException())

        client.post(port, "localhost", "/quotes")
            .sendJsonObject(body)
            .onComplete(testContext.succeeding { response ->
                testContext.verify {
                    assertThat(response.statusCode()).isEqualTo(500)
                    assertThat(response.bodyAsJsonObject().getString("error"))
                        .contains("Internal server error")
                }
                testContext.completeNow()
            })
    }
}