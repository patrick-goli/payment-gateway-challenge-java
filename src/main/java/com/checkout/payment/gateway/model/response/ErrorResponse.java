package com.checkout.payment.gateway.model.response;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.time.LocalDateTime;
import java.util.Map;


@JsonInclude(value = Include.NON_NULL, content = Include.NON_EMPTY)
public record ErrorResponse(

    PaymentStatus status,
    String error,
    String message,
    LocalDateTime timestamp,
    Map<String, String> validationErrors) {

  public ErrorResponse(String message) {
    this(null, null, message, LocalDateTime.now(), null);
  }

  public ErrorResponse(String error, String message) {
    this(null, error, message, LocalDateTime.now(), null);
  }

  public ErrorResponse(PaymentStatus status, String error, String message) {
    this(status, error, message, LocalDateTime.now(), null);
  }

  public ErrorResponse(PaymentStatus status, String error, String message,
      Map<String, String> validationErrors) {
    this(status, error, message, LocalDateTime.now(), validationErrors);
  }
}
