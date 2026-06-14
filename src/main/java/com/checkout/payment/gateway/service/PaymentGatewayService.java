package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.external.AcquiringBankService;
import com.checkout.payment.gateway.model.request.PostPaymentRequest;
import com.checkout.payment.gateway.model.response.BankResponse;
import com.checkout.payment.gateway.model.response.PostPaymentResponse;
import com.checkout.payment.gateway.repository.IdempotencyRepository;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentGatewayService {

  private final PaymentsRepository paymentsRepository;
  private final IdempotencyRepository idempotencyRepository;
  private final AcquiringBankService acquiringBankService;


  public PostPaymentResponse getPaymentById(UUID id) {
    log.debug("Requesting payment details with ID {}", id);
    return paymentsRepository.get(id).orElseThrow(() -> new PaymentNotFoundException(id));
  }

  public PostPaymentResponse processPayment(PostPaymentRequest paymentRequest) {
    // backward-compatible method for null idempotency header
    return processPayment(paymentRequest, null);
  }

  public PostPaymentResponse processPayment(PostPaymentRequest paymentRequest,
      String idempotencyKey) {
    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      // If we've seen this key before, return the same response
      Optional<PostPaymentResponse> existing = idempotencyRepository.find(idempotencyKey);
      if (existing.isPresent()) {
        log.debug("Idempotent payment hit for key={}", idempotencyKey);
        return existing.get();
      }
    }

    // Normal processing
    BankResponse bankResponse = acquiringBankService.processPayment(paymentRequest);

    PaymentStatus status = bankResponse.isAuthorized() ? PaymentStatus.AUTHORIZED
        : PaymentStatus.DECLINED;

    PostPaymentResponse response = PostPaymentResponse.builder()
        .id(UUID.randomUUID())
        .status(status)
        .cardNumberLastFour(paymentRequest.cardNumberLastFour())
        .expiryMonth(paymentRequest.expiryMonth())
        .expiryYear(paymentRequest.expiryYear())
        .currency(paymentRequest.currency())
        .amount(paymentRequest.amount())
        .build();

    paymentsRepository.add(response);

    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      idempotencyRepository.save(idempotencyKey, response);
    }
    log.debug("Payment processed with status={} id={}", status, response.id());
    return response;
  }
}
