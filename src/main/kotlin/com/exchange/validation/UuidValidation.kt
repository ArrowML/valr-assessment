package com.exchange.validation

import com.exchange.service.ValidationException
import java.util.UUID

object UuidValidation {

    fun isValidUuid(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        return runCatching { UUID.fromString(value) }.isSuccess
    }

    fun requireValidUuid(value: String?, fieldName: String) {
        if (!isValidUuid(value)) {
            throw ValidationException(listOf("$fieldName must be a valid UUID"))
        }
    }
}
