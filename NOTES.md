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
The requirement to respond with payment history made me think keeping track using events would be the easiest way to track the state transitions, essentially capturing an event with every update. In the real DB this would be done using transactions, but for this simple example I just implemented an additional map in the in memory DB.
 
### Refund endpoint
I was initially thinking how I would factor in the slippage during that time it was purchased and the time it was refunded but this complexity didn't seem to be part of this assessment, so I implemented it in a basic form. Since it would touch many more systems that weren't part of this basic setup.

### Improvements
- Refactored the Valr client out to an interface to avoid concrete implementation in tests.
- Split repositories to domain specific interfaces this to provide better separations of concerns and makes it possible to test using mocks. Also allows for alternative implementations if required. eg. Postgres
- Renamed concrete repository implementations to provide better detail. 
- Moved business logic to dedicated domain services for better separation of concerns and to avoid "fat" handlers, essentially I wanted to make handlers role to get requests, validate and respond only.
- Removed duplication in response handling and moved response logic to single function so they can be reused. Also centralized error handling.
- Cleaned up hardcoded configs scattered across the code, and placed them into centralized config file
- Added State machine for Payment state transitions

### Assumptions
- `payAmount` on the quote request seems a bit ambiguous. When combined with `payCurrency` I assumed either that are saying I have X amount of counter, how much base can I get, or I want X amount of base, how much counter will I need? Slightly different contexts.
- Did not consider min and max payment amounts in the project, but in the real world, this would be a consideration.
- Endpoint authentication was not considered, but would be in a real application.
- Fee is charged on input. The customer's payAmount is reduced by the fee before conversion, regardless of side. So I standardized it to always be in the counter.
- Scale 8 for all crypto. Fine for BTC; some altcoins use 6 or even 18. pair-specific precision matters, you'd push it into config (per-pair or per-currency) rather   
  than a hardcoded 8.

### AI use
- I used AI quite heavily for the Kotlin syntax and approaches.
- Mostly asked it for feedback on implementation and Kotlin best practices, since most of my recent experience is Golang.
- Helped with the state machine syntax.

### Given more time
- **Atomic quote/payment writes via a single transaction or an outbox pattern. With the in-memory repo this is invisible, but against a real DB a crash between the two leaves quote state and payment state out of sync. The standard fix is to write both rows + an outbox event in a single transaction.
- Basic event data stored in in-memory repo, would be better to use relational DB with transactions.