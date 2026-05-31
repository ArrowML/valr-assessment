package com.exchange.service

import com.exchange.client.ExchangeClient
import com.exchange.model.Quote
import com.exchange.model.Side
import com.exchange.repository.quote.QuoteRepository
import io.vertx.core.Future
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

class QuoteService(
    private val quoteRepository: QuoteRepository,
    private val exchangeClient: ExchangeClient,
    private val brokerageFeePercent: BigDecimal,
    private val ttlSeconds: Long,
) {
    private val logger = LoggerFactory.getLogger(QuoteService::class.java)

    fun createQuote(currencyPair: String, payAmount: BigDecimal, payCurrency: String, side: Side): Future<Quote> {
        return exchangeClient.getMarketPrice(currencyPair).map {
            marketPrice ->

            val fee = payAmount.multiply(brokerageFeePercent)
            val netAmount = payAmount.subtract(fee)

            // BUY: pay fiat → receive crypto (scale 8 covers BTC satoshis)
            // SELL: pay crypto → receive fiat (scale 2 for cents)
            val receiveAmount = when (side) {
                Side.BUY -> netAmount.divide(marketPrice, 8, RoundingMode.HALF_UP)
                Side.SELL -> netAmount.multiply(marketPrice).setScale(2, RoundingMode.HALF_UP)
            }

            val now = Instant.now()
            val quote = Quote(
                currencyPair = currencyPair,
                price = marketPrice,
                payAmount = payAmount,
                payCurrency = payCurrency,
                receiveAmount = receiveAmount,
                fee = fee,
                side = side,
                createdAt = now,
                expiresAt = now.plusSeconds(ttlSeconds),
            )

            quoteRepository.saveQuote(quote)
            logger.info("Created quote {} for {} {}", quote.id, currencyPair, payAmount)

            quote
        }


    }
}
