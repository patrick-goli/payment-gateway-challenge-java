package com.checkout.payment.gateway.external;

import com.checkout.payment.gateway.exception.BankProcessingException;
import com.checkout.payment.gateway.model.request.BankRequest;
import com.checkout.payment.gateway.model.response.BankResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class CustomHttpClient {

  private final RestTemplate restTemplate;

  public BankResponse getBankData(String url, BankRequest bankRequest) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<BankRequest> entity = new HttpEntity<>(bankRequest, headers);

    try {
      ResponseEntity<BankResponse> response = restTemplate.postForEntity(url, entity,
          BankResponse.class);

      HttpStatusCode status = response.getStatusCode();

      if (status.is2xxSuccessful()) {
        return response.getBody();
      }

      if (status == HttpStatus.BAD_REQUEST) {
        throw new BankProcessingException(
            HttpStatus.BAD_REQUEST,
            "Invalid request sent to acquiring bank"
        );
      }

      if (status == HttpStatus.SERVICE_UNAVAILABLE) {
        throw new BankProcessingException(HttpStatus.SERVICE_UNAVAILABLE,
            "Acquiring bank is temporarily unavailable");
      }

      throw new BankProcessingException(status,
          "Unexpected response from acquiring bank: " + status.value());

    } catch (BankProcessingException e) {
      throw e;
    } catch (Exception e) {
      throw new BankProcessingException(HttpStatus.SERVICE_UNAVAILABLE,
          "Error while contacting acquiring bank", e);
    }
  }
}