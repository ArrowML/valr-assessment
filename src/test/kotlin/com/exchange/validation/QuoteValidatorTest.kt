package com.exchange.validation

import com.exchange.model.CreateQuoteRequest
import com.exchange.model.CurrencyPair
import com.exchange.model.Side
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class QuoteValidatorTest {
    private val validator = QuoteValidator(
        supportedPairs = mapOf("BTCZAR" to CurrencyPair("BTC", "ZAR")),
    )

    @Test
    fun `should pass validation for valid input`() {
        val result = validator.validate(CreateQuoteRequest(payAmount = "1000", payCurrency = "ZAR", currencyPair = "BTCZAR", side = "BUY"))
        assertThat(result).isInstanceOf(ValidationResult.Valid::class.java)
        assertThat((result as ValidationResult.Valid).value).isEqualTo(ValidatedCreateQuote(
            payAmount = BigDecimal("1000"),
            payCurrency = "ZAR",
            currencyPair = "BTCZAR",
            side = Side.BUY
        ))
    }

    @Test
    fun `should pass validation for valid input with trimming`() {
        val result = validator.validate(CreateQuoteRequest(payAmount = " 1000 ", payCurrency = " ZAR ", currencyPair = "   BTCZAR   ", side = " BUY "))
        assertThat(result).isInstanceOf(ValidationResult.Valid::class.java)
        assertThat((result as ValidationResult.Valid).value).isEqualTo(ValidatedCreateQuote(
            payAmount = BigDecimal("1000"),
            payCurrency = "ZAR",
            currencyPair = "BTCZAR",
            side = Side.BUY
        ))
    }

    @Test
    fun `should fail when currencyPair is missing`() {
        val result = validator.validate(CreateQuoteRequest(payAmount = "1000", payCurrency = "ZAR", side = "BUY"))
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat((result as ValidationResult.Invalid).errors).containsExactly("currencyPair is required")
    }

    @Test
    fun`should fail when currencyPair is empty`() {
        val result = validator.validate(CreateQuoteRequest(payAmount = "1000", currencyPair = "", payCurrency = "ZAR", side = "BUY"))
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat((result as ValidationResult.Invalid).errors).containsExactly("currencyPair is required")
    }

    @Test
    fun `should fail when currencyPair is unsupported`() {
        val result = validator.validate(CreateQuoteRequest(payAmount = "1000", payCurrency = "ZAR", currencyPair = "NOTVAL", side = "BUY"))
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat((result as ValidationResult.Invalid).errors).containsExactly("currencyPair 'NOTVAL' is not supported. Supported: BTCZAR")
    }

    @Test
    fun `should fail when payAmount is missing`() {
        val result = validator.validate(CreateQuoteRequest(payCurrency = "ZAR", currencyPair = "BTCZAR", side = "BUY"))
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat((result as ValidationResult.Invalid).errors).containsExactly("payAmount is required")
    }

    @Test
    fun `should fail payAmount is negative`() {
        val result = validator.validate(CreateQuoteRequest(payAmount = "-1000", payCurrency = "ZAR", currencyPair = "BTCZAR", side = "BUY"))
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat((result as ValidationResult.Invalid).errors).containsExactly("payAmount must be positive")
    }

    @Test
    fun `should fail payAmount is zero`() {
        val result = validator.validate(CreateQuoteRequest(payAmount = "0", payCurrency = "ZAR", currencyPair = "BTCZAR", side = "BUY"))
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat((result as ValidationResult.Invalid).errors).containsExactly("payAmount must be positive")
    }

    @Test
    fun `should fail payAmount is invalid`() {
        val result = validator.validate(CreateQuoteRequest(payAmount = "10ab0", payCurrency = "ZAR", currencyPair = "BTCZAR", side = "BUY"))
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat((result as ValidationResult.Invalid).errors).containsExactly("payAmount must be a valid decimal")
    }

    @Test
    fun `should fail side is missing`() {
        val result = validator.validate(CreateQuoteRequest(payAmount = "1000", payCurrency = "ZAR", currencyPair = "BTCZAR"))
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat((result as ValidationResult.Invalid).errors).containsExactly("side must be one of BUY, SELL")
    }

    @Test
    fun `should fail side is invalid`() {
        val result = validator.validate(CreateQuoteRequest(payAmount = "1000", payCurrency = "ZAR", currencyPair = "BTCZAR", side = "TEST"))
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat((result as ValidationResult.Invalid).errors).containsExactly("side must be one of BUY, SELL")
    }

    @Test
    fun `should pass with normalized side value`() {
        val result = validator.validate(CreateQuoteRequest(payAmount = "1000", payCurrency = "BTC", currencyPair = "BTCZAR", side = " sell "))
        assertThat(result).isInstanceOf(ValidationResult.Valid::class.java)
        assertThat((result as ValidationResult.Valid).value).isEqualTo(ValidatedCreateQuote(
            payAmount = BigDecimal("1000"),
            payCurrency = "BTC",
            currencyPair = "BTCZAR",
            side = Side.SELL
        ))
    }

    @Test
    fun `should fail when payCurrency is missing`() {
        val result = validator.validate(CreateQuoteRequest(payAmount = "1000", currencyPair = "BTCZAR", side = "BUY"))
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat((result as ValidationResult.Invalid).errors).containsExactly("payCurrency is required")
    }

    @Test
    fun `should fail when payCurrency is empty`() {
        val result = validator.validate(CreateQuoteRequest(payAmount = "1000", payCurrency = " ", currencyPair = "BTCZAR", side = "BUY"))
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat((result as ValidationResult.Invalid).errors).containsExactly("payCurrency is required")
    }

    @Test
    fun `should fail when payCurrency is invalid currency`() {
        val result = validator.validate(CreateQuoteRequest(payAmount = "1000", payCurrency = "ZZZ", currencyPair = "BTCZAR", side = "BUY"))
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat((result as ValidationResult.Invalid).errors).containsExactly("payCurrency should be 'ZAR' on BUY")
    }

    @Test
    fun `should fail when payCurrency on BUY is incorrect`() {
        val result = validator.validate(CreateQuoteRequest(payAmount = "1000", payCurrency = "BTC", currencyPair = "BTCZAR", side = "BUY"))
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat((result as ValidationResult.Invalid).errors).containsExactly("payCurrency should be 'ZAR' on BUY")
    }

    @Test
    fun `should fail when payCurrency on SELL is incorrect`() {
        val result = validator.validate(CreateQuoteRequest(payAmount = "1000", payCurrency = "ZAR", currencyPair = "BTCZAR", side = "SELL"))
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat((result as ValidationResult.Invalid).errors).containsExactly("payCurrency should be 'BTC' on SELL")
    }

    @Test
    fun `errors accumulated`() {
        val result = validator.validate(CreateQuoteRequest(payAmount = "", payCurrency = "", currencyPair = "", side = ""))
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat((result as ValidationResult.Invalid).errors).containsExactly(
            "currencyPair is required",
            "payAmount is required",
            "side must be one of BUY, SELL",
            "payCurrency is required",
        )
    }
}