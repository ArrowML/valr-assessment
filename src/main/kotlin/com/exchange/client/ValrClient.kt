package com.exchange.client

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.net.URL

class ValrClient(
    private val baseUrl: String,
) : ExchangeClient {

    private val logger = LoggerFactory.getLogger(ValrClient::class.java)
    private val objectMapper = ObjectMapper().registerKotlinModule()

    /**
     * Fetches the current market price for a currency pair.
     */
    override fun getMarketPrice(currencyPair: String): BigDecimal {
        logger.info("Fetching price for {}", currencyPair)

        val url = "$baseUrl/$currencyPair/marketsummary"

        var lastException: Exception? = null
        for (attempt in 1..3) {
            try {
                val response = URL(url).readText()
                val json: JsonNode = objectMapper.readTree(response)
                return json.get("lastTradedPrice").asText().toBigDecimal()
            } catch (e: Exception) {
                logger.warn("Attempt $attempt failed for $currencyPair", e)
                lastException = e
            }
        }
        throw lastException!!
    }
}
