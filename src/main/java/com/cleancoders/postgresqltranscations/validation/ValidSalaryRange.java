package com.cleancoders.postgresqltranscations.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SalaryRangeValidator.class)
@Documented
public @interface ValidSalaryRange {
    String message() default "Minimum salary must be less than or equal to maximum salary";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

