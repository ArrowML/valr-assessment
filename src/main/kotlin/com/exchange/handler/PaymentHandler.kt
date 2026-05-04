package com.exchange.handler

import com.exchange.model.ApiResponse
import com.exchange.model.CreatePaymentRequest
import com.exchange.router.json
import com.exchange.service.PaymentService
import com.exchange.service.ValidationException
import com.exchange.validation.PaymentValidator
import com.exchange.validation.UuidValidation
import com.exchange.validation.ValidationResult
import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.ext.web.RoutingContext

class PaymentHandler(
    private val paymentService: PaymentService,
    private val objectMapper: ObjectMapper,
    private val validator: PaymentValidator,
) {

    fun createPayment(ctx: RoutingContext) {
        val request = try {
            objectMapper.readValue(ctx.body().asString().orEmpty(), CreatePaymentRequest::class.java)
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

        try {
            val payment = paymentService.createPayment(validated.quoteId, validated.customerReference)
            ctx.json(201, ApiResponse.ok(payment), objectMapper)
        } catch (e: Exception) {
            ctx.fail(e)
        }
    }

    fun executePayment(ctx: RoutingContext) {
        try {
            val paymentId = ctx.pathParam("id")
            UuidValidation.requireValidUuid(paymentId, "paymentId")

            val payment = paymentService.executePayment(paymentId)
            ctx.json(200, ApiResponse.ok(payment), objectMapper)
        } catch (e: Exception) {
            ctx.fail(e)
        }
    }

    fun getPayment(ctx: RoutingContext) {
        try {
            val paymentId = ctx.pathParam("id")
            UuidValidation.requireValidUuid(paymentId, "paymentId")

            val payment = paymentService.getPayment(paymentId)
            ctx.json(200, ApiResponse.ok(payment), objectMapper)
        } catch (e: Exception) {
            ctx.fail(e)
        }
    }

    fun refundPayment(ctx: RoutingContext) {
        try {
            val paymentId = ctx.pathParam("id")
            UuidValidation.requireValidUuid(paymentId, "paymentId")

            val payment = paymentService.refundPayment(paymentId)
            ctx.json(200, ApiResponse.ok(payment), objectMapper)
        } catch (e: Exception) {
            ctx.fail(e)
        }
    }

    fun getPaymentStatus(ctx: RoutingContext) {
        try {
            val paymentId = ctx.pathParam("id")
            UuidValidation.requireValidUuid(paymentId, "paymentId")

            val paymentSummary = paymentService.getPaymentStatus(paymentId)
            ctx.json(200, ApiResponse.ok(paymentSummary), objectMapper)
        } catch (e: Exception) {
            ctx.fail(e)
        }
    }
}
