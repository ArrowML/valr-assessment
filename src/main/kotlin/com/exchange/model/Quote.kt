package com.exchange.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class Quote(
    val id: String = UUID.randomUUID().toString(),
    val currencyPair: String,
    val price: BigDecimal,
    val payAmount: BigDecimal,
    val receiveAmount: BigDecimal,
    val fee: BigDecimal,
    val side: String,
    val createdAt: Instant = Instant.now(),
    val expiresAt: Instant = Instant.now().plusSeconds(120)
)
