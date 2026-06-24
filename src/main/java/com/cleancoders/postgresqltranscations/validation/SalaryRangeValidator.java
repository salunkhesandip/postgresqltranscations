package com.cleancoders.postgresqltranscations.validation;

import com.cleancoders.postgresqltranscations.dto.EmployeeSearchCriteria;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SalaryRangeValidator implements ConstraintValidator<ValidSalaryRange, EmployeeSearchCriteria> {

    @Override
    public boolean isValid(EmployeeSearchCriteria criteria, ConstraintValidatorContext context) {
        if (criteria == null) {
            return true; // Null object is valid (handled by @NotNull if required)
        }
        
        if (criteria.getMinSalary() != null && criteria.getMaxSalary() != null) {
            return criteria.getMinSalary().compareTo(criteria.getMaxSalary()) <= 0;
        }
        
        return true; // Valid if only one or neither is provided
    }
}

