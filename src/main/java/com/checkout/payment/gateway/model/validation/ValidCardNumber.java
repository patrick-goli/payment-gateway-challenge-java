package com.checkout.payment.gateway.model.validation;


import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Target(FIELD)
@Retention(RUNTIME)
@Constraint(validatedBy = CardNumberValidator.class)
public @interface ValidCardNumber {

  String message() default "Card number is invalid";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}