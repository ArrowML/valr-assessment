# Crypto-Fiat Quote & Payment Service

A Kotlin + Vert.x service that generates crypto-fiat price quotes with a TTL, applies a configurable brokerage fee, and processes payments against valid quotes.

## Getting Started

### Prerequisites
- JDK 17+
- Gradle (wrapper included)

### Run the service

```bash
./gradlew run
```

The service starts on `http://localhost:8080`.

### Run tests

```bash
./gradlew test
```

## Architecture

The service provides a simple payment flow:

1. **Get a Quote** — Client requests a price quote for a crypto-fiat pair
2. **Create a Payment** — Client creates a payment referencing a valid quote
3. **Execute Payment** — Payment transitions through states: `PENDING → PROCESSING → COMPLETED`
4. **Refund Payment** — Refund a completed payment. **This needs to be implemented.**

### API Endpoints

#### `POST /quotes`
Generate a price quote for a crypto-fiat pair.

**Request:**
```json
{
  "currencyPair": "BTCZAR",
  "payAmount": "1000.00",
  "payCurrency": "ZAR",
  "side": "BUY"
}
```

**Response:**
```json
{
  "id": "uuid",
  "currencyPair": "BTCZAR",
  "price": "1250000.00",
  "payAmount": "1000.00",
  "receiveAmount": "0.00078",
  "fee": "10.00",
  "createdAt": "2025-01-15T10:30:00Z",
  "expiresAt": "2025-01-15T10:32:00Z"
}
```

#### `POST /payments`
Create a payment against a valid quote.

**Request:**
```json
{
  "quoteId": "uuid",
  "customerReference": "customer-123"
}
```

#### `POST /payments/{id}/execute`
Execute a pending payment.

#### `GET /payments/{id}`
Get payment details by ID.

#### `POST /payments/{id}/refund`
Refund a completed payment. **This endpoint needs to be implemented.**

#### `GET /payments/{id}/status`
Get the current status of a payment. **This endpoint needs to be implemented.**

## Your Tasks

### 1. Explore & Fix

Get the service running and explore the endpoints. You'll find bugs — some obvious, some subtle. Fix what you find.

### 2. Implement Payment Refund

Build `POST /payments/{id}/refund` that:
- Refunds a payment that is in `COMPLETED` status
- Returns an appropriate error if the payment is still pending, processing, failed, or already refunded
- Write tests for this feature — cover the happy path and the edge cases

### 3. Implement Payment Status Endpoint

Build `GET /payments/{id}/status` that returns:
- Current payment status
- Status change history (when did it transition between states?)
- Quote details associated with the payment

### 4. Improvements

Make any improvements you think are valuable. Be deliberate about what and why.

## VALR Public API

The service uses the VALR public API for live pricing:
- `GET https://api.valr.com/v1/public/{pair}/marketsummary` — Current price for a pair

No authentication required for public endpoints.

## Time Expectation

Spend roughly 6–8 hours. We're looking at how you approach problems, what you prioritise, and how you communicate your decisions — not perfection.

## Submission

Push your solution to a Git repository and share the link. Include a brief `NOTES.md` describing:
- What bugs you found and how you fixed them
- Your approach to the refund and status endpoints
- Test strategy and what you covered
- Any improvements you made and why
- AI usage (how, for what and to what extent)
- Anything you'd do differently with more time
