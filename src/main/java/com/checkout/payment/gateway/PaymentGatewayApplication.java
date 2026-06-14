package com.checkout.payment.gateway;

import com.checkout.payment.gateway.configuration.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties.class)
public class PaymentGatewayApplication {

  public static void main(String[] args) {
    SpringApplication.run(PaymentGatewayApplication.class, args);
  }

}
