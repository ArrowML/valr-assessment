package com.exchange.client

import java.math.BigDecimal

interface ExchangeClient {
    fun getMarketPrice(currencyPair: String): BigDecimal
}