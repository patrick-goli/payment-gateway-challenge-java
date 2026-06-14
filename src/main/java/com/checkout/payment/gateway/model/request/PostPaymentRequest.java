package com.checkout.payment.gateway.model.request;

import com.checkout.payment.gateway.model.validation.ValidCardNumber;
import com.checkout.payment.gateway.model.validation.ValidCurrency;
import com.checkout.payment.gateway.model.validation.ValidExpiryDate;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@ValidExpiryDate
public record PostPaymentRequest(
    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "\\d{14,19}", message = "Card number must contain between 14 and 19 digits")
    @ValidCardNumber
    String cardNumber,

    @NotNull(message = "Expiry month is required")
    @Min(value = 1, message = "Expiry month must be between 1 and 12")
    @Max(value = 12, message = "Expiry month must be between 1 and 12")
    Integer expiryMonth,

    @NotNull(message = "Expiry year is required")
    @Min(value = 1970, message = "Expiry year must be between 1970 and 3099")
    @Max(value = 3099, message = "Expiry year must be between 1970 and 3099")
    Integer expiryYear,

    @NotBlank(message = "Currency is required")
    @ValidCurrency
    String currency,

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be a positive integer")
    Integer amount,

    @NotBlank(message = "CVV is required")
    @Pattern(regexp = "\\d{3,4}", message = "CVV must be 3 or 4 digits")
    String cvv
) {

  public String cardNumberLastFour() {
    return cardNumber.substring(cardNumber.length() - 4);
  }

  @Override
  public String toString() {
    return "PostPaymentRequest[" +
        "cardNumberLastFour=" + cardNumberLastFour() +
        ", expiryMonth=" + expiryMonth +
        ", expiryYear=" + expiryYear +
        ", currency=" + currency +
        ", amount=" + amount +
        ", cvv=<redacted>]";
  }

  public String expiryDate() {
    return String.format("%d/%d", expiryMonth, expiryYear);
  }
}