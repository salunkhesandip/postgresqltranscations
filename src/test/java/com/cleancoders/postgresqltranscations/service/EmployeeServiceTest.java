package com.cleancoders.postgresqltranscations.service;

import com.cleancoders.postgresqltranscations.dto.EmployeeDTO;
import com.cleancoders.postgresqltranscations.entity.Employee;
import com.cleancoders.postgresqltranscations.exception.EmployeeConflictException;
import com.cleancoders.postgresqltranscations.mapper.EmployeeMapper;
import com.cleancoders.postgresqltranscations.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.cleancoders.postgresqltranscations.constants.EmployeeConstTest.TEST_EMPLOYEE_ID;
import static com.cleancoders.postgresqltranscations.constants.EmployeeConstTest.TEST_EMPLOYEE_NAME;
import static com.cleancoders.postgresqltranscations.constants.EmployeeConstTest.TEST_EMPLOYEE_SALARY;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {
    private EmployeeService employeeService;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private EmployeeMapper mapper;
    private EmployeeDTO employeeDTO;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeService(employeeRepository, mapper);
        employeeDTO = new EmployeeDTO();
        employeeDTO.setEmpId(TEST_EMPLOYEE_ID);
        employeeDTO.setEmpName(TEST_EMPLOYEE_NAME);
        employeeDTO.setEmpSalary(TEST_EMPLOYEE_SALARY);
    }

    @Test
    void Given_EmployeeDTO_SaveEmployee_SavedEmployee() {
        given(employeeRepository.findById(anyLong())).willReturn(Optional.empty());
        employeeService.saveEmployee(employeeDTO);
    }

    @Test
    void Given_EmployeeDTO_ExistingEmployee_SaveEmployee_ConflictException() {
        given(employeeRepository.findById(anyLong())).willReturn(createEmployee());
        assertThrows(EmployeeConflictException.class,
                ()-> employeeService.saveEmployee(employeeDTO));
    }

    @Test
    void Given_EmployeeExist_FindEmployee_SuccessResponse () {
        given(employeeRepository.findById(anyLong())).willReturn(createEmployee());
        given(mapper.convertToEmployeeDTO(any(Employee.class))).willReturn(employeeDTO);
        assertNotNull(employeeService.findEmployee(TEST_EMPLOYEE_ID));
    }
    Optional <Employee> createEmployee() {
        return Optional.of(new Employee(TEST_EMPLOYEE_ID, TEST_EMPLOYEE_NAME, TEST_EMPLOYEE_SALARY));
    }

}