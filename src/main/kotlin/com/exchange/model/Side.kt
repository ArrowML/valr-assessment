package com.exchange.model

enum class Side {
    BUY,
    SELL;

    companion object {
        fun from(value: String?): Side {
            val normalized = value?.trim()?.uppercase()
                ?: throw IllegalArgumentException("side is required")
            return entries.firstOrNull { it.name == normalized }
                ?: throw IllegalArgumentException("side must be one of ${entries.joinToString()}, got '$value'")
        }
    }
}
