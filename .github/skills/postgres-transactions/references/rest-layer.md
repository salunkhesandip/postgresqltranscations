# Controller, DTO & Mapper Patterns

## Table of Contents

1. [Packages](#packages)
2. [REST Endpoints at a Glance](#rest-endpoints-at-a-glance)
3. [Controller Template](#controller-template)
4. [DTO Template](#dto-template)
5. [Mapper Template](#mapper-template)
6. [Global Exception Handler Template](#global-exception-handler-template)
7. [JSON Patch Request Format](#json-patch-request-format)
8. [OpenAPI Annotation Guide](#openapi-annotation-guide)
9. [HTTP Response Shape](#http-response-shape)
10. [Common Mistakes](#common-mistakes)
11. [Rules](#rules)

---

## Packages

| Layer             | Package                                                    |
|-------------------|------------------------------------------------------------|
| Controller        | `com.cleancoders.postgresqltranscations.controller`        |
| DTO               | `com.cleancoders.postgresqltranscations.dto`               |
| Mapper            | `com.cleancoders.postgresqltranscations.mapper`            |
| Exception handler | `com.cleancoders.postgresqltranscations.exception`         |

---

## REST Endpoints at a Glance

| Method   | Path                                | Status         | Description                          |
|----------|-------------------------------------|----------------|--------------------------------------|
| `POST`   | `/employees`                        | 201 Created    | Create employee                      |
| `GET`    | `/employees/{id}`                   | 200 OK         | Get by ID                            |
| `PUT`    | `/employees`                        | 200 OK         | Full update                          |
| `PATCH`  | `/employees/{id}`                   | 200 OK         | Partial update — RFC 6902 JSON Patch |
| `DELETE` | `/employees/{id}`                   | 204 No Content | Delete by ID                         |
| `GET`    | `/employees/salary/{salary}`        | 200 OK         | JPQL salary filter                   |
| `GET`    | `/employees/salary/native/{salary}` | 200 OK         | Native SQL salary filter             |

Error responses: `404 Not Found`, `409 Conflict`, `400 Bad Request` (validation failure).

---

## Controller Template

```java
package com.cleancoders.postgresqltranscations.controller;

import com.cleancoders.postgresqltranscations.dto.EmployeeDTO;
import com.cleancoders.postgresqltranscations.service.EmployeeService;
import com.github.fge.jsonpatch.JsonPatch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/employees")
@Tag(name = "Employee API", description = "CRUD + JSON Patch operations on employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Operation(summary = "Create a new employee")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Employee created"),
        @ApiResponse(responseCode = "409", description = "Employee already exists")
    })
    @PostMapping
    public ResponseEntity<EmployeeDTO> create(@Valid @RequestBody EmployeeDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employeeService.saveEmployee(dto));
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Operation(summary = "Get employee by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Employee found"),
        @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDTO> get(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.findEmployee(id));
    }

    // ── FULL UPDATE ───────────────────────────────────────────────────────────

    @Operation(summary = "Full update of an employee")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Employee updated"),
        @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    @PutMapping
    public ResponseEntity<EmployeeDTO> update(@Valid @RequestBody EmployeeDTO dto) {
        return ResponseEntity.ok(employeeService.updateEmployee(dto));
    }

    // ── PARTIAL UPDATE (JSON Patch — RFC 6902) ────────────────────────────────

    @Operation(summary = "Partial update via JSON Patch (RFC 6902)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Employee patched"),
        @ApiResponse(responseCode = "404", description = "Employee not found"),
        @ApiResponse(responseCode = "400", description = "Invalid patch document")
    })
    @PatchMapping(value = "/{id}", consumes = "application/json-patch+json")
    public ResponseEntity<EmployeeDTO> patch(@PathVariable Long id,
                                             @RequestBody JsonPatch patch) {
        return ResponseEntity.ok(employeeService.patchEmployee(id, patch));
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Operation(summary = "Delete employee by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Employee deleted"),
        @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    // ── SALARY FILTER (JPQL) ──────────────────────────────────────────────────

    @Operation(summary = "Find employees with salary greater than given value (JPQL)")
    @ApiResponse(responseCode = "200", description = "List of matching employees")
    @GetMapping("/salary/{salary}")
    public ResponseEntity<List<EmployeeDTO>> bySalary(@PathVariable Long salary) {
        return ResponseEntity.ok(employeeService.findBySalaryGreaterThan(salary));
    }

    // ── SALARY FILTER (Native SQL) ────────────────────────────────────────────

    @Operation(summary = "Find employees with salary greater than given value (Native SQL)")
    @ApiResponse(responseCode = "200", description = "List of matching employees")
    @GetMapping("/salary/native/{salary}")
    public ResponseEntity<List<EmployeeDTO>> bySalaryNative(@PathVariable Long salary) {
        return ResponseEntity.ok(employeeService.findBySalaryGreaterThanNative(salary));
    }
}
```

---

## DTO Template

```java
package com.cleancoders.postgresqltranscations.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data-transfer object for Employee.
 * No JPA annotations — Bean Validation only.
 */
public class EmployeeDTO {

    @NotNull(message = "Employee ID must not be null")
    private Long empId;

    @NotBlank(message = "Employee name must not be blank")
    private String empName;

    @DecimalMin(value = "0.0", inclusive = false, message = "Salary must be positive")
    private BigDecimal empSalary;

    private String empAddress;
    private LocalDate empCreatedDate;
    private LocalDate empUpdatedDate;
    private String empCreatedBy;
    private String empUpdateBy;

    // ── Constructors ─────────────────────────────────────────────────────────

    public EmployeeDTO() {}

    public EmployeeDTO(Long empId, String empName, BigDecimal empSalary) {
        this.empId = empId;
        this.empName = empName;
        this.empSalary = empSalary;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getEmpId() { return empId; }
    public void setEmpId(Long empId) { this.empId = empId; }

    public String getEmpName() { return empName; }
    public void setEmpName(String empName) { this.empName = empName; }

    public BigDecimal getEmpSalary() { return empSalary; }
    public void setEmpSalary(BigDecimal empSalary) { this.empSalary = empSalary; }

    public String getEmpAddress() { return empAddress; }
    public void setEmpAddress(String empAddress) { this.empAddress = empAddress; }

    public LocalDate getEmpCreatedDate() { return empCreatedDate; }
    public void setEmpCreatedDate(LocalDate empCreatedDate) { this.empCreatedDate = empCreatedDate; }

    public LocalDate getEmpUpdatedDate() { return empUpdatedDate; }
    public void setEmpUpdatedDate(LocalDate empUpdatedDate) { this.empUpdatedDate = empUpdatedDate; }

    public String getEmpCreatedBy() { return empCreatedBy; }
    public void setEmpCreatedBy(String empCreatedBy) { this.empCreatedBy = empCreatedBy; }

    public String getEmpUpdateBy() { return empUpdateBy; }
    public void setEmpUpdateBy(String empUpdateBy) { this.empUpdateBy = empUpdateBy; }
}
```

---

## Mapper Template

```java
package com.cleancoders.postgresqltranscations.mapper;

import com.cleancoders.postgresqltranscations.dto.EmployeeDTO;
import com.cleancoders.postgresqltranscations.entity.Employee;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around ModelMapper.
 * The ModelMapper bean is declared in a @Configuration class (e.g. JpaConfig).
 */
@Component
public class EmployeeMapper {

    private final ModelMapper modelMapper;

    public EmployeeMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    /** Converts a JPA entity to a DTO for transport across the API boundary. */
    public EmployeeDTO toDTO(Employee entity) {
        return modelMapper.map(entity, EmployeeDTO.class);
    }

    /** Converts an inbound DTO to a JPA entity for persistence. */
    public Employee toEntity(EmployeeDTO dto) {
        return modelMapper.map(dto, Employee.class);
    }
}
```

---

## Global Exception Handler Template

```java
package com.cleancoders.postgresqltranscations.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 404 — employee not found */
    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<String> handleNotFound(EmployeeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    /** 409 — employee already exists */
    @ExceptionHandler(EmployeeConflictException.class)
    public ResponseEntity<String> handleConflict(EmployeeConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    /** 400 — Bean Validation (@Valid) failure; returns field → message map */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(errors);
    }
}
```

### Exception Classes

```java
// EmployeeNotFoundException — maps to 404
public class EmployeeNotFoundException extends RuntimeException {
    public EmployeeNotFoundException(String message) { super(message); }
}

// EmployeeConflictException — maps to 409
public class EmployeeConflictException extends RuntimeException {
    public EmployeeConflictException(String message) { super(message); }
}
```

---

## JSON Patch Request Format

The `PATCH /employees/{id}` endpoint consumes `application/json-patch+json` (RFC 6902).

The body must be a **JSON array** of operation objects:

```json
[
  { "op": "replace", "path": "/empName",    "value": "Alice Smith" },
  { "op": "replace", "path": "/empSalary",  "value": 85000 },
  { "op": "replace", "path": "/empAddress", "value": "42 Main St" }
]
```

Valid `op` values: `add`, `remove`, `replace`, `move`, `copy`, `test`.

### Common causes of `400 Bad Request`

| Mistake                                               | Fix                                               |
|-------------------------------------------------------|---------------------------------------------------|
| Sending `{}` (an object) instead of `[{}]` (an array) | Always use a JSON array as the root element       |
| Single quotes instead of double quotes                | JSON requires double quotes                       |
| Trailing comma after the last element                 | Remove trailing commas                            |
| Missing `Content-Type: application/json-patch+json`   | Always set this header for PATCH requests         |
| Path referencing a non-existent DTO field             | Check field names against `EmployeeDTO` carefully |

---

## OpenAPI Annotation Guide

| Annotation                                | Where                         | Required    |
|-------------------------------------------|-------------------------------|-------------|
| `@Tag(name = "...", description = "...")` | Controller class              | Yes         |
| `@Operation(summary = "...")`             | Every endpoint method         | Yes         |
| `@ApiResponse(responseCode = "...")`      | Each method (success + error) | Recommended |

Swagger UI is available at `http://localhost:8080/swagger-ui.html` when the app is running.

---

## HTTP Response Shape

| Scenario                         | Status | Body                                      |
|----------------------------------|--------|-------------------------------------------|
| Successful create                | 201    | `EmployeeDTO` JSON object                 |
| Successful read / update / patch | 200    | `EmployeeDTO` JSON object                 |
| Successful delete                | 204    | Empty                                     |
| Not found                        | 404    | Plain string message                      |
| Conflict                         | 409    | Plain string message                      |
| Validation failure               | 400    | `{ "field": "message", ... }` JSON object |

---

## Common Mistakes

| Mistake                                            | Correct Approach                                                     |
|----------------------------------------------------|----------------------------------------------------------------------|
| Adding business logic inside a controller method   | Controllers are thin — delegate everything to the service            |
| Missing `@Valid` on `@RequestBody` parameter       | Every `@RequestBody` in the controller must have `@Valid`            |
| PATCH endpoint missing `consumes` attribute        | Use `consumes = "application/json-patch+json"`                       |
| Missing `@Operation(summary = "...")` on endpoint  | Every endpoint needs this OpenAPI annotation                         |
| Missing `@Tag` on the controller class             | Required once per controller                                         |
| Handling exceptions inside the controller          | All exception mapping belongs in `GlobalExceptionHandler`            |
| DTO carrying JPA annotations                       | DTOs use Bean Validation (`@NotNull` etc.) — zero JPA                |
| `ModelMapper` constructed with `new` in the mapper | Inject the `ModelMapper` Spring bean; declare it in `@Configuration` |

---

## Rules

- Controllers are **thin**: HTTP method mapping, `@Valid`, and service delegation only. No business logic.
- DTOs have **no** JPA annotations; use Bean Validation (`@NotNull`, `@NotBlank`, `@DecimalMin`, etc.).
- Use `@Valid` on **every** `@RequestBody` parameter.
- JSON Patch endpoints must declare `consumes = "application/json-patch+json"`.
- Every endpoint needs `@Operation(summary = "...")` for Swagger / OpenAPI docs.
- Every controller class needs `@Tag(name = "...", description = "...")`.
- All exception-to-HTTP-status mapping lives exclusively in `GlobalExceptionHandler` (`@RestControllerAdvice`).
- The `ModelMapper` bean is declared in a `@Configuration` class; inject it into `EmployeeMapper` — never use `new ModelMapper()` inside a component.
