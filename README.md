# Checkout.com Payment Gateway Challenge (Java/Spring Boot)

This project implements a simplified **payment gateway** for an e‑commerce merchant.  
It exposes an API that allows merchants to:

- Process a card payment through the payment gateway.
- Retrieve the details of a previously made payment.

A **bank simulator** is used as the acquiring bank. The gateway validates requests, forwards valid
ones to the simulator, translates responses into domain statuses, and stores only non‑sensitive
payment data.

---

## Overview

### Features

- `POST /api/payment`
  - Validates card details and payment data.
  - If invalid → rejects the request without calling the bank.
  - If valid → constructs a `BankRequest` and calls the bank simulator.
  - Maps bank response to:
    - `Authorized`
    - `Declined`
  - Handles bank unavailability as a **rejected** payment with a clear error.

- `GET /api/payment/{id}`
  - Returns stored payment details:
    - `id`
    - `status` (`Authorized` / `Declined`)
    - `last four card digits`
    - expiry month/year
    - currency
    - amount

### Data & Security

- Only **non‑sensitive** data is stored and returned:
  - Card number → only last four digits are in storage and responses.
  - Full PAN and CVV are never persisted; they are used only to validate and call the bank.
- Request validation is declarative.
- Errors are returned with a consistent JSON shape and a domain `PaymentStatus.REJECTED`.

---

## Key Design Considerations

### 1. Separation of Concerns

The code is structured into clear layers:

- **Controller layer** (`PaymentGatewayController` / `PaymentGatewayControllerAPI`):
  - Handles HTTP, request/response mapping, and delegates to the service layer.
  - Uses `@Valid` on DTOs to trigger Bean Validation.
  - Is annotated with OpenAPI/Swagger metadata for discoverability and examples.

- **Service layer**:
  - `PaymentGatewayService`
    - Orchestrates the payment flow.
    - For `POST`:
      - Calls `AcquiringBankService` with a validated `PostPaymentRequest`.
      - Maps bank `authorized` flag to `PaymentStatus.AUTHORIZED` or `PaymentStatus.DECLINED`.
      - Builds a `PostPaymentResponse`, generates a `UUID`, stores the payment, and returns the
        response.
    - For `GET`:
      - Calls `PaymentsRepository.get(id)` and throws `PaymentNotFoundException` if absent.
  - `AcquiringBankService`
    - Translates internal `PostPaymentRequest` into external `BankRequest` (fields the bank
      expects).
    - Uses `ApplicationProperties` to resolve the bank API URL.
    - Delegates HTTP details to `CustomHttpClient`.

- **External client layer**:
  - `CustomHttpClient`
    - Wraps `RestTemplate` for the bank call.
    - Sends JSON `BankRequest` via POST.
    - Interprets HTTP response:
      - `2xx` → return `BankResponse`.
      - `400` → `BankProcessingException` with status `BAD_REQUEST` and a descriptive message.
      - `503` or transport errors → `BankProcessingException` with status `SERVICE_UNAVAILABLE`.

- **Persistence layer**:
  - `PaymentsRepository` in‑memory map of `PostPaymentResponse` keyed by `UUID`.

### 2. DTOs and Validation

- Request DTO: `PostPaymentRequest` is implemented as a **record** with Bean Validation annotations:
  - `cardNumber`: not blank, numeric, 14–19 digits, plus a custom `@ValidCardNumber` with Luhn
    validation.
  - `expiryMonth`: 1–12.
  - `expiryYear`: between 1970 and 3099.
  - `currency`: not blank, validated by custom `@ValidCurrency`.
  - `amount`: integer, **must be ≥ 1** (minor units).
  - `cvv`: 3–4 digits, numeric.

- Cross-field date validation:
  - `@ValidExpiryDate` on the record.
  - `ExpiryDateValidator` checks `expiryMonth`+`expiryYear` and enforces that the expiry is in
    the future relative to current month/year.

- Responses:
  - `PostPaymentResponse` is also a record containing only non-sensitive data
    and domain `PaymentStatus`.
  - `BankRequest` / `BankResponse` represent the contract with the acquiring bank.

### 3. Error Handling

A global `@ControllerAdvice` (`GlobalExceptionHandler`) centralizes error handling:

- `MethodArgumentNotValidException`:
  - Populates `validationErrors` map with field/object name → message.
  - Returns:
    - HTTP `400 Bad Request`.
    - Domain `PaymentStatus.REJECTED`.
    - Message: `"Validation failed"`.

- `HttpMessageNotReadableException`:
  - For malformed JSON / unreadable body.
  - Returns `400 Bad Request` + `PaymentStatus.REJECTED` with a concise message.

- `PaymentNotFoundException`:
  - When a payment ID is not found in the repository.
  - Returns `404 Not Found` with message `"Payment ID not found"`.

- `BankProcessingException`:
  - Thrown by `CustomHttpClient` when the bank returns an error status or there is a transport
    issue.
  - Carries the HTTP status (`BAD_REQUEST`, `SERVICE_UNAVAILABLE`, etc.).
  - The handler returns:
    - HTTP status from the exception.
    - Domain status `PaymentStatus.REJECTED`.
    - Informative message.

All error responses share a uniform `ErrorResponse` shape.

### 4. Currency Configuration

Supported currencies are **externalized**:

- Supported currencies are in the config parameter : `app.payment.supported-currencies`
- `@ValidCurrency` uses `CurrencyValidator`, which injects `PaymentProperties` and:
  - checks whether the given currency is in the configured set.
  - builds an error message like `"Currency must be one of: CAD, USD, EUR"` dynamically from the
    configuration.

This makes it easy to change supported currencies without code changes and keeps business rules in
configuration.

### 5. Amount Semantics

- `amount` must be **positive** (`@Min(1)`), i.e. zero is not allowed, because a zero-amount payment
  is not a meaningful financial transaction for this gateway.

### 6. Idempotency

To support safe retries, the gateway implements **idempotency** on `POST /api/payment` using an
`Idempotency-Key` header.

- If the client supplies an `Idempotency-Key`:
  - On first request:
    - The payment is processed normally.
    - The resulting `PostPaymentResponse` is stored in an in-memory `IdempotencyRepository`, keyed
      by that header value.
  - On subsequent requests with the same key:
    - The gateway does **not** call the acquiring bank again.
    - It returns the previously stored `PostPaymentResponse` with the same `id` and `status`.

- If the client does **not** supply an idempotency key:
  - Each POST is treated as a new payment request.

**Important**: For a real production system, the idempotency store must be:

- durable (e.g., database or cache with persistence),
- partitioned by merchant/account,
- and have appropriate TTL and collision handling.
  For this use-case, an in-memory map is sufficient.

---

## Assumptions

- Only a **small subset of currencies** is supported (e.g., `USD`, `EUR`, `CAD`), configured in
  `application.yml` under `app.payment.supported-currencies`.
- The bank simulator URL is configured under `app.acquiring-bank.api`.
- Card expiry:
  - A card expiring in the **current month** is still considered valid until the end of the month.
  - Anything strictly before the current `YearMonth` is rejected.
- CVV is used only for validation and bank requests; it is never persisted or returned in responses.
- The in-memory `PaymentsRepository` is sufficient for this coding exercise; a real implementation
  would use a database with proper indexing, encryption, and retention policies.
- Error responses use the domain `PaymentStatus` in addition to HTTP status; clients are expected to
  rely primarily on HTTP status and `validationErrors` for UI behavior.

---

## Testing

### Unit Tests

- `PaymentGatewayServiceTest`
  - Tests:
    - `processPayment`: maps `authorized=true` → `PaymentStatus.AUTHORIZED` and stores payment.
    - `processPayment`: maps `authorized=false` → `PaymentStatus.DECLINED`.
    - `getPaymentById`: returns stored payment.
    - `getPaymentById`: throws `PaymentNotFoundException` when missing.

- `AcquiringBankServiceTest`
  - Verifies:
    - Correct bank URL is used.
    - `BankRequest` is correctly mapped from `PostPaymentRequest`.
    - Response from `CustomHttpClient` is passed through.

- `HttpClientTest`
  - Verifies:
    - Successful 2xx response returns the body.
    - Errors throw `BankProcessingException`.

### Integration Tests

- `PaymentGatewayControllerTest`
  - Starts a Spring Boot application context.
  - Uses `MockMvc` to:
    - Seed `PaymentsRepository`, then `GET /api/payment/{id}`:
      - Verifies `200 OK` and response fields match stored payment.
    - Call `GET /api/payment/{id}` for a missing ID:
      - Verifies `404 Not Found` and `"Payment ID not found"` message.

- `PaymentGatewayIntegrationTest`
  - Full flow (assuming the bank simulator is reachable at the configured URL):
    - `POST /api/payment` with a valid request:
      - Card ending in odd digit → expects `"Authorized"`, last four, amount, etc.
      - Extracts returned `id`.
    - `GET /api/payment/{id}`:
      - Expects the same data as in the POST response.

---

## How to Run

1. **Configure the bank simulator URL and currencies** in `application.yml`:

```yaml
server:
  port: 8090

app:
  acquiring-bank:
    api: http://localhost:8080/payments

  payment:
    supported-currencies:
      - USD
      - EUR
      - CAD
```

2. **Run the application** (e.g., `./gradlew bootRun` or from the IDE).

3. **Run the tests**:

```bash
./gradlew test
```

4. **Swagger UI** (if `springdoc` is enabled):

- Visit: `http://localhost:8090/swagger-ui.html` or `/swagger-ui/index.html`
- Explore.

---

## Next Steps / Possible Extensions

- Add authentication/authorization (e.g., API keys or OAuth2).
- Introduce persistent storage (e.g., PostgreSQL) instead of in-memory repository.
- Persists the idempotency keys.
  - validate that the same key with a different request body is treated as an error
- Support more currencies.

# Instructions for candidates

This is the Java version of the Payment Gateway challenge. If you haven't already read this [README.md](https://github.com/cko-recruitment/) on the details of this exercise, please do so now.

## Requirements
- JDK 17
- Docker

## Template structure

src/ - A skeleton SpringBoot Application

test/ - Some simple JUnit tests

imposters/ - contains the bank simulator configuration. Don't change this

.editorconfig - don't change this. It ensures a consistent set of rules for submissions when reformatting code

docker-compose.yml - configures the bank simulator


## API Documentation
For documentation openAPI is included, and it can be found under the following url: **http://localhost:8090/swagger-ui/index.html**

**Feel free to change the structure of the solution, use a different library etc.**