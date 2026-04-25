package com.exchange.router

import com.exchange.handler.QuoteHandler
import com.exchange.handler.PaymentHandler
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler

object PaymentRouter {

    fun create(vertx: Vertx, quoteHandler: QuoteHandler, paymentHandler: PaymentHandler): Router {
        val router = Router.router(vertx)

        router.route().handler(BodyHandler.create())

        router.post("/quotes").handler { ctx -> quoteHandler.createQuote(ctx) }

        router.post("/payments").handler { ctx -> paymentHandler.createPayment(ctx) }
        router.post("/payments/:id/execute").handler { ctx -> paymentHandler.executePayment(ctx) }
        router.get("/payments/:id").handler { ctx -> paymentHandler.getPayment(ctx) }

        // TODO: POST /payments/:id/refund — candidate implements this
        // TODO: GET /payments/:id/status — candidate implements this

        return router
    }
}
