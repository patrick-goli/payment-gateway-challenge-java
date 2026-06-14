package com.checkout.payment.gateway.controller;


import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.response.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

    mvc.perform(MockMvcRequestBuilders.get("/api/payment/" + payment.id()))
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
    mvc.perform(MockMvcRequestBuilders.get("/api/payment/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Payment ID not found"));
  }


  @Test
  @DisplayName("End-to-end: create payment and retrieve it by id")
  void createAndRetrievePayment() throws Exception {
    // card ending with odd digit → bank authorized
    Map<String, Object> requestBody = Map.of(
        "card_number", "2277497185963469",
        "expiry_month", 12,
        "expiry_year", 2030,
        "currency", "USD",
        "amount", 100,
        "cvv", "123"
    );

    MvcResult postResult = mvc.perform(
            MockMvcRequestBuilders.post("/api/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status", is("Authorized")))
        .andExpect(jsonPath("$.cardNumberLastFour", is("3469")))
        .andExpect(jsonPath("$.currency", is("USD")))
        .andReturn();

    // Extract id from response
    String responseJson = postResult.getResponse().getContentAsString();
    String id = objectMapper.readTree(responseJson).get("id").asText();

    // Retrieve payment
    mvc.perform(MockMvcRequestBuilders.get("/api/payment/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(id)))
        .andExpect(jsonPath("$.status", is("Authorized")))
        .andExpect(jsonPath("$.cardNumberLastFour", is("3469")))
        .andExpect(jsonPath("$.currency", is("USD")))
        .andExpect(jsonPath("$.amount", is(100)));
  }
}
