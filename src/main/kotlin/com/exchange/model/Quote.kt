package com.exchange.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class QuoteStatus {
    ACTIVE,
    CLAIMED,
    COMPLETE,
}

data class Quote(
    val id: String = UUID.randomUUID().toString(),
    val currencyPair: String,
    val price: BigDecimal,
    val payAmount: BigDecimal,
    val payCurrency: String,
    val receiveAmount: BigDecimal,
    val fee: BigDecimal,
    val side: Side,
    var status: QuoteStatus = QuoteStatus.ACTIVE,
    val createdAt: Instant = Instant.now(),
    val expiresAt: Instant = Instant.now().plusSeconds(120)
)
