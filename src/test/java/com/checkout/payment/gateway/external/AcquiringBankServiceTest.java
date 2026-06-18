package com.checkout.payment.gateway.external;

import static com.checkout.payment.gateway.util.TestUtil.validRequestWithOddCardNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.configuration.ApplicationProperties;
import com.checkout.payment.gateway.model.request.BankRequest;
import com.checkout.payment.gateway.model.request.PostPaymentRequest;
import com.checkout.payment.gateway.model.response.BankResponse;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AcquiringBankServiceTest {

  @Mock
  ApplicationProperties applicationProperties;

  @Mock
  CustomHttpClient customHttpClient;

  @InjectMocks
  AcquiringBankService acquiringBankService;

  @Test
  @DisplayName("processPayment should call CustomHttpClient with correct URL and mapped BankRequest")
  void processPaymentBuildsCorrectBankRequest() {
    ApplicationProperties.AcquiringBank bankProps =
        new ApplicationProperties.AcquiringBank("http://bank-simulator/payments");
    when(applicationProperties.acquiringBank()).thenReturn(bankProps);

    PostPaymentRequest request = validRequestWithOddCardNumber();

    BankResponse bankResponse = BankResponse.builder()
        .authorized(true)
        .authorizationCode(UUID.randomUUID())
        .build();

    ArgumentCaptor<BankRequest> bankRequestCaptor = ArgumentCaptor.forClass(BankRequest.class);

    // Capture the BankRequest
    when(customHttpClient.getBankData(eq(bankProps.api()), bankRequestCaptor.capture()))
        .thenReturn(bankResponse);

    BankResponse result = acquiringBankService.processPayment(request);

    // Assert result is passed through
    assertThat(result).isEqualTo(bankResponse);

    // Inspect captured request
    BankRequest sent = bankRequestCaptor.getValue();
    assertThat(sent.getCardNumber()).isEqualTo(request.cardNumber());
    assertThat(sent.getExpiryDate()).isEqualTo(request.expiryDate());
    assertThat(sent.getCurrency()).isEqualTo(request.currency());
    assertThat(sent.getAmount()).isEqualTo(request.amount());
    assertThat(sent.getCvv()).isEqualTo(request.cvv());

    verify(customHttpClient).getBankData(bankProps.api(), sent);
  }
}