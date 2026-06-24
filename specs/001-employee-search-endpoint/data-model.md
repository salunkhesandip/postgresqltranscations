# Data Model: Employee Search Endpoint

**Feature**: Employee Search with Pagination and Filtering  
**Date**: 2025-01-26  
**Status**: Design Complete

## Overview

This document defines the data structures for the employee search endpoint, including search criteria DTOs, pagination response wrappers, and entity-to-DTO mappings.

---

## DTOs (Data Transfer Objects)

### EmployeeSearchCriteria

**Purpose**: Encapsulates all search filter parameters and pagination controls from the client request.

**Location**: `src/main/java/com/cleancoders/postgresqltranscations/dto/EmployeeSearchCriteria.java`

```java
package com.cleancoders.postgresqltranscations.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@ValidSalaryRange
@ValidDateRange
public class EmployeeSearchCriteria {

    // Pagination parameters
    @Min(value = 0, message = "Page number must be 0 or greater")
    private Integer page = 0;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    private Integer size = 20;

    // Filter parameters (all optional)
    private String name; // Partial match, case-insensitive

    @PositiveOrZero(message = "Minimum salary must be zero or positive")
    private BigDecimal minSalary;

    @PositiveOrZero(message = "Maximum salary must be zero or positive")
    private BigDecimal maxSalary;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdAfter;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdBefore;

    // Getters and setters for all fields
}
```

**Field Descriptions**:

| Field | Type | Required | Default | Constraints | Description |
|-------|------|----------|---------|-------------|-------------|
| `page` | Integer | No | 0 | >= 0 | Zero-based page number |
| `size` | Integer | No | 20 | 1-100 | Number of employees per page |
| `name` | String | No | null | None | Employee name partial match (case-insensitive) |
| `minSalary` | BigDecimal | No | null | >= 0 | Minimum salary (inclusive) |
| `maxSalary` | BigDecimal | No | null | >= 0 | Maximum salary (inclusive) |
| `createdAfter` | LocalDate | No | null | ISO 8601 format | Created on or after this date |
| `createdBefore` | LocalDate | No | null | ISO 8601 format | Created on or before this date |

**Validation Rules**:

1. **Individual Field Validation** (Bean Validation annotations):
   - `page`: Must be >= 0
   - `size`: Must be between 1 and 100 (inclusive)
   - `minSalary`, `maxSalary`: If provided, must be >= 0

2. **Cross-Field Validation** (Custom validators):
   - `@ValidSalaryRange`: If both minSalary and maxSalary are provided, minSalary must be <= maxSalary
   - `@ValidDateRange`: If both createdAfter and createdBefore are provided, createdAfter must be <= createdBefore

**Example Valid Request**:
```json
{
  "page": 0,
  "size": 10,
  "name": "John",
  "minSalary": 40000,
  "maxSalary": 80000,
  "createdAfter": "2024-01-01",
  "createdBefore": "2024-12-31"
}
```

---

### PagedEmployeeResponse

**Purpose**: Wraps the list of employee DTOs with pagination metadata for API responses.

**Location**: `src/main/java/com/cleancoders/postgresqltranscations/dto/PagedEmployeeResponse.java`

```java
package com.cleancoders.postgresqltranscations.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public class PagedEmployeeResponse {

    private List<EmployeeDTO> content;
    private PaginationMetadata pagination;

    // Constructor, getters, setters

    /**
     * Factory method to convert Spring Data Page<EmployeeDTO> to PagedEmployeeResponse.
     */
    public static PagedEmployeeResponse fromPage(Page<EmployeeDTO> page) {
        PagedEmployeeResponse response = new PagedEmployeeResponse();
        response.setContent(page.getContent());
        response.setPagination(PaginationMetadata.fromPage(page));
        return response;
    }
}
```

**Field Descriptions**:

| Field | Type | Description |
|-------|------|-------------|
| `content` | List<EmployeeDTO> | List of employee DTOs for the current page |
| `pagination` | PaginationMetadata | Metadata about the pagination state |

**JSON Response Example**:
```json
{
  "content": [
    {
      "empId": 1,
      "empName": "John Smith",
      "empSalary": 50000.00,
      "empAddress": "123 Main St",
      "empCreatedDate": "2024-06-15",
      "empUpdatedDate": "2024-06-15"
    },
    {
      "empId": 2,
      "empName": "John Doe",
      "empSalary": 60000.00,
      "empAddress": "456 Oak Ave",
      "empCreatedDate": "2024-03-20",
      "empUpdatedDate": "2024-03-20"
    }
  ],
  "pagination": {
    "totalElements": 25,
    "totalPages": 3,
    "currentPage": 0,
    "pageSize": 10,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

---

### PaginationMetadata

**Purpose**: Provides metadata about pagination state to help clients navigate through paginated results.

**Location**: `src/main/java/com/cleancoders/postgresqltranscations/dto/PaginationMetadata.java`

```java
package com.cleancoders.postgresqltranscations.dto;

import org.springframework.data.domain.Page;

public class PaginationMetadata {

    private long totalElements;  // Total number of employees matching the search criteria
    private int totalPages;      // Total number of pages
    private int currentPage;     // Current page number (zero-based)
    private int pageSize;        // Number of items in current page
    private boolean hasNext;     // True if there is a next page
    private boolean hasPrevious; // True if there is a previous page

    // Constructor, getters, setters

    /**
     * Factory method to extract pagination metadata from Spring Data Page.
     */
    public static PaginationMetadata fromPage(Page<?> page) {
        PaginationMetadata metadata = new PaginationMetadata();
        metadata.setTotalElements(page.getTotalElements());
        metadata.setTotalPages(page.getTotalPages());
        metadata.setCurrentPage(page.getNumber());
        metadata.setPageSize(page.getSize());
        metadata.setHasNext(page.hasNext());
        metadata.setHasPrevious(page.hasPrevious());
        return metadata;
    }
}
```

**Field Descriptions**:

| Field | Type | Description | Example Value |
|-------|------|-------------|---------------|
| `totalElements` | long | Total count of employees matching the search criteria | 25 |
| `totalPages` | int | Total number of pages (calculated from totalElements and pageSize) | 3 |
| `currentPage` | int | Current page number (zero-based) | 0 |
| `pageSize` | int | Number of items requested per page | 10 |
| `hasNext` | boolean | True if there are more pages after this one | true |
| `hasPrevious` | boolean | True if there are pages before this one | false |

**Navigation Logic**:

- **First Page**: `currentPage = 0`, `hasPrevious = false`
- **Last Page**: `currentPage = totalPages - 1`, `hasNext = false`
- **Empty Result**: `totalElements = 0`, `totalPages = 0`, `content = []`
- **Single Page**: `totalPages = 1`, `hasNext = false`, `hasPrevious = false`

---

## Extended EmployeeDTO

The existing `EmployeeDTO` must be extended to include additional fields required by the search endpoint response.

**Current EmployeeDTO** (from existing codebase):
```java
public class EmployeeDTO implements Serializable {
    private Long empId;
    private String empName;
    private BigDecimal empSalary;
    // Getters/setters
}
```

**Extended EmployeeDTO** (required for search):
```java
package com.cleancoders.postgresqltranscations.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public class EmployeeDTO implements Serializable {
    @NotNull
    @Positive(message = "Employee ID must be a positive number")
    private Long empId;
    
    @NotEmpty
    private String empName;
    
    @NotNull
    private BigDecimal empSalary;
    
    private String empAddress;       // NEW - organizational unit or location
    private LocalDate empCreatedDate; // NEW - record creation timestamp
    private LocalDate empUpdatedDate; // NEW - last modification timestamp
    
    // Getters and setters for all fields
}
```

**Rationale for Extension**:
- Feature spec (FR-006) requires DTOs to include: id, name, email, salary, department, createdAt
- Current Employee entity already has: empAddress (can represent department/location), empCreatedDate, empUpdatedDate
- Email field not present in existing entity; may need to add if required by future features
- **Note**: The spec mentions "email" and "department", but the existing schema only has `empAddress`. This should be clarified with stakeholders. For now, we'll use `empAddress` as a proxy for department/location information.

---

## Entity (No Changes Required)

The existing `Employee` entity already has all fields needed for search functionality:

```java
@Entity
@Table(name = "employee", schema = "company")
public class Employee {
    @Id
    @Column(name = "emp_id")
    private Long empId;
    
    @Column(name = "emp_name")
    private String empName;
    
    @Column(name = "emp_salary")
    private BigDecimal empSalary;
    
    @Column(name = "emp_address")
    private String empAddress;
    
    @Column(name = "emp_created_date")
    private LocalDate empCreatedDate;
    
    @Column(name = "emp_updated_date")
    private LocalDate empUpdatedDate;
    
    @Column(name = "emp_created_by", length = 30)
    private String empCreatedBy;
    
    @Column(name = "emp_update_by", length = 30)
    private String empUpdateBy;
    
    // Getters, setters, constructors
}
```

**Mapping Strategy**: ModelMapper (existing bean) will handle Entity ↔ DTO conversion. Field names match exactly (empId, empName, empSalary, empAddress, empCreatedDate, empUpdatedDate), so STRICT matching strategy is safe.

---

## Custom Validation Annotations

### @ValidSalaryRange

**Purpose**: Validates that minSalary <= maxSalary when both are provided.

**Location**: `src/main/java/com/cleancoders/postgresqltranscations/validation/ValidSalaryRange.java`

```java
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
```

**Validator Implementation**:
```java
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
```

### @ValidDateRange

**Purpose**: Validates that createdAfter <= createdBefore when both are provided.

**Location**: `src/main/java/com/cleancoders/postgresqltranscations/validation/ValidDateRange.java`

```java
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
```

**Validator Implementation**:
```java
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
```

---

## Specification Predicates

**Purpose**: Build dynamic JPA Criteria predicates for optional search filters.

**Location**: `src/main/java/com/cleancoders/postgresqltranscations/specification/EmployeeSpecification.java`

```java
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
```

**Predicate Composition Example** (in EmployeeService):
```java
Specification<Employee> spec = Specification.where(null);

if (criteria.getName() != null && !criteria.getName().isBlank()) {
    spec = spec.and(EmployeeSpecification.hasName(criteria.getName()));
}

if (criteria.getMinSalary() != null || criteria.getMaxSalary() != null) {
    spec = spec.and(EmployeeSpecification.hasSalaryBetween(
        criteria.getMinSalary(), criteria.getMaxSalary()));
}

if (criteria.getCreatedAfter() != null || criteria.getCreatedBefore() != null) {
    spec = spec.and(EmployeeSpecification.hasCreatedDateBetween(
        criteria.getCreatedAfter(), criteria.getCreatedBefore()));
}

Page<Employee> page = employeeRepository.findAll(spec, pageable);
```

---

## Repository Extension

Extend `EmployeeRepository` to support Specifications:

```java
package com.cleancoders.postgresqltranscations.repository;

import com.cleancoders.postgresqltranscations.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long>, 
                                           JpaSpecificationExecutor<Employee> {

    // Existing custom query methods remain unchanged
    @Query("SELECT e FROM Employee e WHERE e.empSalary > :salary")
    List<Employee> findBySalaryGreaterThan(@Param("salary") BigDecimal salary);

    @Query(value = "SELECT * FROM company.employee WHERE emp_salary > :salary", nativeQuery = true)
    List<Employee> findBySalaryGreaterThanNative(@Param("salary") BigDecimal salary);

    @Modifying
    @Query("DELETE FROM Employee e WHERE e.empSalary > ?1")
    void deleteUsersBySalaryGreater(BigDecimal salary);
}
```

**Change**: Added `JpaSpecificationExecutor<Employee>` interface extension. This provides:
- `findAll(Specification<Employee> spec, Pageable pageable)` method
- No implementation needed; Spring Data JPA generates the implementation at runtime

---

## Data Flow Diagram

```
Client Request (GET /api/employees/search?name=John&minSalary=40000&page=0&size=10)
    ↓
EmployeeController
    ↓ (binds query params to EmployeeSearchCriteria DTO)
    ↓ (validates with @Valid → Bean Validation + Custom Validators)
    ↓
EmployeeService.searchEmployees(criteria)
    ↓ (builds Specification from criteria)
    ↓ (creates PageRequest from criteria.page and criteria.size)
    ↓
EmployeeRepository.findAll(spec, pageable)
    ↓ (executes JPA Criteria query with pagination)
    ↓ (returns Page<Employee>)
    ↓
EmployeeService (maps Page<Employee> to Page<EmployeeDTO> using ModelMapper)
    ↓ (converts to PagedEmployeeResponse.fromPage(dtoPage))
    ↓
EmployeeController
    ↓ (returns ResponseEntity<PagedEmployeeResponse>)
    ↓
Client receives JSON response with content and pagination metadata
```

---

## Database Schema (Reference)

**Table**: `company.employee`

| Column | Type | Constraints | Indexed | Used For |
|--------|------|-------------|---------|----------|
| emp_id | BIGINT | PRIMARY KEY | Yes (PK) | Unique identifier |
| emp_name | VARCHAR(255) | | **Should be** (LOWER(emp_name)) | Name filtering (case-insensitive) |
| emp_salary | NUMERIC(19,2) | | **Should be** | Salary range filtering |
| emp_address | VARCHAR(255) | | | Display only (department proxy) |
| emp_created_date | DATE | | **Should be** | Date range filtering |
| emp_updated_date | DATE | | | Display only |
| emp_created_by | VARCHAR(30) | | | Audit trail |
| emp_update_by | VARCHAR(30) | | | Audit trail |

**Recommended Indexes** (for optimal search performance):

```sql
-- Functional index for case-insensitive name search
CREATE INDEX idx_lower_emp_name ON company.employee (LOWER(emp_name));

-- Index for salary range queries
CREATE INDEX idx_emp_salary ON company.employee (emp_salary);

-- Index for date range queries
CREATE INDEX idx_emp_created_date ON company.employee (emp_created_date);
```

---

## Summary

**New Components**:
1. `EmployeeSearchCriteria` DTO - Search filter parameters with validation
2. `PagedEmployeeResponse` DTO - Pagination wrapper for results
3. `PaginationMetadata` DTO - Pagination state metadata
4. `@ValidSalaryRange` and `@ValidDateRange` - Custom validation annotations
5. `SalaryRangeValidator` and `DateRangeValidator` - Validation implementations
6. `EmployeeSpecification` - JPA Specification predicate builders
7. `EmployeeRepository` extension - Add `JpaSpecificationExecutor<Employee>`

**Extended Components**:
- `EmployeeDTO` - Add empAddress, empCreatedDate, empUpdatedDate fields

**No Changes Required**:
- `Employee` entity - Already has all necessary fields
- `EmployeeMapper` - ModelMapper handles new fields automatically (same names in entity and DTO)

**Next Steps**:
- Create OpenAPI contract (contracts/search-api.yml)
- Create quickstart validation guide (quickstart.md)
- Update agent context reference

