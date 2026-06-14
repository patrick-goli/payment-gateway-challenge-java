package com.checkout.payment.gateway.external;

import com.checkout.payment.gateway.configuration.ApplicationProperties;
import com.checkout.payment.gateway.model.request.BankRequest;
import com.checkout.payment.gateway.model.request.PostPaymentRequest;
import com.checkout.payment.gateway.model.response.BankResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AcquiringBankService {

  private final ApplicationProperties applicationProperties;
  private final CustomHttpClient customHttpClient;

  public BankResponse processPayment(PostPaymentRequest request) {
    BankRequest bankRequest = BankRequest.builder()
        .cardNumber(request.cardNumber())
        .expiryDate(request.expiryDate())
        .currency(request.currency())
        .amount(request.amount())
        .cvv(request.cvv())
        .build();

    log.debug("New payment request to bank amount={} currency={}", request.amount(),
        request.currency());

    BankResponse response = customHttpClient.getBankData(
        applicationProperties.acquiringBank().api(),
        bankRequest
    );
    log.debug("Bank payment response authorized={}", response.isAuthorized());
    return response;
  }
}