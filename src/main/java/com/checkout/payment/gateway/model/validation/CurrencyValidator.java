package com.checkout.payment.gateway.model.validation;


import com.checkout.payment.gateway.configuration.ApplicationProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CurrencyValidator implements ConstraintValidator<ValidCurrency, String> {

  private final Set<String> supportedCurrencies;

  public CurrencyValidator(ApplicationProperties applicationProperties) {
    this.supportedCurrencies = applicationProperties.payment().supportedCurrencies();
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null || value.isBlank()) {
      return true; // @NotBlank handles this
    }

    if (supportedCurrencies.contains(value)) {
      return true;
    }

    context.disableDefaultConstraintViolation();
    context.buildConstraintViolationWithTemplate(
        "Currency must be one of: " + String.join(", ", supportedCurrencies)
    ).addConstraintViolation();

    return false;
  }
}