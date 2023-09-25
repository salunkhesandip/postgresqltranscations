package com.cleancoders.postgresqltranscations.service;

import com.cleancoders.postgresqltranscations.dto.EmployeeDTO;
import com.cleancoders.postgresqltranscations.entity.Employee;
import com.cleancoders.postgresqltranscations.exception.EmployeeConflictException;
import com.cleancoders.postgresqltranscations.exception.EmployeeNotFoundException;
import com.cleancoders.postgresqltranscations.mapper.EmployeeMapper;
import com.cleancoders.postgresqltranscations.repository.EmployeeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
@Transactional
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper mapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EmployeeService(EmployeeRepository employeeRepository, EmployeeMapper mapper) {
        this.employeeRepository = employeeRepository;
        this.mapper = mapper;
    }
    public EmployeeDTO saveEmployee(EmployeeDTO employeeDTO) {
        Optional<Employee> existingEmployee = employeeRepository.findById(employeeDTO.getEmpId());
        if (existingEmployee.isPresent()) {
            throw new EmployeeConflictException("Conflict : Found existing employee id : " + employeeDTO.getEmpId());
        }
        Employee employee = new Employee(employeeDTO.getEmpId(), employeeDTO.getEmpName(), employeeDTO.getEmpSalary());
        employee.setEmpCreatedDate(LocalDate.now());
        employee.setEmpUpdatedDate(LocalDate.now());
        return mapper.convertToEmployeeDTO(employeeRepository.save(employee));
    }

    public EmployeeDTO findEmployee(Long id) {
        Optional<Employee> existingEmployee = employeeRepository.findById(id);
        if (existingEmployee.isPresent()) {
            return mapper.convertToEmployeeDTO(existingEmployee.get());
        } else {
            throw new EmployeeNotFoundException("Employee " + id + " not found");
        }
    }

    public void deleteEmployee(Long id) {
        employeeRepository.deleteById(id);
    }

    public EmployeeDTO updateEmployee(EmployeeDTO employeeDTO) {
        if (employeeRepository.existsById(employeeDTO.getEmpId())) {
            Employee employee = new Employee(employeeDTO.getEmpId(), employeeDTO.getEmpName(), employeeDTO.getEmpSalary());
            employee.setEmpCreatedDate(LocalDate.now());
            employee.setEmpUpdatedDate(LocalDate.now());
            return mapper.convertToEmployeeDTO(employeeRepository.save(employee));
        } else {
            throw new EmployeeNotFoundException("Employee " + employeeDTO.getEmpId() + " not found");
        }
    }

    public EmployeeDTO patchEmployee(Long id, JsonPatch jsonPatchRequest)
            throws JsonPatchException, JsonProcessingException {
        EmployeeDTO employeeDTO = findEmployee(id);
        return updateEmployee(applyPatchToEmployee(jsonPatchRequest, employeeDTO));
    }

    private EmployeeDTO applyPatchToEmployee(JsonPatch patch, EmployeeDTO employeeDTO)
            throws JsonPatchException, JsonProcessingException {
        JsonNode patched = patch.apply(objectMapper.convertValue(employeeDTO, JsonNode.class));
        return objectMapper.treeToValue(patched, EmployeeDTO.class);
    }
}
