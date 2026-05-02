# Notes

### Version Update
Updated `build.grandle.kts` to JDK version 21 to match my local

### Bugs
- 'payCurrency' field not being considered on Quote handler and logic, which would affect quote calculations
- Incomplete validation on create payment, decided to Create a new request type to serialize the request into, and then extended the validator to cover more invalid input states.
- Missing validation on other endpoints - Followed the same approach as above, to standardise the expected input structure and then validate input before handing it over to the service.
- BigDecimal precision loss
- Quote Calculation only correct for BUY

### Status endpoint
- Since this status of a payment is really just a subset of the get payment endpoint data, I figured this would go through a very similar flow, and tried to reuse functionality
 
### Refund endpoint
- Took some time to think about the implications of a refund.
- Refund implies customers funds are returned, but goods are returned to vendor
- Crypto needs to factor in the slippage during that time it was purchased and the time it was refunded but this complexity didn't seem to be part of this assessment, so I implemented it in a basic form. Since it would touch many more systems.

### Improvements
- Refactored the Valr client out to an interface to avoid concrete implementation in tests
- Split repositories to domain specific interfaces this to provide better separations of concerns. This removed the concrete dependencies for better testing and better extension.
- Renamed concrete repository implementations to provide better detail. 
- Moved business logic to dedicated domain services for better separation of concerns and to avoid "fat" handlers, essentially I wanted to make handlers role to get requests, validate and respond only.
- Removed duplication in response handling and moved response logic to single function so they can be reused.
- Cleaned up hardcoded configs scattered across the code, and placed them into centralized config file

### Assumptions
- Endpoint authentication was not considered, but would be in a real world application
- Fee is charged on input. The customer's payAmount is reduced by the fee before conversion, regardless of side. So a BUY fee is in fiat, a SELL fee is in crypto.
- Scale 8 for all crypto. Fine for BTC; some altcoins use 6 or even 18. pair-specific precision matters, you'd push it into config (per-pair or per-currency) rather   
  than a hardcoded 8.

### AI use
- I used AI quite heavily for the Kotlin syntax and approaches.
- Mostly asked it for feedback on implementation and Kotlin best practices, since most of my recent experience is Golang.

### Given more time
- **Atomic quote/payment writes via a single transaction or an outbox pattern. With the in-memory repo this is invisible, but against a real DB a crash between the two leaves quote state and payment state out of sync. The standard fix is to write both rows + an outbox event in a single transaction and let a relay publish downstream events — atomic from the service's perspective, eventually consistent for consumers.