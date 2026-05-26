# REF: transactions

Packages:
- Service — `com.cleancoders.postgresqltranscations.service`
- Repository — `com.cleancoders.postgresqltranscations.repository`

## TRANSACTION ANNOTATION CHEATSHEET

| Operation                        | Annotation                                 |
|----------------------------------|--------------------------------------------|
| Create / update / delete / patch | `@Transactional`                           |
| Any read                         | `@Transactional(readOnly = true)`          |
| `@Modifying` JPQL/native query   | `@Transactional` on calling service method |

## SERVICE TEMPLATE

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

    @Transactional
    public EmployeeDTO saveEmployee(EmployeeDTO dto) {
        employeeRepository.findById(dto.getEmpId()).ifPresent(e -> {
            throw new EmployeeConflictException("Employee already exists: " + dto.getEmpId());
        });
        return mapper.toDTO(employeeRepository.save(mapper.toEntity(dto)));
    }

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
                .stream().map(mapper::toDTO).toList();
    }

    @Transactional(readOnly = true)
    @CircuitBreaker(name = "databaseCalls")
    public List<EmployeeDTO> findBySalaryGreaterThanNative(Long salary) {
        return employeeRepository.findBySalaryGreaterThanNative(salary)
                .stream().map(mapper::toDTO).toList();
    }

    @Transactional
    public EmployeeDTO updateEmployee(EmployeeDTO dto) {
        employeeRepository.findById(dto.getEmpId())
                .orElseThrow(() -> new EmployeeNotFoundException("Not found: " + dto.getEmpId()));
        return mapper.toDTO(employeeRepository.save(mapper.toEntity(dto)));
    }

    @Transactional
    public EmployeeDTO patchEmployee(Long empId, JsonPatch patch) {
        Employee entity = employeeRepository.findById(empId)
                .orElseThrow(() -> new EmployeeNotFoundException("Not found: " + empId));
        try {
            JsonNode patched = patch.apply(objectMapper.convertValue(entity, JsonNode.class));
            Employee patchedEntity = objectMapper.treeToValue(patched, Employee.class);
            return mapper.toDTO(employeeRepository.save(patchedEntity));
        } catch (Exception e) {
            throw new RuntimeException("JSON Patch failed for employee " + empId, e);
        }
    }

    @Transactional
    public void deleteEmployee(Long empId) {
        employeeRepository.findById(empId)
                .orElseThrow(() -> new EmployeeNotFoundException("Not found: " + empId));
        employeeRepository.deleteById(empId);
    }
}
```

## REPOSITORY TEMPLATE

```java
package com.cleancoders.postgresqltranscations.repository;

import com.cleancoders.postgresqltranscations.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // JPQL — uses entity field names; portable across JPA providers
    @Query("SELECT e FROM Employee e WHERE e.empSalary > :salary")
    List<Employee> findBySalaryGreaterThan(@Param("salary") Long salary);

    // Native SQL — uses DB column names; use for complex SQL / PostgreSQL-specific features
    @Query(value = "SELECT * FROM company.employee WHERE emp_salary > :salary", nativeQuery = true)
    List<Employee> findBySalaryGreaterThanNative(@Param("salary") Long salary);

    // Bulk mutating query — @Modifying mandatory; calling service method must be @Transactional
    @Modifying
    @Query("DELETE FROM Employee e WHERE e.empSalary < :threshold")
    int deleteLowSalaryEmployees(@Param("threshold") Long threshold);
}
```

## JPQL vs NATIVE

| Scenario                                             | Choose                                       |
|------------------------------------------------------|----------------------------------------------|
| Filter / join on entity fields                       | JPQL — type-safe, uses Java field names      |
| Window functions, full-text, PostgreSQL-specific SQL | Native — uses DB column names                |
| Bulk INSERT / UPDATE / DELETE                        | Either — add `@Modifying` + `@Transactional` |

## CIRCUIT BREAKER

- Named instances: `backendA` (COUNT/50, 40%, 15 s wait), `databaseCalls` (TIME/60 s, 60%, 30 s wait)
- Place `@CircuitBreaker` on service method — never on controller
- Stack above `@Transactional`: `@CircuitBreaker` first, then `@Transactional(readOnly = true)`

## PROPAGATION QUICK REFERENCE

| Propagation          | Behaviour                          | When to use                         |
|----------------------|------------------------------------|-------------------------------------|
| `REQUIRED` (default) | Join existing tx; create if none   | All standard mutating methods       |
| `REQUIRES_NEW`       | Suspend outer; start fresh tx      | Audit logging, independent sub-ops  |
| `SUPPORTS`           | Join if present; run non-tx if not | Rarely — prefer explicit `readOnly` |
| `NOT_SUPPORTED`      | Always non-transactional           | Batch jobs managing their own tx    |
