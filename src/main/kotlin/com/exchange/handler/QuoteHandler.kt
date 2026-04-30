package com.exchange.handler

import com.exchange.client.ExchangeClient
import com.exchange.model.ApiResponse
import com.exchange.model.Quote
import com.exchange.repository.quote.QuoteRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory
import java.math.BigDecimal

class QuoteHandler(
    private val exchangeClient: ExchangeClient,
    private val quoteRepository: QuoteRepository,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(QuoteHandler::class.java)

    private val brokerageFeePercent = 0.015 // 1.5% fee

    fun createQuote(ctx: RoutingContext) {
        try {
            val body = ctx.body().asJsonObject()
            val currencyPair = body.getString("currencyPair")
            val payAmount = BigDecimal(body.getString("payAmount"))
            val side = body.getString("side")

            val marketPrice = exchangeClient.getMarketPrice(currencyPair)

            val fee = BigDecimal(payAmount.toDouble() * brokerageFeePercent)
            val netAmount = payAmount.subtract(fee)
            val receiveAmount = netAmount.divide(marketPrice)

            val quote = Quote(
                currencyPair = currencyPair,
                price = marketPrice,
                payAmount = payAmount,
                receiveAmount = receiveAmount,
                fee = fee,
                side = side
            )

            quoteRepository.saveQuote(quote)

            logger.info("Created quote {} for {} {}", quote.id, currencyPair, payAmount)

            ctx.response()
                .setStatusCode(201)
                .putHeader("Content-Type", "application/json")
                .end(objectMapper.writeValueAsString(ApiResponse.ok(quote)))

        } catch (e: Exception) {
            logger.error("Failed to create quote", e)
            ctx.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "application/json")
                .end(objectMapper.writeValueAsString(ApiResponse.error<Nothing>("Failed to create quote")))
        }
    }
}
