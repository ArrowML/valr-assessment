package com.exchange.validation

import com.exchange.handler.CreatePaymentRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class PaymentValidatorTest {

    private lateinit var validator: PaymentValidator

    @BeforeEach
    fun setUp() {
        validator = PaymentValidator()
    }

    @Test
    fun `should pass validation for valid input`() {
        val request = CreatePaymentRequest(
            quoteId = UUID.randomUUID().toString(),
            customerReference = "customer-ref",
        )
        val result = validator.validate(request)
        assertThat(result).isInstanceOf(ValidationResult.Valid::class.java)
    }

    @Test
    fun `should fail when quoteId is null`() {
        val request = CreatePaymentRequest(quoteId = null, customerReference = "customer-ref")
        val result = validator.validate(request)
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat((result as ValidationResult.Invalid).errors).contains("quoteId is required")
    }

    @Test
    fun `should fail when quoteId is not a valid UUID`() {
        val request = CreatePaymentRequest(quoteId = "not-a-uuid", customerReference = "customer-ref")
        val result = validator.validate(request)
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat((result as ValidationResult.Invalid).errors).contains("quoteId must be a valid UUID")
    }
}
