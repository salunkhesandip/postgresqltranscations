package com.cleancoders.postgresqltranscations.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
@Documented
public @interface ValidDateRange {
    String message() default "Created after date must be before or equal to created before date";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

