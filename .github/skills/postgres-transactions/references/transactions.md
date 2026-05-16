# @Transactional Service Patterns

## Table of Contents

1. [Package](#package)
2. [When to Use `@Transactional`](#when-to-use-transactional)
3. [Full Service Template](#full-service-template)
4. [Repository Query Patterns](#repository-query-patterns)
5. [Bulk Operation Pattern](#bulk-operation-pattern)
6. [Circuit Breaker Integration](#circuit-breaker-integration)
7. [Transaction Propagation Quick Reference](#transaction-propagation-quick-reference)
8. [Common Mistakes](#common-mistakes)
9. [Rules](#rules)

---

## Package

```
com.cleancoders.postgresqltranscations.service
```

---

## When to Use `@Transactional`

| Operation type                      | Annotation                           |
|-------------------------------------|--------------------------------------|
| Create / update / delete            | `@Transactional`                     |
| Read (single or list)               | `@Transactional(readOnly = true)`    |
| Bulk `@Modifying` JPQL/native query | `@Transactional` (mandatory)         |
| JSON Patch partial update           | `@Transactional`                     |

> `readOnly = true` lets Hibernate skip dirty-checking and signals the DB driver to
> optimise the connection (e.g. use a read replica if configured).

---

## Full Service Template

```java
package com.cleancoders.postgresqltranscations.service;

import com.cleancoders.postgresqltranscations.dto.EmployeeDTO;
import com.cleancoders.postgresqltranscations.entity.Employee;
import com.cleancoders.postgresqltranscations.exception.EmployeeConflictException;
import com.cleancoders.postgresqltranscations.exception.EmployeeNotFoundException;
import com.cleancoders.postgresqltranscations.mapper.EmployeeMapper;
import com.cleancoders.postgresqltranscations.repository.EmployeeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper mapper;
    private final ObjectMapper objectMapper;

    public EmployeeService(EmployeeRepository employeeRepository,
                           EmployeeMapper mapper,
                           ObjectMapper objectMapper) {
        this.employeeRepository = employeeRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Transactional
    public EmployeeDTO saveEmployee(EmployeeDTO dto) {
        employeeRepository.findById(dto.getEmpId()).ifPresent(existing -> {
            throw new EmployeeConflictException("Employee already exists: " + dto.getEmpId());
        });
        Employee saved = employeeRepository.save(mapper.toEntity(dto));
        return mapper.toDTO(saved);
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public EmployeeDTO findEmployee(Long empId) {
        return employeeRepository.findById(empId)
                .map(mapper::toDTO)
                .orElseThrow(() -> new EmployeeNotFoundException("Not found: " + empId));
    }

    @Transactional(readOnly = true)
    @CircuitBreaker(name = "databaseCalls")
    public List<EmployeeDTO> findBySalaryGreaterThan(Long salary) {
        return employeeRepository.findBySalaryGreaterThan(salary)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    @CircuitBreaker(name = "databaseCalls")
    public List<EmployeeDTO> findBySalaryGreaterThanNative(Long salary) {
        return employeeRepository.findBySalaryGreaterThanNative(salary)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    // ── FULL UPDATE ───────────────────────────────────────────────────────────

    @Transactional
    public EmployeeDTO updateEmployee(EmployeeDTO dto) {
        employeeRepository.findById(dto.getEmpId())
                .orElseThrow(() -> new EmployeeNotFoundException("Not found: " + dto.getEmpId()));
        Employee updated = employeeRepository.save(mapper.toEntity(dto));
        return mapper.toDTO(updated);
    }

    // ── PARTIAL UPDATE (JSON Patch — RFC 6902) ────────────────────────────────

    @Transactional
    public EmployeeDTO patchEmployee(Long empId, JsonPatch patch) {
        Employee entity = employeeRepository.findById(empId)
                .orElseThrow(() -> new EmployeeNotFoundException("Not found: " + empId));
        try {
            JsonNode patched = patch.apply(objectMapper.convertValue(entity, JsonNode.class));
            Employee patchedEntity = objectMapper.treeToValue(patched, Employee.class);
            return mapper.toDTO(employeeRepository.save(patchedEntity));
        } catch (Exception e) {
            throw new RuntimeException("JSON Patch application failed for employee " + empId, e);
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Transactional
    public void deleteEmployee(Long empId) {
        employeeRepository.findById(empId)
                .orElseThrow(() -> new EmployeeNotFoundException("Not found: " + empId));
        employeeRepository.deleteById(empId);
    }
}
```

---

## Repository Query Patterns

```java
package com.cleancoders.postgresqltranscations.repository;

import com.cleancoders.postgresqltranscations.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // ── JPQL — entity field names, portable across JPA providers ──────────────

    @Query("SELECT e FROM Employee e WHERE e.empSalary > :salary")
    List<Employee> findBySalaryGreaterThan(@Param("salary") Long salary);

    // ── Native SQL — PostgreSQL column names; best for complex SQL/aggregates ──

    @Query(value = "SELECT * FROM company.employee WHERE emp_salary > :salary",
           nativeQuery = true)
    List<Employee> findBySalaryGreaterThanNative(@Param("salary") Long salary);

    // ── Bulk DELETE — @Modifying required; must run inside @Transactional ──────

    @Modifying
    @Query("DELETE FROM Employee e WHERE e.empSalary > :salary")
    void deleteEmployeesBySalaryGreaterThan(@Param("salary") Long salary);
}
```

### JPQL vs Native — When to Use Which

| Scenario                                 | Prefer     | Reason                                        |
|------------------------------------------|------------|-----------------------------------------------|
| Simple filter / join on entity fields    | JPQL       | Portable, type-safe, uses entity field names  |
| Complex SQL, window functions, full-text | Native SQL | Full PostgreSQL feature set available         |
| Bulk INSERT / UPDATE / DELETE            | Either     | Add `@Modifying` and wrap in `@Transactional` |

---

## Bulk Operation Pattern

```java
// In EmployeeRepository:
@Modifying
@Query("DELETE FROM Employee e WHERE e.empSalary < :threshold")
int deleteLowSalaryEmployees(@Param("threshold") Long threshold);

// In EmployeeService (must be @Transactional):
@Transactional
public int purgeEmployeesBelowSalary(Long threshold) {
    return employeeRepository.deleteLowSalaryEmployees(threshold);
}
```

> `@Modifying` without `@Transactional` on the calling service method will throw
> `javax.persistence.TransactionRequiredException` at runtime.

---

## Circuit Breaker Integration

Two named instances are declared in `application.yml`:

| Instance name   | Sliding window   | Failure threshold | Wait before retry |
|-----------------|------------------|-------------------|-------------------|
| `backendA`      | COUNT (50 calls) | 40 %              | 15 s              |
| `databaseCalls` | TIME (60 s)      | 60 %              | 30 s              |

Apply `@CircuitBreaker` on the **service** method, never on the controller:

```java
@Transactional(readOnly = true)
@CircuitBreaker(name = "databaseCalls")
public List<EmployeeDTO> findBySalaryGreaterThan(Long salary) {
    return employeeRepository.findBySalaryGreaterThan(salary)
            .stream()
            .map(mapper::toDTO)
            .toList();
}
```

Circuit breaker states: `CLOSED` → `OPEN` (threshold breached) → `HALF_OPEN` (after wait) → `CLOSED` (calls recover).

---

## Transaction Propagation Quick Reference

| Propagation          | Behaviour                                                    | When to use                                 |
|----------------------|--------------------------------------------------------------|---------------------------------------------|
| `REQUIRED` (default) | Join existing tx; create one if none exists                  | All standard mutating methods               |
| `REQUIRES_NEW`       | Always suspend outer tx and start a fresh one                | Audit logging, independent sub-operations   |
| `SUPPORTS`           | Join existing tx if present; run non-transactionally if not  | Rarely — prefer explicit `readOnly = true`  |
| `NOT_SUPPORTED`      | Always run without a transaction                             | Batch jobs that manage their own tx         |

---

## Common Mistakes

| Mistake                                               | Correct Approach                                                  |
|-------------------------------------------------------|-------------------------------------------------------------------|
| `@Transactional` on a controller method               | `@Transactional` belongs on **service** methods only              |
| Read method without `readOnly = true`                 | Always use `@Transactional(readOnly = true)` for reads            |
| Using `@Modifying` without enclosing `@Transactional` | The calling service method must be `@Transactional`               |
| Returning `null` from a service method                | Throw `EmployeeNotFoundException` or `EmployeeConflictException`  |
| Writing raw JDBC / SQL strings inside service classes | All persistence goes through `EmployeeRepository`                 |
| Catching and swallowing exceptions in service methods | Re-throw as a domain exception or let the tx roll back            |
| `@CircuitBreaker` placed on the controller            | Always place on the service method                                |
| JPQL using column names instead of field names        | JPQL uses Java entity field names (`empSalary`, not `emp_salary`) |

---

## Rules

- All mutating methods (`save`, `update`, `patch`, `delete`, bulk ops) must be annotated with `@Transactional`.
- All read-only queries must use `@Transactional(readOnly = true)` — Hibernate skips dirty-checking and can optimise the TX.
- Never return `null` — throw `EmployeeNotFoundException` (404) or `EmployeeConflictException` (409).
- No raw SQL or JDBC inside service classes; all persistence goes through `EmployeeRepository`.
- `@Modifying` is **mandatory** for `INSERT`, `UPDATE`, or `DELETE` JPQL/native queries; the calling service method must also be `@Transactional`.
- Place `@CircuitBreaker(name = "databaseCalls")` on the service method, **never** on the controller.
- JPQL queries reference entity/field names; native queries reference DB table/column names.
