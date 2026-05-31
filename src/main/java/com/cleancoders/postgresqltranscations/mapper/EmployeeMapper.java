package com.cleancoders.postgresqltranscations.mapper;

import com.cleancoders.postgresqltranscations.dto.EmployeeDTO;
import com.cleancoders.postgresqltranscations.entity.Employee;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around the shared {@link ModelMapper} bean.
 * Annotated as {@code @Component} — it is not a service (no business logic).
 * The {@link ModelMapper} instance is injected and configured once in {@code AppConfig}.
 */
@Component
public class EmployeeMapper {
    private final ModelMapper modelMapper;

    public EmployeeMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public EmployeeDTO convertToEmployeeDTO(Employee employee) {
        return modelMapper.map(employee, EmployeeDTO.class);
    }

    public Employee convertToEmployee(EmployeeDTO employeeDTO) {
        return modelMapper.map(employeeDTO, Employee.class);
    }
}
