package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.model.request.PostPaymentRequest;
import com.checkout.payment.gateway.model.response.PostPaymentResponse;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PaymentGatewayController implements PaymentGatewayControllerAPI {

  private final PaymentGatewayService paymentGatewayService;

  @Override
  public ResponseEntity<PostPaymentResponse> getPostPaymentEventById(UUID id) {
    return new ResponseEntity<>(paymentGatewayService.getPaymentById(id), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<PostPaymentResponse> createPayment(String idempotencyKey,
      PostPaymentRequest request) {
    PostPaymentResponse response = paymentGatewayService.processPayment(request, idempotencyKey);
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }
}
