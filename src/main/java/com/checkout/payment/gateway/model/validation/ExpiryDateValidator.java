package com.checkout.payment.gateway.model.validation;


import com.checkout.payment.gateway.model.request.PostPaymentRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.YearMonth;

public class ExpiryDateValidator implements
    ConstraintValidator<ValidExpiryDate, PostPaymentRequest> {

  @Override
  public boolean isValid(PostPaymentRequest value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }

    Integer month = value.expiryMonth();
    Integer year = value.expiryYear();

    if (month == null || year == null) {
      return true;
    }

    YearMonth expiry;
    try {
      expiry = YearMonth.of(year, month);
    } catch (RuntimeException ex) {
      return false;
    }

    YearMonth now = YearMonth.now();

    return !expiry.isBefore(now);
  }
}