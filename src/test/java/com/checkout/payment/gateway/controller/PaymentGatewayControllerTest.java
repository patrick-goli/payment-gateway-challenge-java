package com.checkout.payment.gateway.controller;


import static com.checkout.payment.gateway.util.TestUtil.validRequestWithOddCardNumber;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.request.PostPaymentRequest;
import com.checkout.payment.gateway.model.response.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  public static final String API_PAYMENTS = "/api/payments";
  @Autowired
  PaymentsRepository paymentsRepository;

  @Autowired
  private MockMvc mvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    PostPaymentResponse payment = new PostPaymentResponse(
        UUID.randomUUID(),
        PaymentStatus.AUTHORIZED,
        "4321",
        12,
        2024, "USD",
        10
    );

    paymentsRepository.add(payment);

    mvc.perform(MockMvcRequestBuilders.get(API_PAYMENTS + "/" + payment.id()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(payment.status().getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(payment.cardNumberLastFour()))
        .andExpect(jsonPath("$.expiryMonth").value(payment.expiryMonth()))
        .andExpect(jsonPath("$.expiryYear").value(payment.expiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.currency()))
        .andExpect(jsonPath("$.amount").value(payment.amount()));
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get(API_PAYMENTS + "/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Payment ID not found"));
  }

  @Test
  @DisplayName("End-to-end: create payment and retrieve it by id")
  void createAndRetrievePayment() throws Exception {
    // card ending with odd digit → bank authorized
    PostPaymentRequest paymentRequest = validRequestWithOddCardNumber();

    MvcResult postResult = mvc.perform(
            MockMvcRequestBuilders.post(API_PAYMENTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status", is("Authorized")))
        .andExpect(jsonPath("$.cardNumberLastFour", is(paymentRequest.cardNumberLastFour())))
        .andExpect(jsonPath("$.currency", is(paymentRequest.currency())))
        .andReturn();

    // Extract id from response
    String responseJson = postResult.getResponse().getContentAsString();
    String id = objectMapper.readTree(responseJson).get("id").asText();
    // Verify Location header
    String location = postResult.getResponse().getHeader("Location");
    Assertions.assertNotNull(location);
    Assertions.assertTrue(location.endsWith(API_PAYMENTS + "/" + id));

    // Retrieve payment
    mvc.perform(MockMvcRequestBuilders.get(API_PAYMENTS + "/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(id)))
        .andExpect(jsonPath("$.status", is("Authorized")))
        .andExpect(jsonPath("$.cardNumberLastFour", is(paymentRequest.cardNumberLastFour())))
        .andExpect(jsonPath("$.currency", is(paymentRequest.currency())))
        .andExpect(jsonPath("$.amount", is(paymentRequest.amount())));
  }

  @ParameterizedTest
  @MethodSource("com.checkout.payment.gateway.util.TestUtil#invalidPostRequests")
  @DisplayName("POST /api/payments returns 400 and validationErrors for invalid body")
  void createPaymentInvalidBodyReturnsBadRequestWithValidationErrors(
      PostPaymentRequest requestBody)
      throws Exception {

    mvc.perform(
            MockMvcRequestBuilders.post(API_PAYMENTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("Rejected"))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.validationErrors").isMap());
  }
}
