# Notes

### Version Update
Updated `build.grandle.kts` to JDK version 21 to match my local

### Refund approach
- Took some time to think about the implications of a refund.
- Refund implies customers funds are returned, but goods are returned to vendor
- Crypto needs to factor in the slippage during that time it was purchased and the time it was refunded
- How fees apply in both directions

### Improvements
- Refactored the Valr client out to an interface to avoid concrete implementation in tests
- Split repositories to domain specific interfaces this to provide better separations of concerns.
- Renamed concrete repository implementations to provide better detail. 
- Moved business logic to dedicated domain service for better separation of concerns
- Removed duplication in response handling

### Domain separation