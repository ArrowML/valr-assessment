package com.exchange.handler

import com.exchange.model.ApiResponse
import com.exchange.router.json
import com.exchange.service.PaymentService
import com.exchange.service.ValidationException
import com.exchange.validation.PaymentValidator
import com.fasterxml.jackson.databind.ObjectMapper

import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory

class PaymentHandler(
    private val paymentService: PaymentService,
    private val objectMapper: ObjectMapper,
    private val validator: PaymentValidator,
) {

    private val logger = LoggerFactory.getLogger(PaymentHandler::class.java)

    fun createPayment(ctx: RoutingContext) {
        try {
            val body = ctx.body().asJsonObject()
            val quoteId = body.getString("quoteId")
            val customerReference = body.getString("customerReference")

            val validationErrors = validator.validate(quoteId, customerReference)
            if (validationErrors.isNotEmpty()) {
                throw ValidationException(validationErrors)
            }

            val payment = paymentService.createPayment(quoteId, customerReference)

            ctx.json(201, ApiResponse.ok(payment), objectMapper)

        } catch (e: Exception) {
            ctx.fail(e)
        }
    }

    fun executePayment(ctx: RoutingContext) {
        try {
            val paymentId = ctx.pathParam("id")
            val payment = paymentService.executePayment(paymentId)
            ctx.json(200, ApiResponse.ok(payment), objectMapper)
        } catch (e: Exception) {
            logger.error("Failed to execute payment", e)
            ctx.fail(e)
        }
    }

    fun getPayment(ctx: RoutingContext) {
        try {
            val paymentId = ctx.pathParam("id")
            val payment = paymentService.getPayment(paymentId)
            ctx.json(200, ApiResponse.ok(payment), objectMapper)
        } catch (e: Exception) {
            ctx.fail(e)
        }
    }

    fun getPaymentStatus(ctx: RoutingContext) {
        try {
            val paymentId = ctx.pathParam("id")
            val payment = paymentService.getPayment(paymentId)
            ctx.json(200, ApiResponse.ok(payment.status), objectMapper)
        } catch (e: Exception) {
            logger.error("Failed to get payment status", e)
            ctx.fail(e)
        }
    }
}
