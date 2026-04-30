package com.exchange.router

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.vertx.ext.web.RoutingContext

object Json {
    val mapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
}

fun RoutingContext.json(status: Int, body: Any, mapper: ObjectMapper = Json.mapper) {
    response()
        .setStatusCode(status)
        .putHeader("Content-Type", "application/json")
        .end(mapper.writeValueAsString(body))
}