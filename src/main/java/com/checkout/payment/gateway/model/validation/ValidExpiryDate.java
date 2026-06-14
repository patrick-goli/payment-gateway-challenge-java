package com.checkout.payment.gateway.model.validation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Constraint(validatedBy = ExpiryDateValidator.class)
public @interface ValidExpiryDate {

  String message() default "Card expiry date must be in the future";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}