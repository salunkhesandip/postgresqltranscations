package com.cleancoders.postgresqltranscations.service;

import com.cleancoders.postgresqltranscations.dto.EmployeeDTO;
import com.cleancoders.postgresqltranscations.entity.Employee;
import com.cleancoders.postgresqltranscations.exception.EmployeeConflictException;
import com.cleancoders.postgresqltranscations.exception.EmployeeNotFoundException;
import com.cleancoders.postgresqltranscations.exception.ServiceUnavailableException;
import com.cleancoders.postgresqltranscations.mapper.EmployeeMapper;
import com.cleancoders.postgresqltranscations.repository.EmployeeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.cleancoders.postgresqltranscations.constants.EmployeeConstTest.TEST_EMPLOYEE_ID;
import static com.cleancoders.postgresqltranscations.constants.EmployeeConstTest.TEST_EMPLOYEE_NAME;
import static com.cleancoders.postgresqltranscations.constants.EmployeeConstTest.TEST_EMPLOYEE_SALARY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
        // Inject a plain ObjectMapper — EmployeeDTO has no LocalDate fields so no JavaTimeModule needed.
        employeeService = new EmployeeService(employeeRepository, mapper, new ObjectMapper());
        employeeDTO = new EmployeeDTO();
        employeeDTO.setEmpId(TEST_EMPLOYEE_ID);
        employeeDTO.setEmpName(TEST_EMPLOYEE_NAME);
        employeeDTO.setEmpSalary(TEST_EMPLOYEE_SALARY);
    }

    // -------------------------------------------------------------------------
    // saveEmployee
    // -------------------------------------------------------------------------

    @Test
    void Given_NewEmployee_When_SaveEmployee_Then_Success() {
        given(employeeRepository.existsById(TEST_EMPLOYEE_ID)).willReturn(false);
        given(mapper.convertToEmployee(any(EmployeeDTO.class))).willReturn(createEmployee());
        given(mapper.convertToEmployeeDTO(any(Employee.class))).willReturn(employeeDTO);
        given(employeeRepository.save(any(Employee.class))).willReturn(createEmployee());

        EmployeeDTO result = employeeService.saveEmployee(employeeDTO);

        assertNotNull(result);
    }

    @Test
    void Given_ExistingEmployee_When_SaveEmployee_Then_ConflictException() {
        given(employeeRepository.existsById(TEST_EMPLOYEE_ID)).willReturn(true);
        assertThrows(EmployeeConflictException.class, () -> employeeService.saveEmployee(employeeDTO));
        verify(employeeRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // findEmployee
    // -------------------------------------------------------------------------

    @Test
    void Given_EmployeeExists_When_FindEmployee_Then_Success() {
        given(employeeRepository.findById(TEST_EMPLOYEE_ID)).willReturn(Optional.of(createEmployee()));
        given(mapper.convertToEmployeeDTO(any(Employee.class))).willReturn(employeeDTO);

        EmployeeDTO result = employeeService.findEmployee(TEST_EMPLOYEE_ID);

        assertThat(result.getEmpId()).isEqualTo(TEST_EMPLOYEE_ID);
    }

    @Test
    void Given_EmployeeNotExist_When_FindEmployee_Then_NotFoundException() {
        given(employeeRepository.findById(anyLong())).willReturn(Optional.empty());
        assertThrows(EmployeeNotFoundException.class, () -> employeeService.findEmployee(TEST_EMPLOYEE_ID));
    }

    // -------------------------------------------------------------------------
    // deleteEmployee
    // -------------------------------------------------------------------------

    @Test
    void Given_EmployeeExists_When_DeleteEmployee_Then_Deleted() {
        given(employeeRepository.existsById(TEST_EMPLOYEE_ID)).willReturn(true);
        employeeService.deleteEmployee(TEST_EMPLOYEE_ID);
        verify(employeeRepository).deleteById(TEST_EMPLOYEE_ID);
    }

    @Test
    void Given_EmployeeNotExist_When_DeleteEmployee_Then_NotFoundException() {
        given(employeeRepository.existsById(TEST_EMPLOYEE_ID)).willReturn(false);
        assertThrows(EmployeeNotFoundException.class, () -> employeeService.deleteEmployee(TEST_EMPLOYEE_ID));
        verify(employeeRepository, never()).deleteById(any());
    }

    // -------------------------------------------------------------------------
    // updateEmployee
    // -------------------------------------------------------------------------

    @Test
    void Given_EmployeeDTO_When_UpdateEmployee_Then_Updated() {
        given(employeeRepository.findById(TEST_EMPLOYEE_ID)).willReturn(Optional.of(createEmployee()));
        given(employeeRepository.save(any(Employee.class))).willReturn(createEmployee());
        given(mapper.convertToEmployeeDTO(any(Employee.class))).willReturn(employeeDTO);

        assertNotNull(employeeService.updateEmployee(employeeDTO));
    }

    @Test
    void Given_EmployeeNotExist_When_UpdateEmployee_Then_NotFoundException() {
        given(employeeRepository.findById(anyLong())).willReturn(Optional.empty());
        assertThrows(EmployeeNotFoundException.class, () -> employeeService.updateEmployee(employeeDTO));
    }

    // -------------------------------------------------------------------------
    // patchEmployee
    // -------------------------------------------------------------------------

    @Test
    void Given_PatchRequest_When_PatchEmployee_Then_Success() throws IOException, JsonPatchException {
        EmployeeService spy = Mockito.spy(employeeService);
        doReturn(employeeDTO).when(spy).findEmployee(TEST_EMPLOYEE_ID);
        given(employeeRepository.findById(TEST_EMPLOYEE_ID)).willReturn(Optional.of(createEmployee()));
        given(employeeRepository.save(any(Employee.class))).willReturn(createEmployee());
        given(mapper.convertToEmployeeDTO(any(Employee.class))).willReturn(employeeDTO);

        spy.patchEmployee(TEST_EMPLOYEE_ID, """
                [{"op":"replace","path":"/empName","value":"Sandip Salunkhe"}]""");
    }

    // -------------------------------------------------------------------------
    // Circuit-breaker fallback tests (called directly — no Spring AOP context)
    // -------------------------------------------------------------------------

    @Test
    void saveEmployeeFallback_throwsServiceUnavailable() {
        var cause = new RuntimeException("DB down");
        assertThatThrownBy(() -> employeeService.saveEmployeeFallback(employeeDTO, cause))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining(String.valueOf(TEST_EMPLOYEE_ID));
    }

    @Test
    void findEmployeeFallback_throwsServiceUnavailable() {
        var cause = new RuntimeException("DB down");
        assertThatThrownBy(() -> employeeService.findEmployeeFallback(TEST_EMPLOYEE_ID, cause))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining(String.valueOf(TEST_EMPLOYEE_ID));
    }

    @Test
    void updateEmployeeFallback_throwsServiceUnavailable() {
        var cause = new RuntimeException("DB down");
        assertThatThrownBy(() -> employeeService.updateEmployeeFallback(employeeDTO, cause))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining(String.valueOf(TEST_EMPLOYEE_ID));
    }

    @Test
    void deleteEmployeeFallback_throwsServiceUnavailable() {
        var cause = new RuntimeException("DB down");
        assertThatThrownBy(() -> employeeService.deleteEmployeeFallback(TEST_EMPLOYEE_ID, cause))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining(String.valueOf(TEST_EMPLOYEE_ID));
    }

    @Test
    void patchEmployeeFallback_throwsServiceUnavailable() {
        var cause = new RuntimeException("DB down");
        assertThatThrownBy(() -> employeeService.patchEmployeeFallback(TEST_EMPLOYEE_ID, "[]", cause))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining(String.valueOf(TEST_EMPLOYEE_ID));
    }

    @Test
    void findEmployeesBySalaryFallback_throwsServiceUnavailable() {
        Long testSalary = TEST_EMPLOYEE_SALARY.longValue();
        assertThatThrownBy(() -> employeeService.findEmployeesBySalaryFallback(testSalary, new RuntimeException()))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining(String.valueOf(testSalary));
    }

    @Test
    void findEmployeesBySalaryNativeFallback_throwsServiceUnavailable() {
        Long testSalary = TEST_EMPLOYEE_SALARY.longValue();
        assertThatThrownBy(() -> employeeService.findEmployeesBySalaryNativeFallback(testSalary, new RuntimeException()))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining(String.valueOf(testSalary));
    }

    @Test
    void deleteEmployeeWithGreaterSalaryFallback_throwsServiceUnavailable() {
        Long testSalary = TEST_EMPLOYEE_SALARY.longValue();
        assertThatThrownBy(() -> employeeService.deleteEmployeeWithGreaterSalaryFallback(testSalary, new RuntimeException()))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining(String.valueOf(testSalary));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    Employee createEmployee() {
        return new Employee(TEST_EMPLOYEE_ID, TEST_EMPLOYEE_NAME, TEST_EMPLOYEE_SALARY);
    }
}