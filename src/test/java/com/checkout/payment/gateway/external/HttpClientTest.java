package com.checkout.payment.gateway.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.exception.BankProcessingException;
import com.checkout.payment.gateway.model.request.BankRequest;
import com.checkout.payment.gateway.model.response.BankResponse;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class HttpClientTest {

  @Mock
  RestTemplate restTemplate;

  @InjectMocks
  CustomHttpClient customHttpClient;

  @Test
  @DisplayName("getBankData should return body when response is 2xx")
  void getBankDataSuccess() {
    BankRequest request = BankRequest.builder()
        .cardNumber("4242424242424241")
        .expiryDate("12/2030")
        .currency("USD")
        .amount(100)
        .cvv("123")
        .build();

    BankResponse body = BankResponse.builder()
        .authorized(true)
        .authorizationCode(UUID.randomUUID())
        .build();

    when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), any()))
        .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

    BankResponse result = customHttpClient.getBankData("http://bank", request);

    assertThat(result).isEqualTo(body);
  }

  @Test
  @DisplayName("getBankData should throw BankProcessingException on RestTemplate error")
  void getBankDataThrowsOnError() {
    BankRequest request = BankRequest.builder()
        .cardNumber("4242424242424241")
        .expiryDate("12/2030")
        .currency("USD")
        .amount(100)
        .cvv("123")
        .build();

    when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), any()))
        .thenThrow(new RuntimeException("boom"));

    assertThrows(
        BankProcessingException.class,
        () -> customHttpClient.getBankData("http://bank", request)
    );
  }
}