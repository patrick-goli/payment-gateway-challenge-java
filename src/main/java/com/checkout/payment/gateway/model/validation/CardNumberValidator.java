package com.checkout.payment.gateway.model.validation;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CardNumberValidator implements ConstraintValidator<ValidCardNumber, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null || value.isBlank()) {
      return true;
    }

    if (!value.matches("\\d{14,19}")) {
      return false;
    }

    return isLuhnValid(value);
  }

  private boolean isLuhnValid(String number) {
    int sum = 0;
    boolean alternate = false;

    for (int i = number.length() - 1; i >= 0; i--) {
      int n = number.charAt(i) - '0';

      if (alternate) {
        n *= 2;
        if (n > 9) {
          n -= 9;
        }
      }

      sum += n;
      alternate = !alternate;
    }

    return sum % 10 == 0;
  }
}
