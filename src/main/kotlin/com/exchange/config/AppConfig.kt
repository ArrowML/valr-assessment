package com.exchange.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.math.BigDecimal

data class AppConfig(
    val server: Server,
    val exchange: Exchange,
    val quotes: Quotes,
    val brokerage: Brokerage,
) {
    data class Server(val port: Int)
    data class Exchange(val baseUrl: String)
    data class Quotes(val supportedCurrencyPairs: Set<String>, val ttlSeconds: Long)
    data class Brokerage(val feePercent: BigDecimal)

    companion object {
        fun load(resourcePath: String = "/application.yaml"): AppConfig {
            val stream = AppConfig::class.java.getResourceAsStream(resourcePath)
                ?: throw IllegalStateException("Config not found on classpath: $resourcePath")
            val mapper = YAMLMapper()
                .registerKotlinModule()
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            return mapper.readValue(stream, AppConfig::class.java)
        }
    }
}
