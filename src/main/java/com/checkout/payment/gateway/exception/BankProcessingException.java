package com.checkout.payment.gateway.exception;


import org.springframework.http.HttpStatusCode;

public class BankProcessingException extends RuntimeException {

  private final HttpStatusCode status;

  public BankProcessingException(HttpStatusCode status, String message) {
    super(message);
    this.status = status;
  }

  public BankProcessingException(HttpStatusCode status, String message, Throwable cause) {
    super(message, cause);
    this.status = status;
  }

  public HttpStatusCode getStatus() {
    return status;
  }
}