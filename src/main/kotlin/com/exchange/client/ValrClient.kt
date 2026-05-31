package com.exchange.client

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.Vertx.vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.net.URL

class ValrClient(
    private val baseUrl: String,
    vertx: Vertx,
) : ExchangeClient {
    private val logger = LoggerFactory.getLogger(ValrClient::class.java)
    private val objectMapper = ObjectMapper().registerKotlinModule()
    private val client = WebClient.create(vertx, WebClientOptions().setIdleTimeout(5000).setConnectTimeout(5000))

    /**
     * Fetches the current market price for a currency pair.
     */
    override fun getMarketPrice(currencyPair: String): Future<BigDecimal> {
        logger.info("Fetching price for {}", currencyPair)

        val url = "$baseUrl/$currencyPair/marketsummary"

        var lastException: Exception? = null
        for (attempt in 1..3) {
            try {
                return client.getAbs(url).send().map { response ->
                        if (response.statusCode() != 200) {
                            throw RuntimeException("Error fetching price for $currencyPair")
                        }
                        if (response.bodyAsString().isNullOrBlank()){
                            throw RuntimeException("Empty response fetching price for $currencyPair")
                        }
                        val resp = objectMapper.readTree(response.bodyAsString())

                        resp.get("lastTradedPrice")?.asText()?.toBigDecimal()
                            ?:throw RuntimeException("Error fetching price for $currencyPair")
                }
            } catch (e: Exception) {
                logger.warn("Attempt $attempt failed for $currencyPair", e)
                lastException = e
            }
        }
        throw lastException!!
    }
}
