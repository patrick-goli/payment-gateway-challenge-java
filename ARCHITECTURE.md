# Architecture

This document describes the internal architecture of the Payment Gateway implementation: modules,
layers, important classes, and key flows.

---

## High-Level View

The system is a **single Spring Boot service** that acts as a payment gateway between merchants and
an acquiring bank simulator.

Main responsibilities:

1. Validate incoming payment requests.
2. Translate requests into the acquiring bank’s expected format.
3. Call the bank simulator and interpret its responses.
4. Persist and expose payment details for later retrieval.

Key components:

- **API layer**: REST controllers and OpenAPI documentation.
- **Application / domain layer**: services and domain models.
- **Integration layer**: HTTP client and bank-specific models.
- **Persistence layer**:
  - `PaymentsRepository` – stores payments by `UUID`.
  - `IdempotencyRepository` – stores `Idempotency-Key` → `PostPaymentResponse` mappings for
    idempotent POST requests.
- **Configuration & validation**: configuration properties, validators.
- **Error handling**: global exception handler.

---

## Package Structure

Simplified:

- `com.checkout.payment.gateway.controller`
  - `PaymentGatewayControllerAPI`
  - `PaymentGatewayController`

- `com.checkout.payment.gateway.service`
  - `PaymentGatewayService`

- `com.checkout.payment.gateway.external`
  - `AcquiringBankService`
  - `CustomHttpClient`

- `com.checkout.payment.gateway.model.request`
  - `PostPaymentRequest`
  - `BankRequest`

- `com.checkout.payment.gateway.model.response`
  - `PostPaymentResponse`
  - `BankResponse`
  - `ErrorResponse`

- `com.checkout.payment.gateway.repository`
  - `PaymentsRepository`

- `com.checkout.payment.gateway.configuration`
  - `ApplicationProperties`
  - `PaymentProperties`
  - HTTP client configuration (`RestTemplate` bean)

- `com.checkout.payment.gateway.validation`
  - `ValidExpiryDate`, `ExpiryDateValidator`
  - `ValidCardNumber`, `CardNumberValidator`
  - `ValidCurrency`, `CurrencyValidator`

- `com.checkout.payment.gateway.exception`
  - `PaymentNotFoundException`
  - `BankProcessingException`
  - `GlobalExceptionHandler`

---

## Domain Model

At the core is a **payment** concept:

- Payment status:
  - `AUTHORIZED`: payment authorized by the bank.
  - `DECLINED`: payment declined by the bank.
  - `REJECTED`: gateway rejected the request (invalid input or bank error).

Outbound representation:

- `PostPaymentResponse`:
  - `UUID id`
  - `PaymentStatus status`
  - `String cardNumberLastFour`
  - `int expiryMonth`
  - `int expiryYear`
  - `String currency`
  - `int amount` (minor units)

Inbound representation:

- `PostPaymentRequest`:
  - Fields from the spec and validation annotations.
  - Derived methods:
    - `cardNumberLastFour()`
    - `expiryDate()` (for bank request formatting)

The domain status is distinct from HTTP status codes to clearly differentiate business outcome from
transport-level success/error.

---

## Request Flow: Process Payment

`POST /api/payment`:

1. **Controller** (`PaymentGatewayController` implementing `PaymentGatewayControllerAPI`):

- Accepts `PostPaymentRequest` as JSON.
- Annotated with `@Valid` to trigger Bean Validation.
- Delegates to `PaymentGatewayService.processPayment`.

2. **Validation**:

- Bean Validation runs:
  - Field constraints (`@NotBlank`, `@Min`, `@Pattern`, etc.).
  - Custom constraints:
    - `@ValidCardNumber` → `CardNumberValidator` (Luhn + length).
    - `@ValidCurrency` → `CurrencyValidator` (against configured currencies).
    - `@ValidExpiryDate` → `ExpiryDateValidator` (cross-field date > now).

- On failure, a `MethodArgumentNotValidException` is thrown and handled by `GlobalExceptionHandler`.


3. **Service layer** (`PaymentGatewayService.processPayment(request, idempotencyKey)`):

- If `idempotencyKey` is present and non-blank:
  - Look up existing response in `IdempotencyRepository`.
  - If found, return it immediately without calling the bank (idempotent hit).
- Otherwise (or if not found):
  - Call `AcquiringBankService.processPayment(request)`.
  - Map `authorized` → `PaymentStatus.AUTHORIZED`/`DECLINED`.
  - Build and persist `PostPaymentResponse`.
  - If `idempotencyKey` was provided:
    - Store the response in `IdempotencyRepository` for future reuse.

4. **External call** (`AcquiringBankService` and `CustomHttpClient`):

- `AcquiringBankService`:
  - Builds a `BankRequest`:
    - `card_number`: full PAN.
    - `expiry_date`: serialized as `"MM/YYYY"`.
    - `currency`, `amount`, `cvv`.
  - Gets bank URL from `ApplicationProperties.acquiringBank().api()`.
  - Calls `CustomHttpClient.getBankData(url, bankRequest)`.

- `CustomHttpClient`:
  - Uses `RestTemplate` to POST JSON to the bank simulator.
  - Interprets the `ResponseEntity<BankResponse>`:
    - `2xx` → returns `BankResponse`.
    - `400` → throws `BankProcessingException(HttpStatus.BAD_REQUEST, ...)`.
    - `503`/other errors → throws `BankProcessingException(HttpStatus.SERVICE_UNAVAILABLE, ...)`.

5. **Bank response interpretation** (`PaymentGatewayService`):

- Receives `BankResponse` from `AcquiringBankService`.
- Maps:
  - `authorized = true` → `PaymentStatus.AUTHORIZED`.
  - `authorized = false` → `PaymentStatus.DECLINED`.
- Creates a new `UUID` for the payment.
- Builds `PostPaymentResponse` with:
  - `id`, `status`, last four digits, expiry, currency, amount.
- Stores it via `PaymentsRepository.add(response)`.
- Returns `PostPaymentResponse` to the controller.

6. **Controller response**:

- Returns `200 OK` with the `PostPaymentResponse` JSON body.

---

## Request Flow: Retrieve Payment

`GET /api/payment/{id}`:

1. Controller calls `PaymentGatewayService.getPaymentById(id)`.
2. Service:

- `paymentsRepository.get(id)` returns `Optional<PostPaymentResponse>`.
- If present → returns the stored response.
- If empty → throws `PaymentNotFoundException(id)`.

3. `GlobalExceptionHandler` maps:

- `PaymentNotFoundException` → `404 Not Found` with an `ErrorResponse("Payment ID not found")`.

---

## Error Model

`ErrorResponse` record:

- `PaymentStatus status`
- `String error` (usually HTTP reason phrase)
- `String message`
- `LocalDateTime timestamp`
- `Map<String, String> validationErrors`

Convenience constructors let handlers build errors with different degrees of detail, always adding a
`timestamp` automatically.

### Error Scenarios

- **Validation errors** (`MethodArgumentNotValidException`):
  - `status`: `REJECTED`
  - `error`: `"Bad Request"`
  - `message`: `"Validation failed"`
  - `validationErrors`: field/object → message.

- **Unreadable JSON** (`HttpMessageNotReadableException`):
  - `status`: `REJECTED`
  - `error`: `"Bad Request"`
  - `message`: short message from exception, logged with method and path.

- **Payment not found** (`PaymentNotFoundException`):
  - `status`: `null` (or `REJECTED`, depending on design choice).
  - `error`: `"Not Found"`
  - `message`: `"Payment ID not found"`

- **Acquiring bank issues** (`BankProcessingException`):
  - `status`: `REJECTED`
  - `error`: from `ex.getStatus().getReasonPhrase()` (e.g. `"Bad Request"`,
    `"Service Unavailable"`).
  - `message`: descriptive message set by `CustomHttpClient`.

The error model is intentionally simple to keep clients’ handling straightforward.

---

## Configuration & Properties

Two main configuration classes:

- `ApplicationProperties` (`@ConfigurationProperties(prefix = "app")`):
  - Nested `AcquiringBank` record with:
    - `String api` – base URL for the bank simulator.

- `PaymentProperties` (`@ConfigurationProperties(prefix = "app.payment")`, `@Validated`):
  - `Set<String> supportedCurrencies` – used by `CurrencyValidator`.

Both are bound from `application.yml` and validated at startup.

Example snippet:

```yaml
app:
  acquiring-bank:
    api: http://localhost:8080/payments

  payment:
    supported-currencies:
      - USD
      - EUR
      - CAD
```

---

## Testing Architecture

- **Unit tests**:
  - Use JUnit 5 + Mockito.
  - Each service and validator is tested in isolation:
    - Mocks collaborators.
    - Focuses on business logic and mapping.

- **Integration tests**:
  - Use `@SpringBootTest` + `@AutoConfigureMockMvc`.
  - Boot an application context with in‑memory repository and real validation.
  - Exercise full HTTP path (controller → validation → service → repository) and error handlers.

If needed, a separate profile can decouple tests from the real bank simulator by mocking
`CustomHttpClient` or pointing `app.acquiring-bank.api` to a local stub.

---

## Trade-Offs

- The implementation uses a simple in‑memory repository to satisfy the challenge and keep focus on
  API design, validation, and external communication.
- The gateway is synchronous; no message queues or asynchronous workflows are introduced, as the
  requirements call for simple request/response processing.
- Domain modeling is intentionally minimal (DTO + status enum) rather than full DDD; this keeps the
  code concise and easier to review during an interview while still respecting clean boundaries.
