package com.cleancoders.postgresqltranscations.validation;

import com.cleancoders.postgresqltranscations.dto.EmployeeSearchCriteria;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, EmployeeSearchCriteria> {

    @Override
    public boolean isValid(EmployeeSearchCriteria criteria, ConstraintValidatorContext context) {
        if (criteria == null) {
            return true;
        }

        if (criteria.getCreatedAfter() != null && criteria.getCreatedBefore() != null) {
            return !criteria.getCreatedAfter().isAfter(criteria.getCreatedBefore());
        }

        return true; // Valid if only one or neither is provided
    }
}

