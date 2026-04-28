package com.exchange.handler

import com.exchange.model.ApiResponse
import com.exchange.model.Payment
import com.exchange.model.PaymentStatus
import com.exchange.repository.payment.PaymentRepository
import com.exchange.repository.quote.QuoteRepository
import com.exchange.validation.PaymentValidator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory
import java.time.Instant

class PaymentHandler(
    private val paymentRepository: PaymentRepository,
    private val quoteRepository: QuoteRepository,
    private val validator: PaymentValidator
) {

    private val logger = LoggerFactory.getLogger(PaymentHandler::class.java)
    private val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    fun createPayment(ctx: RoutingContext) {
        try {
            val body = ctx.body().asJsonObject()
            val quoteId = body.getString("quoteId")
            val customerReference = body.getString("customerReference")

            val quote = quoteRepository.findQuote(quoteId)
            if (quote == null) {
                ctx.response()
                    .setStatusCode(404)
                    .putHeader("Content-Type", "application/json")
                    .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Quote not found")))
                return
            }

            val validationErrors = validator.validate(quoteId, customerReference)
            if (validationErrors.isNotEmpty()) {
                ctx.response()
                    .setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>(validationErrors.joinToString(", "))))
                return
            }

            val payment = Payment(
                quoteId = quoteId,
                customerReference = customerReference
            )

            paymentRepository.savePayment(payment)

            logger.info("Created payment {} for quote {}", payment.id, quoteId)

            ctx.response()
                .setStatusCode(201)
                .putHeader("Content-Type", "application/json")
                .end(objectMapper.writeValueAsString(ApiResponse.ok(payment)))

        } catch (e: Exception) {
            logger.error("Failed to create payment", e)
            ctx.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "application/json")
                .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Failed to create payment")))
        }
    }

    fun executePayment(ctx: RoutingContext) {
        try {
            val paymentId = ctx.pathParam("id")

            val payment = paymentRepository.findPayment(paymentId)
            if (payment == null) {
                ctx.response()
                    .setStatusCode(404)
                    .putHeader("Content-Type", "application/json")
                    .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Payment not found")))
                return
            }

            if (payment.status != PaymentStatus.PENDING) {
                ctx.response()
                    .setStatusCode(409)
                    .putHeader("Content-Type", "application/json")
                    .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Payment is not in PENDING status")))
                return
            }

            payment.status = PaymentStatus.PROCESSING
            payment.updatedAt = Instant.now()
            paymentRepository.updatePayment(payment)

            // Simulate async processing — in real life this would be an async operation
            payment.status = PaymentStatus.COMPLETED
            payment.updatedAt = Instant.now()
            paymentRepository.updatePayment(payment)

            logger.info("Executed payment {}", paymentId)

            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(objectMapper.writeValueAsString(ApiResponse.ok(payment)))

        } catch (e: Exception) {
            logger.error("Failed to execute payment", e)
            ctx.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "application/json")
                .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Failed to execute payment")))
        }
    }

    fun getPayment(ctx: RoutingContext) {
        try {
            val paymentId = ctx.pathParam("id")

            val payment = paymentRepository.findPayment(paymentId)
            if (payment == null) {
                ctx.response()
                    .setStatusCode(404)
                    .putHeader("Content-Type", "application/json")
                    .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Payment not found")))
                return
            }

            ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(objectMapper.writeValueAsString(ApiResponse.ok(payment)))

        } catch (e: Exception) {
            logger.error("Failed to get payment", e)
            ctx.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "application/json")
                .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Failed to get payment")))
        }
    }
}
