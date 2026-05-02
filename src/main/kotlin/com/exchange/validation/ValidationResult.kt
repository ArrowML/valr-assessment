package com.exchange.validation

sealed class ValidationResult<out T> {
    data class Valid<T>(val value: T) : ValidationResult<T>()
    data class Invalid(val errors: List<String>) : ValidationResult<Nothing>()
}
