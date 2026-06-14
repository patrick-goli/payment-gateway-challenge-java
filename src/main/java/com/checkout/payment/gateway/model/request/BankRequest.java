package com.checkout.payment.gateway.model.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@ToString(onlyExplicitlyIncluded = true)
public class BankRequest {

  @ToString.Exclude
  private final String cardNumber;

  @ToString.Exclude
  private final String expiryDate;

  @ToString.Include
  private final String currency;

  @ToString.Include
  private final int amount;

  @ToString.Exclude
  private final String cvv;
}