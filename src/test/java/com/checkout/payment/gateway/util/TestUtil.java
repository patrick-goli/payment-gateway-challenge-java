package com.checkout.payment.gateway.util;

import com.checkout.payment.gateway.model.request.PostPaymentRequest;
import java.time.LocalDate;
import java.util.stream.Stream;

public class TestUtil {


  public static PostPaymentRequest validRequestWithEvenCardNumber() {
    return PostPaymentRequest.builder()
        .cardNumber("6454772338190988")
        .expiryMonth(12)
        .expiryYear(LocalDate.now().plusYears(2).getYear())
        .currency("USD")
        .amount(100)
        .cvv("123")
        .build();
  }


  public static PostPaymentRequest validRequestWithOddCardNumber() {

    return new PostPaymentRequest(
        "2277497185963469",
        12,
        2030,
        "USD",
        100,
        "123"
    );
  }

  public static Stream<PostPaymentRequest> invalidPostRequests() {
    return Stream.of(
        // Missing card_number
        new PostPaymentRequest(
            null,
            12,
            2030,
            "USD",
            100,
            "123"
        )
        ,
        // Invalid card_number format
        new PostPaymentRequest(
            "1234",
            12,
            2030,
            "USD",
            100,
            "123"
        ),
        // Unsupported currency
        new PostPaymentRequest(
            "2277497185963469",
            12,
            2030,
            "ZZZZ",
            100,
            "123"
        ),
        // Negative amount
        new PostPaymentRequest(
            "2277497185963469",
            12,
            2030,
            "EUR",
            -1,
            "123"
        ),
        // Invalid date in the past
        new PostPaymentRequest(
            "2277497185963469",
            12,
            LocalDate.now().minusYears(1).getYear(),
            "USD",
            100,
            "123"
        ),
        // Invalid date 2
        new PostPaymentRequest(
            "2277497185963469",
            LocalDate.now().minusMonths(1).getMonthValue(),
            LocalDate.now().getYear(),
            "EUR",
            100,
            "123"
        )
    );
  }

}
