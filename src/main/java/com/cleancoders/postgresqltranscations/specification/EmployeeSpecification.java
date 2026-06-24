package com.cleancoders.postgresqltranscations.specification;

import com.cleancoders.postgresqltranscations.entity.Employee;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

public class EmployeeSpecification {

    /**
     * Specification for case-insensitive partial name matching.
     * Returns null if name is null or blank (no filter applied).
     */
    public static Specification<Employee> hasName(String name) {
        return (root, query, criteriaBuilder) -> {
            if (name == null || name.isBlank()) {
                return null; // No filter
            }
            String lowerName = name.trim().toLowerCase();
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get("empName")),
                "%" + lowerName + "%"
            );
        };
    }

    /**
     * Specification for salary range filtering.
     * Supports minSalary only, maxSalary only, or both (BETWEEN).
     */
    public static Specification<Employee> hasSalaryBetween(BigDecimal minSalary, BigDecimal maxSalary) {
        return (root, query, criteriaBuilder) -> {
            if (minSalary != null && maxSalary != null) {
                return criteriaBuilder.between(root.get("empSalary"), minSalary, maxSalary);
            } else if (minSalary != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("empSalary"), minSalary);
            } else if (maxSalary != null) {
                return criteriaBuilder.lessThanOrEqualTo(root.get("empSalary"), maxSalary);
            }
            return null; // No filter
        };
    }

    /**
     * Specification for creation date range filtering.
     * Supports createdAfter only, createdBefore only, or both (BETWEEN).
     */
    public static Specification<Employee> hasCreatedDateBetween(LocalDate createdAfter, LocalDate createdBefore) {
        return (root, query, criteriaBuilder) -> {
            if (createdAfter != null && createdBefore != null) {
                return criteriaBuilder.between(root.get("empCreatedDate"), createdAfter, createdBefore);
            } else if (createdAfter != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("empCreatedDate"), createdAfter);
            } else if (createdBefore != null) {
                return criteriaBuilder.lessThanOrEqualTo(root.get("empCreatedDate"), createdBefore);
            }
            return null; // No filter
        };
    }
}

