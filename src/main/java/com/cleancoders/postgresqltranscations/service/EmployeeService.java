package com.cleancoders.postgresqltranscations.service;

import com.cleancoders.postgresqltranscations.dto.EmployeeDTO;
import com.cleancoders.postgresqltranscations.entity.Employee;
import com.cleancoders.postgresqltranscations.exception.EmployeeConflictException;
import com.cleancoders.postgresqltranscations.exception.EmployeeNotFoundException;
import com.cleancoders.postgresqltranscations.exception.ServiceUnavailableException;
import com.cleancoders.postgresqltranscations.mapper.EmployeeMapper;
import com.cleancoders.postgresqltranscations.repository.EmployeeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    // Write operations — guarded by backendA (COUNT_BASED, 40% threshold)
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
    public void deleteEmployeeWithGreaterSalary(Long salary) {
        employeeRepository.deleteUsersBySalaryGreater(salary);
    }

    // -------------------------------------------------------------------------
    // Read operations — guarded by databaseCalls (TIME_BASED, 60 s) + retry
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
    public List<EmployeeDTO> findEmployeesBySalary(Long salary) {
        List<Employee> employees = employeeRepository.findBySalaryGreaterThan(salary);
        if (employees.isEmpty()) {
            throw new EmployeeNotFoundException("No employees found with salary above " + salary);
        }
        return employees.stream().map(mapper::convertToEmployeeDTO).toList();
    }

    @Retry(name = "databaseCalls")
    @CircuitBreaker(name = "databaseCalls", fallbackMethod = "findEmployeesBySalaryNativeFallback")
    @Transactional(readOnly = true)
    public List<EmployeeDTO> findEmployeesBySalaryNative(Long salary) {
        List<Employee> employees = employeeRepository.findBySalaryGreaterThanNative(salary);
        if (employees.isEmpty()) {
            throw new EmployeeNotFoundException("No employees found with salary above " + salary);
        }
        return employees.stream().map(mapper::convertToEmployeeDTO).toList();
    }

    // -------------------------------------------------------------------------
    // Circuit-breaker fallback methods
    // Package-private so they are directly testable from the same package.
    // Each must match the guarded method's signature plus a trailing Throwable.
    // -------------------------------------------------------------------------

    EmployeeDTO saveEmployeeFallback(EmployeeDTO employeeDTO, Throwable t) {
        throw new ServiceUnavailableException(
                "Employee create temporarily unavailable. id=" + employeeDTO.getEmpId(), t);
    }

    EmployeeDTO updateEmployeeFallback(EmployeeDTO employeeDTO, Throwable t) {
        throw new ServiceUnavailableException(
                "Employee update temporarily unavailable. id=" + employeeDTO.getEmpId(), t);
    }

    EmployeeDTO patchEmployeeFallback(Long id, String jsonPatchRequest, Throwable t) {
        throw new ServiceUnavailableException(
                "Employee patch temporarily unavailable. id=" + id, t);
    }

    void deleteEmployeeFallback(Long id, Throwable t) {
        throw new ServiceUnavailableException(
                "Employee delete temporarily unavailable. id=" + id, t);
    }

    void deleteEmployeeWithGreaterSalaryFallback(Long salary, Throwable t) {
        throw new ServiceUnavailableException(
                "Bulk delete temporarily unavailable. salary=" + salary, t);
    }

    EmployeeDTO findEmployeeFallback(Long id, Throwable t) {
        throw new ServiceUnavailableException(
                "Employee lookup temporarily unavailable. id=" + id, t);
    }

    List<EmployeeDTO> findEmployeesBySalaryFallback(Long salary, Throwable t) {
        throw new ServiceUnavailableException(
                "Salary lookup temporarily unavailable. salary=" + salary, t);
    }

    List<EmployeeDTO> findEmployeesBySalaryNativeFallback(Long salary, Throwable t) {
        throw new ServiceUnavailableException(
                "Salary lookup (native) temporarily unavailable. salary=" + salary, t);
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
