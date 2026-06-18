package com.checkout.payment.gateway.service;

import static com.checkout.payment.gateway.util.TestUtil.validRequestWithEvenCardNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {

  @Mock
  PaymentsRepository paymentsRepository;

  @Mock
  IdempotencyRepository idempotencyRepository;

  @Mock
  AcquiringBankService acquiringBankService;

  @InjectMocks
  PaymentGatewayService paymentGatewayService;


  @Test
  @DisplayName("processPayment should map authorized=true to PaymentStatus.AUTHORIZED and store payment")
  void processPaymentAuthorized() {
    PostPaymentRequest request = validRequestWithEvenCardNumber();

    BankResponse bankResponse = BankResponse.builder()
        .authorized(true)
        .build();

    when(acquiringBankService.processPayment(request)).thenReturn(bankResponse);

    PostPaymentResponse response = paymentGatewayService.processPayment(request);

    assertThat(response.status()).isEqualTo(PaymentStatus.AUTHORIZED);
    assertThat(response.cardNumberLastFour()).isEqualTo(request.cardNumberLastFour());
    assertThat(response.currency()).isEqualTo(request.currency());
    assertThat(response.amount()).isEqualTo(request.amount());
    assertThat(response.id()).isNotNull();

    ArgumentCaptor<PostPaymentResponse> captor =
        ArgumentCaptor.forClass(PostPaymentResponse.class);
    verify(paymentsRepository).add(captor.capture());
    assertThat(captor.getValue().id()).isEqualTo(response.id());
  }

  @Test
  @DisplayName("processPayment should map authorized=false to PaymentStatus.DECLINED and store payment")
  void processPaymentDeclined() {
    PostPaymentRequest request = validRequestWithEvenCardNumber();

    BankResponse bankResponse = BankResponse.builder()
        .authorized(false)
        .build();

    when(acquiringBankService.processPayment(request)).thenReturn(bankResponse);

    PostPaymentResponse response = paymentGatewayService.processPayment(request);

    assertThat(response.status()).isEqualTo(PaymentStatus.DECLINED);
    verify(paymentsRepository).add(any(PostPaymentResponse.class));
  }


  @Test
  @DisplayName("processPayment with Idempotency-Key should return same response and avoid second bank call")
  void processPaymentIsIdempotentWithKey() {
    // given
    String idempotencyKey = "test-key-123";
    PostPaymentRequest request = validRequestWithEvenCardNumber();

    BankResponse bankResponse = BankResponse.builder()
        .authorized(true)
        .build();

    when(idempotencyRepository.find(idempotencyKey)).thenReturn(Optional.empty());
    when(acquiringBankService.processPayment(request)).thenReturn(bankResponse);

    ArgumentCaptor<PostPaymentResponse> responseCaptor =
        ArgumentCaptor.forClass(PostPaymentResponse.class);

    // when: first call
    PostPaymentResponse first = paymentGatewayService.processPayment(request, idempotencyKey);

    // the response is now stored for this idempotency key
    when(idempotencyRepository.find(idempotencyKey)).thenReturn(Optional.of(first));

    // when: second call with same key and same request
    PostPaymentResponse second = paymentGatewayService.processPayment(request, idempotencyKey);

    // then: same response returned
    assertThat(second).isEqualTo(first);
    assertThat(second.id()).isEqualTo(first.id());
    assertThat(second.status()).isEqualTo(PaymentStatus.AUTHORIZED);

    // bank called only once
    verify(acquiringBankService, times(1)).processPayment(request);

    // payment stored only once
    verify(paymentsRepository, times(1)).add(responseCaptor.capture());
    PostPaymentResponse stored = responseCaptor.getValue();
    assertThat(stored.id()).isEqualTo(first.id());
  }

  @Test
  @DisplayName("getPaymentById should return stored payment")
  void getPaymentByIdFound() {
    UUID id = UUID.randomUUID();
    PostPaymentResponse stored = PostPaymentResponse.builder()
        .id(id)
        .status(PaymentStatus.AUTHORIZED)
        .cardNumberLastFour("1111")
        .expiryMonth(1)
        .expiryYear(2030)
        .currency("USD")
        .amount(50)
        .build();

    when(paymentsRepository.get(id)).thenReturn(Optional.of(stored));

    PostPaymentResponse result = paymentGatewayService.getPaymentById(id);

    assertThat(result).isEqualTo(stored);
  }

  @Test
  @DisplayName("getPaymentById should throw PaymentNotFoundException when payment does not exist")
  void getPaymentByIdNotFound() {
    UUID id = UUID.randomUUID();
    when(paymentsRepository.get(id)).thenReturn(Optional.empty());

    org.junit.jupiter.api.Assertions.assertThrows(
        PaymentNotFoundException.class,
        () -> paymentGatewayService.getPaymentById(id)
    );
  }
}