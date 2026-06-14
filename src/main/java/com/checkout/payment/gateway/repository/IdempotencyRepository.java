package com.checkout.payment.gateway.repository;

import com.checkout.payment.gateway.model.response.PostPaymentResponse;
import java.util.Optional;

public interface IdempotencyRepository {

  Optional<PostPaymentResponse> find(String key);

  void save(String key, PostPaymentResponse response);
}