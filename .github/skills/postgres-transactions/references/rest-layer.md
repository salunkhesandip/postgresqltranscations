# REF: rest-layer

Packages:
- Controller — `com.cleancoders.postgresqltranscations.controller`
- DTO — `com.cleancoders.postgresqltranscations.dto`
- Mapper — `com.cleancoders.postgresqltranscations.mapper`
- Exception — `com.cleancoders.postgresqltranscations.exception`

## CONTROLLER TEMPLATE

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
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/employees")
@Tag(name = "Employee API", description = "CRUD + JSON Patch operations on employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @Operation(summary = "Create a new employee")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Employee created"),
        @ApiResponse(responseCode = "409", description = "Employee already exists")
    })
    @PostMapping
    public ResponseEntity<EmployeeDTO> create(@Valid @RequestBody EmployeeDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.saveEmployee(dto));
    }

    @Operation(summary = "Get employee by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Employee found"),
        @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDTO> get(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.findEmployee(id));
    }

    @Operation(summary = "Full update of an employee")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Employee updated"),
        @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    @PutMapping
    public ResponseEntity<EmployeeDTO> update(@Valid @RequestBody EmployeeDTO dto) {
        return ResponseEntity.ok(employeeService.updateEmployee(dto));
    }

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

    @Operation(summary = "Find employees with salary greater than value (JPQL)")
    @ApiResponse(responseCode = "200", description = "List of matching employees")
    @GetMapping("/salary/{salary}")
    public ResponseEntity<List<EmployeeDTO>> bySalary(@PathVariable Long salary) {
        return ResponseEntity.ok(employeeService.findBySalaryGreaterThan(salary));
    }

    @Operation(summary = "Find employees with salary greater than value (Native SQL)")
    @ApiResponse(responseCode = "200", description = "List of matching employees")
    @GetMapping("/salary/native/{salary}")
    public ResponseEntity<List<EmployeeDTO>> bySalaryNative(@PathVariable Long salary) {
        return ResponseEntity.ok(employeeService.findBySalaryGreaterThanNative(salary));
    }
}
```

## DTO TEMPLATE

```java
package com.cleancoders.postgresqltranscations.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

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

    public EmployeeDTO() {}

    public EmployeeDTO(Long empId, String empName, BigDecimal empSalary) {
        this.empId = empId;
        this.empName = empName;
        this.empSalary = empSalary;
    }

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

## MAPPER TEMPLATE

```java
package com.cleancoders.postgresqltranscations.mapper;

import com.cleancoders.postgresqltranscations.dto.EmployeeDTO;
import com.cleancoders.postgresqltranscations.entity.Employee;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    private final ModelMapper modelMapper;

    public EmployeeMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public EmployeeDTO toDTO(Employee entity) {
        return modelMapper.map(entity, EmployeeDTO.class);
    }

    public Employee toEntity(EmployeeDTO dto) {
        return modelMapper.map(dto, Employee.class);
    }
}
```

## EXCEPTION HANDLER TEMPLATE

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

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<String> handleNotFound(EmployeeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(EmployeeConflictException.class)
    public ResponseEntity<String> handleConflict(EmployeeConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(errors);
    }
}
```

```java
// EmployeeNotFoundException — 404
public class EmployeeNotFoundException extends RuntimeException {
    public EmployeeNotFoundException(String message) { super(message); }
}

// EmployeeConflictException — 409
public class EmployeeConflictException extends RuntimeException {
    public EmployeeConflictException(String message) { super(message); }
}
```

## RESPONSE SHAPE

| Scenario              | Status | Body                     |
|-----------------------|--------|--------------------------|
| Create                | 201    | `EmployeeDTO` JSON       |
| Read / update / patch | 200    | `EmployeeDTO` JSON       |
| Delete                | 204    | Empty                    |
| Not found             | 404    | Plain string             |
| Conflict              | 409    | Plain string             |
| Validation failure    | 400    | `{ "field": "message" }` |
