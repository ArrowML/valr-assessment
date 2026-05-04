package com.exchange.validation

import com.exchange.model.CreatePaymentRequest
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
        val uuid = UUID.randomUUID().toString()
        val request = CreatePaymentRequest(
            quoteId = uuid,
            customerReference = "customer-ref",
        )
        val result = validator.validate(request)
        assertThat(result).isInstanceOf(ValidationResult.Valid::class.java)
        assertThat((result as ValidationResult.Valid).value).isEqualTo(ValidatedCreatePayment(
            quoteId = uuid,
            customerReference = "customer-ref",
        ))
    }

    @Test
    fun `should fail when quoteId is null`() {
        val request = CreatePaymentRequest(quoteId = null, customerReference = "customer-ref")
        val result = validator.validate(request)
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat((result as ValidationResult.Invalid).errors).containsExactly("quoteId is required")
    }

    @Test
    fun `should fail when quoteId is empty`() {
        val request = CreatePaymentRequest(quoteId = " ", customerReference = "customer-ref")
        val result = validator.validate(request)
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat((result as ValidationResult.Invalid).errors).containsExactly("quoteId is required")
    }

    @Test
    fun `should fail when customerReference is null`() {
        val request = CreatePaymentRequest(quoteId = UUID.randomUUID().toString(), customerReference = null)
        val result = validator.validate(request)
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat((result as ValidationResult.Invalid).errors).containsExactly("customerReference is required")
    }

    @Test
    fun `should fail when customerReference is empty`() {
        val request = CreatePaymentRequest(quoteId = UUID.randomUUID().toString(), customerReference = " ")
        val result = validator.validate(request)
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat((result as ValidationResult.Invalid).errors).containsExactly("customerReference is required")
    }

    @Test
    fun `should fail when quoteId is not a valid UUID`() {
        val request = CreatePaymentRequest(quoteId = "not-a-uuid", customerReference = "customer-ref")
        val result = validator.validate(request)
        assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
        assertThat((result as ValidationResult.Invalid).errors).containsExactly("quoteId must be a valid UUID")
    }
}
