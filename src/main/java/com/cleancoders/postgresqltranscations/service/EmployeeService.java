package com.cleancoders.postgresqltranscations.service;

import com.cleancoders.postgresqltranscations.context.RequestContext;
import com.cleancoders.postgresqltranscations.dto.EmployeeDTO;
import com.cleancoders.postgresqltranscations.dto.EmployeeSearchCriteria;
import com.cleancoders.postgresqltranscations.dto.PagedEmployeeResponse;
import com.cleancoders.postgresqltranscations.entity.Employee;
import com.cleancoders.postgresqltranscations.exception.EmployeeConflictException;
import com.cleancoders.postgresqltranscations.exception.EmployeeNotFoundException;
import com.cleancoders.postgresqltranscations.exception.ServiceUnavailableException;
import com.cleancoders.postgresqltranscations.mapper.EmployeeMapper;
import com.cleancoders.postgresqltranscations.repository.EmployeeRepository;
import com.cleancoders.postgresqltranscations.specification.EmployeeSpecification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Java 25: Module Import Declaration (JEP 511) — imports all public packages of java.base
// in one line, replacing individual imports of BigDecimal, LocalDate, List, Gatherers, etc.
import module java.base;

/// Employee business logic — the sole transactional boundary in the application.
///
/// **Java 21–25 features demonstrated in this class:**
/// - **`import module java.base`** (JEP 511, Java 25) — replaces explicit
///   `java.math`, `java.time`, `java.util`, `java.util.stream` imports.
/// - **Stream Gatherers** (JEP 485, Java 24) — `Gatherers.mapConcurrent` for
///   concurrent DTO mapping in salary-filter read operations.
/// - **Scoped Values** (JEP 506, Java 25) — fallback methods read the per-request
///   `CORRELATION_ID` without any parameter threading, enabling distributed tracing
///   even when the circuit breaker fires on a different thread.
/// - **Virtual Threads** (JEP 444, Java 21) — enabled globally via
///   `spring.threads.virtual.enabled=true`; all `@Transactional` DB calls are
///   non-pinning when running on virtual threads.
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

    // -------------------------------------------------------------------------
    // Write operations — guarded by backendA (COUNT_BASED, 40% threshold, 15 s)
    // -------------------------------------------------------------------------

    @CircuitBreaker(name = "backendA", fallbackMethod = "saveEmployeeFallback")
    @Transactional
    public EmployeeDTO saveEmployee(EmployeeDTO employeeDTO) {
        if (employeeRepository.existsById(employeeDTO.getEmpId())) {
            throw new EmployeeConflictException("Conflict: employee already exists with id=" + employeeDTO.getEmpId());
        }
        Employee employee = mapper.convertToEmployee(employeeDTO);
        employee.setEmpCreatedDate(LocalDate.now());
        employee.setEmpUpdatedDate(LocalDate.now());
        return mapper.convertToEmployeeDTO(employeeRepository.save(employee));
    }

    @CircuitBreaker(name = "backendA", fallbackMethod = "updateEmployeeFallback")
    @Transactional
    public EmployeeDTO updateEmployee(EmployeeDTO employeeDTO) {
        Employee existing = employeeRepository.findById(employeeDTO.getEmpId())
                .orElseThrow(() -> new EmployeeNotFoundException("Employee " + employeeDTO.getEmpId() + " not found"));
        existing.setEmpName(employeeDTO.getEmpName());
        existing.setEmpSalary(employeeDTO.getEmpSalary());
        existing.setEmpUpdatedDate(LocalDate.now());
        return mapper.convertToEmployeeDTO(employeeRepository.save(existing));
    }

    @CircuitBreaker(name = "backendA", fallbackMethod = "patchEmployeeFallback")
    @Transactional
    public EmployeeDTO patchEmployee(Long id, String jsonPatchRequest)
            throws JsonPatchException, JsonProcessingException {
        JsonPatch jsonPatch = objectMapper.readValue(jsonPatchRequest, JsonPatch.class);
        EmployeeDTO employeeDTO = findEmployee(id);
        return updateEmployee(applyPatchToEmployee(jsonPatch, employeeDTO));
    }

    @CircuitBreaker(name = "backendA", fallbackMethod = "deleteEmployeeFallback")
    @Transactional
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new EmployeeNotFoundException("Employee " + id + " not found");
        }
        employeeRepository.deleteById(id);
    }

    @CircuitBreaker(name = "backendA", fallbackMethod = "deleteEmployeeWithGreaterSalaryFallback")
    @Transactional
    public void deleteEmployeeWithGreaterSalary(BigDecimal salary) {
        employeeRepository.deleteUsersBySalaryGreater(salary);
    }

    // -------------------------------------------------------------------------
    // Read operations — guarded by databaseCalls (TIME_BASED, 60 s, 60%) + retry
    // -------------------------------------------------------------------------

    @Retry(name = "databaseCalls")
    @CircuitBreaker(name = "databaseCalls", fallbackMethod = "findEmployeeFallback")
    @Transactional(readOnly = true)
    public EmployeeDTO findEmployee(Long id) {
        return employeeRepository.findById(id)
                .map(mapper::convertToEmployeeDTO)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee " + id + " not found"));
    }

    @Retry(name = "databaseCalls")
    @CircuitBreaker(name = "databaseCalls", fallbackMethod = "findEmployeesBySalaryFallback")
    @Transactional(readOnly = true)
    public List<EmployeeDTO> findEmployeesBySalary(BigDecimal salary) {
        List<Employee> employees = employeeRepository.findBySalaryGreaterThan(salary);
        if (employees.isEmpty()) {
            throw new EmployeeNotFoundException("No employees found with salary above " + salary);
        }
        // Java 24: Stream Gatherers — mapConcurrent (JEP 485).
        // Applies the mapping function concurrently across up to 4 virtual threads.
        // Most valuable when the mapping function performs I/O (e.g. an external enrichment call).
        return employees.stream()
                .gather(Gatherers.mapConcurrent(4, mapper::convertToEmployeeDTO))
                .toList();
    }

    @Retry(name = "databaseCalls")
    @CircuitBreaker(name = "databaseCalls", fallbackMethod = "findEmployeesBySalaryNativeFallback")
    @Transactional(readOnly = true)
    public List<EmployeeDTO> findEmployeesBySalaryNative(BigDecimal salary) {
        List<Employee> employees = employeeRepository.findBySalaryGreaterThanNative(salary);
        if (employees.isEmpty()) {
            throw new EmployeeNotFoundException("No employees found with salary above " + salary);
        }
        // Java 24: Stream Gatherers — mapConcurrent (JEP 485)
        return employees.stream()
                .gather(Gatherers.mapConcurrent(4, mapper::convertToEmployeeDTO))
                .toList();
    }

    /// Search employees with dynamic filtering and pagination.
    /// Supports filtering by name (partial, case-insensitive), salary range,
    /// and creation date range. User Stories 1–5 implementation.
    @Retry(name = "databaseCalls")
    @CircuitBreaker(name = "databaseCalls", fallbackMethod = "searchEmployeesFallback")
    @Transactional(readOnly = true)
    public PagedEmployeeResponse searchEmployees(EmployeeSearchCriteria criteria) {
        Specification<Employee> spec = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

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

        Pageable pageable = PageRequest.of(criteria.getPage(), criteria.getSize());
        Page<Employee> page = employeeRepository.findAll(spec, pageable);
        Page<EmployeeDTO> dtoPage = page.map(mapper::convertToEmployeeDTO);
        return PagedEmployeeResponse.fromPage(dtoPage);
    }

    // -------------------------------------------------------------------------
    // Circuit-breaker fallback methods
    // Package-private so they are directly testable from the same package.
    // Each must match the guarded method's signature plus a trailing Throwable.
    //
    // Java 25: Scoped Values (JEP 506) — RequestContext.CORRELATION_ID is bound
    // per-request by CorrelationIdFilter and is readable here without any explicit
    // parameter passing, making the error message traceable across distributed logs.
    // -------------------------------------------------------------------------

    EmployeeDTO saveEmployeeFallback(EmployeeDTO employeeDTO, Throwable t) {
        String correlationId = RequestContext.CORRELATION_ID.orElse("n/a");
        throw new ServiceUnavailableException(
                "Employee create temporarily unavailable. id=" + employeeDTO.getEmpId()
                + " correlationId=" + correlationId, t);
    }

    EmployeeDTO updateEmployeeFallback(EmployeeDTO employeeDTO, Throwable t) {
        String correlationId = RequestContext.CORRELATION_ID.orElse("n/a");
        throw new ServiceUnavailableException(
                "Employee update temporarily unavailable. id=" + employeeDTO.getEmpId()
                + " correlationId=" + correlationId, t);
    }

    EmployeeDTO patchEmployeeFallback(Long id, String jsonPatchRequest, Throwable t) {
        String correlationId = RequestContext.CORRELATION_ID.orElse("n/a");
        throw new ServiceUnavailableException(
                "Employee patch temporarily unavailable. id=" + id
                + " correlationId=" + correlationId, t);
    }

    void deleteEmployeeFallback(Long id, Throwable t) {
        String correlationId = RequestContext.CORRELATION_ID.orElse("n/a");
        throw new ServiceUnavailableException(
                "Employee delete temporarily unavailable. id=" + id
                + " correlationId=" + correlationId, t);
    }

    void deleteEmployeeWithGreaterSalaryFallback(BigDecimal salary, Throwable t) {
        String correlationId = RequestContext.CORRELATION_ID.orElse("n/a");
        throw new ServiceUnavailableException(
                "Bulk delete temporarily unavailable. salary=" + salary
                + " correlationId=" + correlationId, t);
    }

    EmployeeDTO findEmployeeFallback(Long id, Throwable t) {
        String correlationId = RequestContext.CORRELATION_ID.orElse("n/a");
        throw new ServiceUnavailableException(
                "Employee lookup temporarily unavailable. id=" + id
                + " correlationId=" + correlationId, t);
    }

    List<EmployeeDTO> findEmployeesBySalaryFallback(BigDecimal salary, Throwable t) {
        String correlationId = RequestContext.CORRELATION_ID.orElse("n/a");
        throw new ServiceUnavailableException(
                "Salary lookup temporarily unavailable. salary=" + salary
                + " correlationId=" + correlationId, t);
    }

    List<EmployeeDTO> findEmployeesBySalaryNativeFallback(BigDecimal salary, Throwable t) {
        String correlationId = RequestContext.CORRELATION_ID.orElse("n/a");
        throw new ServiceUnavailableException(
                "Salary lookup (native) temporarily unavailable. salary=" + salary
                + " correlationId=" + correlationId, t);
    }

    PagedEmployeeResponse searchEmployeesFallback(EmployeeSearchCriteria criteria, Throwable t) {
        String correlationId = RequestContext.CORRELATION_ID.orElse("n/a");
        throw new ServiceUnavailableException(
                "Employee search temporarily unavailable. criteria=" + criteria
                + " correlationId=" + correlationId, t);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private EmployeeDTO applyPatchToEmployee(JsonPatch patch, EmployeeDTO employeeDTO)
            throws JsonPatchException, JsonProcessingException {
        JsonNode patched = patch.apply(objectMapper.convertValue(employeeDTO, JsonNode.class));
        return objectMapper.treeToValue(patched, EmployeeDTO.class);
    }
}
