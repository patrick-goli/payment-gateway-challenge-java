package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


  @ExceptionHandler(PaymentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handlePaymentNotFoundException(PaymentNotFoundException ex,
      HttpServletRequest request) {
    log.warn("Payment ID not found. Method={} path={}", request.getMethod(),
        request.getRequestURI());

    var error = new ErrorResponse("Payment ID not found");
    return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException ex,
      HttpServletRequest request
  ) {
    log.error("Validation error. Method={} path={}", request.getMethod(), request.getRequestURI());

    Map<String, String> errors = new HashMap<>();

    ex.getBindingResult().getAllErrors().forEach(error -> {
      String fieldName;
      if (error instanceof FieldError fieldError) {
        fieldName = fieldError.getField();
      } else {
        fieldName = error.getObjectName();
      }
      String message = error.getDefaultMessage();
      errors.put(fieldName, message);
    });

    ErrorResponse error = new ErrorResponse(
        PaymentStatus.REJECTED,
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        "Validation failed",
        errors
    );

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    log.warn("Cannot read body. Method {} path={} status={}",
        request.getMethod(), request.getRequestURI(), HttpStatus.BAD_REQUEST.value());
    var error = new ErrorResponse(PaymentStatus.REJECTED, HttpStatus.BAD_REQUEST.getReasonPhrase(),
        ex.getMessage());
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }


  @ExceptionHandler(BankProcessingException.class)
  public ResponseEntity<ErrorResponse> handleBankProcessingException(BankProcessingException ex,
      HttpServletRequest request) {
    HttpStatus status = (HttpStatus) ex.getStatus();

    log.error("Bank processing error. Method={} path={} status={}",
        request.getMethod(), request.getRequestURI(), status.value());

    ErrorResponse error = new ErrorResponse(
        PaymentStatus.REJECTED,
        status.getReasonPhrase(),
        ex.getMessage()
    );

    return ResponseEntity.status(status).body(error);
  }
}
