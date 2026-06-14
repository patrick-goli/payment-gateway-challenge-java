package com.checkout.payment.gateway.repository;

import com.checkout.payment.gateway.model.response.PostPaymentResponse;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryIdempotencyRepository implements IdempotencyRepository {

  private final Map<String, PostPaymentResponse> storage = new ConcurrentHashMap<>();

  @Override
  public Optional<PostPaymentResponse> find(String key) {
    return Optional.ofNullable(storage.get(key));
  }

  @Override
  public void save(String key, PostPaymentResponse response) {
    storage.putIfAbsent(key, response);
  }
}