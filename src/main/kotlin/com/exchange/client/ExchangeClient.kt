package com.exchange.client

import io.vertx.core.Future
import java.math.BigDecimal

interface ExchangeClient {
    fun getMarketPrice(currencyPair: String): Future<BigDecimal>
}