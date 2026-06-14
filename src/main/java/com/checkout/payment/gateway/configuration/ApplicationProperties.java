package com.checkout.payment.gateway.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app")
@Validated
public record ApplicationProperties(@Valid AcquiringBank acquiringBank, @Valid Payment payment) {

  public record AcquiringBank(
      @NotBlank(message = "Acquiring bank api must not be blank")
      String api
  ) {

  }

  public record Payment(
      @NotEmpty(message = "app.payment.supported-currencies must not be empty")
      Set<String> supportedCurrencies
  ) {

  }
}
