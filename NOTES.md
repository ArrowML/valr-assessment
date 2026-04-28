# Notes

### Version Update
Updated `build.grandle.kts` to JDK version 21 to match my local

### Improvements
- Refactored the Valr client out to an interface to avoid concrete implementation in tests
- Split repositories to domain specific interfaces this to provide better separations of concerns.
- Renamed concrete repository implementations to provide better detail. 