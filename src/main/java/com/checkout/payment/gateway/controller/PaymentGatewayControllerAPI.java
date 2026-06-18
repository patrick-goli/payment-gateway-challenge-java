package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.model.request.PostPaymentRequest;
import com.checkout.payment.gateway.model.response.PostPaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/payments")
@Tag(name = "Payment Gateway", description = "Process and retrieve card payments")
public interface PaymentGatewayControllerAPI {

  @Operation(
      summary = "Get payment details by ID",
      description = "Returns the details of a previously processed payment, including status and masked card information."
  )
  @ApiResponse(
      responseCode = "201",
      description = "Payment found",
      content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = PostPaymentResponse.class),
          examples = @ExampleObject(
              name = "PaymentFound",
              summary = "Authorized payment example",
              value = """
                  {
                    "id": "f0e9c8d7-1234-4b56-9abc-0123456789ab",
                    "status": "Authorized",
                    "cardNumberLastFour": "4241",
                    "expiryMonth": 12,
                    "expiryYear": 2030,
                    "currency": "USD",
                    "amount": 100
                  }
                  """
          )
      )
  )
  @ApiResponse(
      responseCode = "404",
      description = "Payment not found",
      content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(
              name = "PaymentNotFound",
              value = """
                  {
                    "error": "Not Found",
                    "message": "Payment ID not found",
                    "timestamp": "2026-06-14T17:22:10.4969087"
                  }
                  """
          )
      )
  )
  @GetMapping("/{id}")
  ResponseEntity<PostPaymentResponse> getPostPaymentEventById(
      @Parameter(
          description = "Payment identifier returned when the payment was created",
          required = true,
          example = "f0e9c8d7-1234-4b56-9abc-0123456789ab"
      )
      @PathVariable UUID id
  );

  @Operation(
      summary = "Create a new payment",
      description = """
          Processes a card payment through the payment gateway and returns its resulting status.
          - If the request is valid and the acquiring bank simulator authorizes the payment, status will be `Authorized`.
          - If the request is valid but the bank simulator declines, status will be `Declined`.
          - If the request is invalid, the payment is rejected before calling the bank.
          
          Idempotency:
          - If provided an `Idempotency-Key` header, repeated requests with the same key and identical body will return the same payment response.
          - This ensures clients can safely retry a payment request without creating duplicate payments.
          """
  )
  @ApiResponse(
      responseCode = "200",
      description = "Payment processed successfully",
      content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = PostPaymentResponse.class),
          examples = @ExampleObject(
              name = "AuthorizedPayment",
              summary = "Successful authorization",
              value = """
                  {
                    "id": "c2f4d3b1-5678-4a9c-8def-234567890abc",
                    "status": "Authorized",
                    "cardNumberLastFour": "4241",
                    "expiryMonth": 12,
                    "expiryYear": 2030,
                    "currency": "USD",
                    "amount": 100
                  }
                  """
          )
      )
  )
  @ApiResponse(
      responseCode = "400",
      description = "Validation error in the request body",
      content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(
              name = "ValidationError",
              value = """
                  {
                    "status": "Rejected",
                    "error": "Bad Request",
                    "message": "Validation failed",
                    "timestamp": "2026-06-14T17:22:10.4969087",
                    "validationErrors": {
                      "cardNumber": "Card number is invalid",
                      "currency": "Currency must be one of: CAD, USD, EUR"
                    }
                  }
                  """
          )
      )
  )
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      description = """
          Processes a card payment through the payment gateway and returns its resulting status.
          - If the request is valid and the acquiring bank simulator authorizes the payment, status will be `Authorized`.
          - If the request is valid but the bank simulator declines, status will be `Declined`.
          - If the request is invalid, the payment is rejected before calling the bank.
          
          Idempotency:
          - If provided an `Idempotency-Key` header, repeated requests with the same key and identical body will return the same payment response.
          - This ensures clients can safely retry a payment request without creating duplicate payments.
          """,
      content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = PostPaymentRequest.class),
          examples = {
              @ExampleObject(
                  name = "AuthorizedExample",
                  summary = "Card ending in odd digit - authorized",
                  value = """
                      {
                        "card_number": "36503510233365",
                        "expiry_month": 12,
                        "expiry_year": 2030,
                        "currency": "USD",
                        "amount": 100,
                        "cvv": "9123"
                      }
                      """
              ),
              @ExampleObject(
                  name = "DeclinedExample",
                  summary = "Card ending in even digit - declined",
                  value = """
                      {
                        "card_number": "6454772338190988",
                        "expiry_month": 12,
                        "expiry_year": 2030,
                        "currency": "USD",
                        "amount": 100,
                        "cvv": "123"
                      }
                      """
              )
          }
      )
  )
  @PostMapping
  ResponseEntity<PostPaymentResponse> createPayment(
      @Parameter(
          description = "Idempotency key to ensure that retries do not create duplicate payments. "
              + "Must be unique per logical payment attempt.",
          required = false,
          example = "e3b0c442-98fc-1c14-9afb-f4c8996fb924"
      )
      @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody PostPaymentRequest request);
}