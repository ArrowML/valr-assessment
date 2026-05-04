package com.exchange.handler

import com.exchange.model.Payment
import com.exchange.model.PaymentEvent
import com.exchange.model.PaymentEventType
import com.exchange.model.PaymentStatus
import com.exchange.model.Quote
import com.exchange.model.QuoteStatus
import com.exchange.model.Side
import com.exchange.repository.payment.InMemoryPaymentRepository
import com.exchange.repository.quote.QuoteRepository
import com.exchange.router.PaymentRouter
import com.exchange.service.PaymentService
import com.exchange.validation.PaymentValidator
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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@ExtendWith(VertxExtension::class)
class PaymentHandlerTest {
    private lateinit var client: WebClient
    private lateinit var paymentRepository: InMemoryPaymentRepository
    private var port: Int = 0
    private val quoteRepositoryMock: QuoteRepository = mock()
    private val quoteHandler: QuoteHandler = mock()

    @BeforeEach
    fun setUp(vertx: Vertx, testContext: VertxTestContext) {

        reset(quoteRepositoryMock)

        val objectMapper = ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        paymentRepository = InMemoryPaymentRepository()
        val paymentValidator = PaymentValidator()
        val paymentService = PaymentService(
            paymentRepository,
            quoteRepositoryMock
        )
        val paymentHandler = PaymentHandler(paymentService, objectMapper, paymentValidator )

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

    private fun activeQuote(
        id: String = UUID.randomUUID().toString(),
        status: QuoteStatus = QuoteStatus.ACTIVE,
        expiresAt: Instant = Instant.now().plusSeconds(60),
    ) = Quote(
        id = id,
        currencyPair = "BTCZAR",
        price = BigDecimal("10000"),
        payAmount = BigDecimal("1000"),
        payCurrency = "ZAR",
        receiveAmount = BigDecimal("0.0985"),
        fee = BigDecimal("15"),
        side = Side.BUY,
        status = status,
        expiresAt = expiresAt,
    )

    private fun seedPendingPayment(quoteId: String = UUID.randomUUID().toString()): Payment {
        val payment = Payment(quoteId = quoteId, customerReference = "custom-ref")
        val event = PaymentEvent(
            paymentId = payment.id,
            eventType = PaymentEventType.CREATED,
            fromStatus = null,
            toStatus = PaymentStatus.PENDING,
        )
        paymentRepository.savePayment(payment, event)
        return payment
    }

    private fun seedCompletedPayment(quoteId: String = UUID.randomUUID().toString()): Payment {
        val payment = Payment(quoteId = quoteId, customerReference = "custom-ref")
        payment.transitionTo(PaymentStatus.PROCESSING)
        payment.transitionTo(PaymentStatus.COMPLETED)
        paymentRepository.savePayment(payment, PaymentEvent(
            paymentId = payment.id,
            eventType = PaymentEventType.COMPLETED,
            fromStatus = PaymentStatus.PROCESSING,
            toStatus = PaymentStatus.COMPLETED,
        ))
        return payment
    }

    @Nested
    inner class CreatePayment {

        @Test
        fun `should return 400 for validation failure`(testContext: VertxTestContext) {
            val body = JsonObject()
                .put("quoteId", "abc-123")
                .put("customerReference", "custom-ref")

            client.post(port, "localhost", "/payments")
                .sendJsonObject(body)
                .onComplete(testContext.succeeding { response ->
                    testContext.verify {
                        assertThat(response.statusCode()).isEqualTo(400)
                        assertThat(response.bodyAsJsonObject().getString("error"))
                            .contains("quoteId must be a valid UUID")
                    }
                    testContext.completeNow()
                })
        }

        @Test
        fun `should return 201 for successful create`(testContext: VertxTestContext) {
            val uuid = UUID.randomUUID().toString()
            val now = Instant.now()
            val body = JsonObject()
                .put("quoteId", uuid)
                .put("customerReference", "custom-ref")

            whenever(quoteRepositoryMock.findQuote(uuid)).thenReturn(Quote(
                id = uuid,
                currencyPair = "BTCZAR",
                price = BigDecimal(10000),
                payAmount = BigDecimal(1000),
                payCurrency = "ZAR",
                receiveAmount = BigDecimal(0.0005),
                fee = BigDecimal(15),
                side = Side.BUY,
                status = QuoteStatus.ACTIVE,
                createdAt = now,
                expiresAt = now.plusSeconds(60)
            ))

            client.post(port, "localhost", "/payments")
                .sendJsonObject(body)
                .onComplete(testContext.succeeding { response ->
                    testContext.verify {
                        assertThat(response.statusCode()).isEqualTo(201)

                        val json = response.bodyAsJsonObject()
                        assertThat(json.getBoolean("success")).isTrue()

                        val data = json.getJsonObject("data")
                        assertThat(data.getString("id")).isNotBlank()
                        assertThat(data.getString("quoteId")).isEqualTo(uuid)
                        assertThat(data.getString("customerReference")).isEqualTo("custom-ref")
                        assertThat(data.getString("status")).isEqualTo(PaymentStatus.PENDING.toString())
                    }
                    testContext.completeNow()
                })
        }
    }

    @Nested
    inner class ExecutePayment {

        @Test
        fun `should return 400 for invalid UUID`(testContext: VertxTestContext) {
            client.post(port, "localhost", "/payments/not-a-uuid/execute")
                .send()
                .onComplete(testContext.succeeding { response ->
                    testContext.verify {
                        assertThat(response.statusCode()).isEqualTo(400)
                    }
                    testContext.completeNow()
                })
        }

        @Test
        fun `should return 200 and COMPLETED status on success`(testContext: VertxTestContext) {
            val quote = activeQuote(status = QuoteStatus.CLAIMED)
            val payment = seedPendingPayment(quote.id)
            whenever(quoteRepositoryMock.findQuote(quote.id)).thenReturn(quote)

            client.post(port, "localhost", "/payments/${payment.id}/execute")
                .send()
                .onComplete(testContext.succeeding { response ->
                    testContext.verify {
                        assertThat(response.statusCode()).isEqualTo(200)
                        val data = response.bodyAsJsonObject().getJsonObject("data")
                        assertThat(data.getString("id")).isEqualTo(payment.id)
                        assertThat(data.getString("status")).isEqualTo(PaymentStatus.COMPLETED.toString())
                    }
                    testContext.completeNow()
                })
        }
    }

    @Nested
    inner class GetPayment {

        @Test
        fun `should return 400 for invalid UUID`(testContext: VertxTestContext) {
            client.get(port, "localhost", "/payments/not-a-uuid")
                .send()
                .onComplete(testContext.succeeding { response ->
                    testContext.verify {
                        assertThat(response.statusCode()).isEqualTo(400)
                    }
                    testContext.completeNow()
                })
        }

        @Test
        fun `should return 200 with payment for valid id`(testContext: VertxTestContext) {
            val payment = seedPendingPayment()

            client.get(port, "localhost", "/payments/${payment.id}")
                .send()
                .onComplete(testContext.succeeding { response ->
                    testContext.verify {
                        assertThat(response.statusCode()).isEqualTo(200)
                        val data = response.bodyAsJsonObject().getJsonObject("data")
                        assertThat(data.getString("id")).isEqualTo(payment.id)
                        assertThat(data.getString("status")).isEqualTo(PaymentStatus.PENDING.toString())
                    }
                    testContext.completeNow()
                })
        }

        @Test
        fun `should return 404 when payment not found`(testContext: VertxTestContext) {
            client.get(port, "localhost", "/payments/${UUID.randomUUID()}")
                .send()
                .onComplete(testContext.succeeding { response ->
                    testContext.verify {
                        assertThat(response.statusCode()).isEqualTo(404)
                    }
                    testContext.completeNow()
                })
        }
    }

    @Nested
    inner class RefundPayment {

        @Test
        fun `should return 400 for invalid UUID`(testContext: VertxTestContext) {
            client.post(port, "localhost", "/payments/not-a-uuid/refund")
                .send()
                .onComplete(testContext.succeeding { response ->
                    testContext.verify {
                        assertThat(response.statusCode()).isEqualTo(400)
                    }
                    testContext.completeNow()
                })
        }

        @Test
        fun `should return 200 and REFUNDED for completed payment`(testContext: VertxTestContext) {
            val payment = seedCompletedPayment()

            client.post(port, "localhost", "/payments/${payment.id}/refund")
                .send()
                .onComplete(testContext.succeeding { response ->
                    testContext.verify {
                        assertThat(response.statusCode()).isEqualTo(200)
                        val data = response.bodyAsJsonObject().getJsonObject("data")
                        assertThat(data.getString("status")).isEqualTo(PaymentStatus.REFUNDED.toString())
                    }
                    testContext.completeNow()
                })
        }

        @Test
        fun `should return 422 when payment is not COMPLETED`(testContext: VertxTestContext) {
            val payment = seedPendingPayment()

            client.post(port, "localhost", "/payments/${payment.id}/refund")
                .send()
                .onComplete(testContext.succeeding { response ->
                    testContext.verify {
                        assertThat(response.statusCode()).isEqualTo(422)
                    }
                    testContext.completeNow()
                })
        }
    }

    @Nested
    inner class GetPaymentStatus {

        @Test
        fun `should return 400 for invalid UUID`(testContext: VertxTestContext) {
            client.get(port, "localhost", "/payments/not-a-uuid/status")
                .send()
                .onComplete(testContext.succeeding { response ->
                    testContext.verify {
                        assertThat(response.statusCode()).isEqualTo(400)
                    }
                    testContext.completeNow()
                })
        }

        @Test
        fun `should return 200 with status, history and quote details`(testContext: VertxTestContext) {
            val quote = activeQuote()
            val payment = seedPendingPayment(quote.id)
            whenever(quoteRepositoryMock.findQuote(quote.id)).thenReturn(quote)

            client.get(port, "localhost", "/payments/${payment.id}/status")
                .send()
                .onComplete(testContext.succeeding { response ->
                    testContext.verify {
                        assertThat(response.statusCode()).isEqualTo(200)
                        val data = response.bodyAsJsonObject().getJsonObject("data")
                        assertThat(data.getString("status")).isEqualTo(PaymentStatus.PENDING.toString())
                        assertThat(data.getJsonArray("history")).isNotEmpty
                        assertThat(data.getJsonObject("quoteDetails").getString("id")).isEqualTo(quote.id)
                    }
                    testContext.completeNow()
                })
        }
    }
}
