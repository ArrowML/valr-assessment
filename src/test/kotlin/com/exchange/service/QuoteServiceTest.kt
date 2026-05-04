package com.exchange.service

import com.exchange.client.ExchangeClient
import com.exchange.model.QuoteStatus
import com.exchange.model.Side
import com.exchange.repository.quote.QuoteRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class QuoteServiceTest {

    private val exchangeClient: ExchangeClient = mock()
    private val quoteRepository: QuoteRepository = mock()

    private val service = QuoteService(
        quoteRepository = quoteRepository,
        exchangeClient = exchangeClient,
        brokerageFeePercent = BigDecimal("0.015"),
        ttlSeconds = 30,
    )

    @Test
    fun `should deduct fee and divides by market price on BUY`() {
        // fee = 1000 * 0.015 = 15, net = 985, receiveAmount = 985 / 10000
        whenever(exchangeClient.getMarketPrice("BTCZAR")).thenReturn(BigDecimal("10000"))

        val quote = service.createQuote("BTCZAR", BigDecimal("1000"), "ZAR", Side.BUY)

        assertThat(quote.fee).isEqualByComparingTo("15")
        assertThat(quote.receiveAmount).isEqualByComparingTo("0.09850000")
    }

    @Test
    fun `should deduct fee and multiply by market price on SELL`() {
        // fee = 0.1 * 0.015 = 0.0015, net = 0.0985, receiveAmount = 0.0985 * 10000
        whenever(exchangeClient.getMarketPrice("BTCZAR")).thenReturn(BigDecimal("10000"))

        val quote = service.createQuote("BTCZAR", BigDecimal("0.1"), "BTC", Side.SELL)

        assertThat(quote.fee).isEqualByComparingTo("0.0015")
        assertThat(quote.receiveAmount).isEqualByComparingTo("985.00")
    }

    @Test
    fun `should round HALF_UP to 8 decimal places to crypto on BUY`() {
        // net = 0.985, 0.985 / 6 = 0.16416666... → 9th digit is 6, rounds up → 0.16416667
        whenever(exchangeClient.getMarketPrice("BTCZAR")).thenReturn(BigDecimal("6"))

        val quote = service.createQuote("BTCZAR", BigDecimal("1"), "ZAR", Side.BUY)

        assertThat(quote.receiveAmount).isEqualByComparingTo("0.16416667")
        assertThat(quote.receiveAmount.scale()).isEqualTo(8)
    }

    @Test
    fun `should round HALF_UP to 2 decimal places to fiat on SELL`() {
        // net = 0.985, 0.985 * 10000.555 = 9850.546675 → rounds up → 9850.55
        whenever(exchangeClient.getMarketPrice("BTCZAR")).thenReturn(BigDecimal("10000.555"))

        val quote = service.createQuote("BTCZAR", BigDecimal("1"), "BTC", Side.SELL)

        assertThat(quote.receiveAmount).isEqualByComparingTo("9850.55")
        assertThat(quote.receiveAmount.scale()).isEqualTo(2)
    }

    @Test
    fun `should set quote fields correctly`() {
        whenever(exchangeClient.getMarketPrice("BTCZAR")).thenReturn(BigDecimal("500000"))

        val quote = service.createQuote("BTCZAR", BigDecimal("1000"), "ZAR", Side.BUY)

        assertThat(quote.id).isNotBlank()
        assertThat(quote.currencyPair).isEqualTo("BTCZAR")
        assertThat(quote.payCurrency).isEqualTo("ZAR")
        assertThat(quote.side).isEqualTo(Side.BUY)
        assertThat(quote.price).isEqualByComparingTo("500000")
        assertThat(quote.payAmount).isEqualByComparingTo("1000")
        assertThat(quote.status).isEqualTo(QuoteStatus.ACTIVE)
    }

    @Test
    fun `should set expiresAt the configured ttlSeconds after createdAt`() {
        whenever(exchangeClient.getMarketPrice("BTCZAR")).thenReturn(BigDecimal("500000"))

        val quote = service.createQuote("BTCZAR", BigDecimal("1000"), "ZAR", Side.BUY)

        assertThat(quote.expiresAt).isEqualTo(quote.createdAt.plusSeconds(30))
    }

    @Test
    fun `should save quote to repository`() {
        whenever(exchangeClient.getMarketPrice("BTCZAR")).thenReturn(BigDecimal("500000"))

        val quote = service.createQuote("BTCZAR", BigDecimal("1000"), "ZAR", Side.BUY)

        verify(quoteRepository).saveQuote(quote)
    }

    @Test
    fun `should fetch the correct currency pair for quote`() {
        whenever(exchangeClient.getMarketPrice("ETHZAR")).thenReturn(BigDecimal("50000"))

        service.createQuote("ETHZAR", BigDecimal("1000"), "ZAR", Side.BUY)

        verify(exchangeClient).getMarketPrice("ETHZAR")
    }
}
