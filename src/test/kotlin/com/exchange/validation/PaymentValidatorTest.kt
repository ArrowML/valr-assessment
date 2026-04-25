package com.exchange.validation

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat

class PaymentValidatorTest {

    private lateinit var validator: PaymentValidator

    @BeforeEach
    fun setUp() {
        validator = PaymentValidator()
    }

    @Test
    fun `should pass validation for valid input`() {
        val errors = validator.validate("quote-123", "customer-ref")
        assertThat(errors).isEmpty()
    }

    @Test
    fun `should fail when quoteId is null`() {
        val errors = validator.validate(null, "customer-ref")
        assertThat(errors).contains("quoteId is required")
    }
}
