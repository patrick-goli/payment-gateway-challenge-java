package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(PaymentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handlePaymentNotFoundException(PaymentNotFoundException ex,
      HttpServletRequest request) {
    log.warn("Payment ID not found. Method={} path={}", request.getMethod(),
        request.getRequestURI());

    ErrorResponse error = new ErrorResponse(
        HttpStatus.NOT_FOUND.getReasonPhrase(),
        "Payment ID not found"
    );

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(BankProcessingException.class)
  public ResponseEntity<ErrorResponse> handleBankProcessingException(BankProcessingException ex,
      HttpServletRequest request) {
    HttpStatus status = (HttpStatus) ex.getStatus();

    log.error("Bank processing error. Method={} path={} status={}", request.getMethod(),
        request.getRequestURI(), status.value(), ex);

    ErrorResponse error = new ErrorResponse(
        PaymentStatus.REJECTED,
        status.getReasonPhrase(),
        ex.getMessage()
    );

    return ResponseEntity.status(status).body(error);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
      HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    String path = extractPath(request);
    log.warn("Validation error. path={} status={}", path, status.value());

    Map<String, String> errors = new HashMap<>();

    ex.getBindingResult().getAllErrors().forEach(error -> {
      String fieldName;
      if (error instanceof FieldError fieldError) {
        fieldName = fieldError.getField();
      } else {
        fieldName = error.getObjectName();
      }
      errors.put(fieldName, error.getDefaultMessage());
    });

    ErrorResponse errorResponse = new ErrorResponse(
        PaymentStatus.REJECTED,
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        "Validation failed",
        errors
    );

    return handleExceptionInternal(ex, errorResponse, headers, HttpStatus.BAD_REQUEST, request);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
      HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    String path = extractPath(request);
    log.warn("Cannot read body. path={} status={}", path, status.value());

    ErrorResponse errorResponse = new ErrorResponse(
        PaymentStatus.REJECTED,
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        "Malformed or unreadable request body"
    );

    return handleExceptionInternal(ex, errorResponse, headers, HttpStatus.BAD_REQUEST, request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex,
      HttpServletRequest request) {
    log.error("Unexpected error. Method={} path={}", request.getMethod(), request.getRequestURI(),
        ex);

    ErrorResponse error = new ErrorResponse(
        PaymentStatus.REJECTED,
        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
        "An unexpected error occurred"
    );

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  private String extractPath(WebRequest request) {
    if (request instanceof ServletWebRequest servletWebRequest) {
      return servletWebRequest.getRequest().getRequestURI();
    }
    return "N/A";
  }
}