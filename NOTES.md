# Notes

### Version Update
Updated `build.grandle.kts` to JDK version 21 to match my local

### Bugs found and fixed
- 'payCurrency' field not being considered on Quote handler and logic, added it to the request, but did not implement specific quote logic into it, but used for validation. Not exactly real world scenario, but fixed the request.
- Quote Calculation only correct for BUY using division by the market price, for SELL we needed to multiply by market price
- Incomplete validation on create payment, decided to Create a new request type to serialize the request into, and then extended the validator to cover more invalid input states.
- Missing validation on other endpoints - Followed the same approach as above, to standardise the expected input structure and then validate input before handing it over to the service.
- BigDecimal precision loss by using Double on quotes, instead used rounding, which would be a bit more accurate.
- I found that quote expiry was not being considered after a payment had been created, this seems incorrect since the execute call, could be called later after significant price movement and still be successful. I added some additional state and expiry checks to the execute method on the payment service.

### Status endpoint approach
The requirement to respond with payment transition history led me to an approach using some basic events. I felt this would be the simplest way to track the state changes, essentially capturing an event with every update. In the real DB this would be done using transactions, but for this simple example I just implemented an additional map in the in memory DB as this would give us the ability to lookup events for a payment. Using 
 
### Refund endpoint approach
I was initially thinking how I would factor in the slippage during that time it was purchased and the time it was refunded but this complexity didn't seem to be part of this assessment, so my approach was to implement it in a basic form, and follow the example pattern established in the `execute` path. I figured, realistically it would touch many more systems that weren't part of this basic setup. 

### Testing strategy
My strategy for the testing was to separate the business logic and make that independently testable. So I wanted comprehensive test coverage of the Service layer and the Validation logic. This meant I could make testing the router and handler layer much simpler, as I only needed basic wiring tests, since most of the logic was covered in the service and validation tests.

### Improvements made
- Refactored the ValrClient out to an interface to avoid concrete implementation and make testing possible.
- Split repositories to domain specific interfaces this to provide better separations of concerns and makes it possible to test using mocks. Also allows for alternative implementations if required. eg. Postgres
- Renamed concrete repository implementations to provide better detail. 
- Moved business logic to dedicated domain services for better separation of concerns and to avoid "fat" handlers, essentially I wanted to simplify the handlers role to just get requests, validate and respond only. Also made writing tests for business logic easier if moved to individual service class.
- Removed duplication in response handling and moved response logic to single function so they can be reused, also centralized error handling. This was to make response handling consistent across handlers.
- Cleaned up hardcoded configs scattered across the code, and placed them into centralized config file to maker it easier to find and update if needed.
- Added state machine for Payments to control transitions and build obvious logic to track workflow.
- Added Quote status to help track claimed quotes, to help ensure idempotent payments
- Added graceful shutdown to hard stops in the service.

### Assumptions
- Decided to keep the basic in-memory repo, makes sense for this small project.
- When creating a quote, I assumed `payCurrency` was just serving as a record for the quote, and did not factor into the calculation, but in a real app this would be able to support additional payment currencies.
- Did not consider min and max payment amounts in the project, but in the real world, this would be a consideration driven by business requirements. 
- Endpoint authentication was not considered, but would be in a real application.
- Fee is charged on input. The customer's payAmount is reduced by the fee before conversion, regardless of side.
- Scale 8 for all crypto. Fine for BTC; some altcoins use up to 18. pair-specific precision matters, I would extend this with additional currency config, rather than a hardcoded 8.
- Left the data responses object wrapped in the "data" field, seems it was designed this way and works well.

### AI use
- I used Claude Code in my IDE for some assistance with this. Mostly using prompts to double-check my thinking or implementations.
- I used AI for quite a bit of the Kotlin syntax and best practices, since most of my recent experience is using Golang.
- It generated the payments state machine syntax for me.
- Used AI to generate a fair amount of the handler and service tests to speed it up.

### Things I would do differently with more time
- On the quote request, I would implement a way for the user to specify either the fiat amount they want to spend, or the crypto amount the wany to buy, which would then quote them on the required fiat spend amount. For simplicity and the way the endpoint variables were named, I felt it would be better to keep it to fiat for buy and crypto for sell.
- Atomic quote/payment writes via a single transaction or an outbox pattern. With the in-memory repo this is invisible, but against a real DB a crash between the two leaves quote state and payment state out of sync. The standard fix is to write both rows + an outbox event in a single transaction.
- Basic event data stored in in-memory repo, would be better to use relational DB with transactions.
- HTTP timeouts and retry backoff on the VALR client. 
- The read-then-write on quote status isn't really atomic; in a real DB you'd use either a transaction with SELECT FOR UPDATE or a conditional update on status = ACTIVE.
- Thread safety of in-memory repos, fine for this small project but a real DB would help here
- UUID validation on some of the endpoints are done inline, ideally on larger projects all validation should be consistent.
- UUID's in the status events seems unnecessary to include as part of the response. But for simplicity I re-used basic entity models, but a better approach would be to have been to have different models for each "layer", API request and response models for Handlers, DTO models for services layer and Entity Models for DB layer.
- Would implement state transitions on quotes in the same pattern as payments using events.