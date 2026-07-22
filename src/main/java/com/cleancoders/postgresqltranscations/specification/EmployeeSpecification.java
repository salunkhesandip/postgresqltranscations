package com.cleancoders.postgresqltranscations.specification;

import com.cleancoders.postgresqltranscations.entity.Employee;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

/// Static factory methods that produce JPA [Specification] predicates for
/// dynamic employee filtering.
///
/// **Java 22 feature used:** Unnamed Variables `_` (JEP 456)
/// Each lambda receives three parameters `(root, query, criteriaBuilder)`.
/// The second parameter `query` is never used in any of these specifications.
/// Replacing it with `_` signals intent clearly and suppresses IDE warnings.
public class EmployeeSpecification {

    /// Specification for case-insensitive partial name matching.
    /// Returns `null` (no-op) when `name` is `null` or blank.
    ///
    /// @param name partial name filter; `null` or blank means no filter
    public static Specification<Employee> hasName(String name) {
        // Java 22: _ for the unused CriteriaQuery parameter (JEP 456)
        return (root, _, criteriaBuilder) -> {
            if (name == null || name.isBlank()) {
                return null;
            }
            String lowerName = name.trim().toLowerCase();
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("empName")),
                    "%" + lowerName + "%"
            );
        };
    }

    /// Specification for salary range filtering.
    /// Supports `minSalary` only, `maxSalary` only, or both (BETWEEN).
    ///
    /// @param minSalary inclusive lower bound; `null` means no lower bound
    /// @param maxSalary inclusive upper bound; `null` means no upper bound
    public static Specification<Employee> hasSalaryBetween(BigDecimal minSalary, BigDecimal maxSalary) {
        // Java 22: _ for the unused CriteriaQuery parameter (JEP 456)
        return (root, _, criteriaBuilder) -> {
            if (minSalary != null && maxSalary != null) {
                return criteriaBuilder.between(root.get("empSalary"), minSalary, maxSalary);
            } else if (minSalary != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("empSalary"), minSalary);
            } else if (maxSalary != null) {
                return criteriaBuilder.lessThanOrEqualTo(root.get("empSalary"), maxSalary);
            }
            return null;
        };
    }

    /// Specification for creation date range filtering.
    /// Supports `createdAfter` only, `createdBefore` only, or both (BETWEEN).
    ///
    /// @param createdAfter  inclusive lower date bound; `null` means no lower bound
    /// @param createdBefore inclusive upper date bound; `null` means no upper bound
    public static Specification<Employee> hasCreatedDateBetween(LocalDate createdAfter, LocalDate createdBefore) {
        // Java 22: _ for the unused CriteriaQuery parameter (JEP 456)
        return (root, _, criteriaBuilder) -> {
            if (createdAfter != null && createdBefore != null) {
                return criteriaBuilder.between(root.get("empCreatedDate"), createdAfter, createdBefore);
            } else if (createdAfter != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("empCreatedDate"), createdAfter);
            } else if (createdBefore != null) {
                return criteriaBuilder.lessThanOrEqualTo(root.get("empCreatedDate"), createdBefore);
            }
            return null;
        };
    }
}
