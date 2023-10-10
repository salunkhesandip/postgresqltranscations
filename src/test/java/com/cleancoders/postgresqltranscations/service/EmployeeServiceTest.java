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
import com.github.fge.jsonpatch.JsonPatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;

import static com.cleancoders.postgresqltranscations.constants.EmployeeConstTest.TEST_EMPLOYEE_ID;
import static com.cleancoders.postgresqltranscations.constants.EmployeeConstTest.TEST_EMPLOYEE_NAME;
import static com.cleancoders.postgresqltranscations.constants.EmployeeConstTest.TEST_EMPLOYEE_SALARY;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
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
                () -> employeeService.saveEmployee(employeeDTO));
    }

    @Test
    void Given_EmployeeExist_FindEmployee_SuccessResponse() {
        given(employeeRepository.findById(anyLong())).willReturn(createEmployee());
        given(mapper.convertToEmployeeDTO(any(Employee.class))).willReturn(employeeDTO);
        assertNotNull(employeeService.findEmployee(TEST_EMPLOYEE_ID));
    }

    @Test
    void Given_EmployeeNotExist_FindEmployee_EmployeeNotFoundException() {
        given(employeeRepository.findById(anyLong())).willReturn(Optional.empty());
        assertThrows(EmployeeNotFoundException.class, () -> employeeService.findEmployee(TEST_EMPLOYEE_ID));
    }

    @Test
    void Given_EmployeeId_DeleteById_Deleted() {
        employeeService.deleteEmployee(TEST_EMPLOYEE_ID);
        verify(employeeRepository).deleteById(TEST_EMPLOYEE_ID);
    }

    @Test
    void Given_EmployeeDTO_UpdateEmployee_Updated() {
        given(employeeRepository.existsById(anyLong())).willReturn(true);
        given(employeeRepository.save(any(Employee.class)))
                .willReturn(new Employee(TEST_EMPLOYEE_ID, TEST_EMPLOYEE_NAME, TEST_EMPLOYEE_SALARY));
        given(mapper.convertToEmployeeDTO(any(Employee.class))).willReturn(employeeDTO);
        assertNotNull(employeeService.updateEmployee(employeeDTO));
    }

    @Test
    void Given_EmployeeNotExist_UpdateEmployee_EmployeeNotFoundException() {
        given(employeeRepository.existsById(anyLong())).willReturn(false);
        assertThrows(EmployeeNotFoundException.class, () -> employeeService.updateEmployee(employeeDTO));
    }

    @Test
    void Given_PatchRequestExistingEmployee_Patch_Success() throws IOException, JsonPatchException {
        EmployeeService employeeServicespy = Mockito.spy(employeeService);
        doReturn(employeeDTO).when(employeeServicespy).findEmployee(TEST_EMPLOYEE_ID);
        given(employeeRepository.existsById(anyLong())).willReturn(true);
        given(employeeRepository.save(any(Employee.class)))
                .willReturn(new Employee(TEST_EMPLOYEE_ID, TEST_EMPLOYEE_NAME, TEST_EMPLOYEE_SALARY));
        given(mapper.convertToEmployeeDTO(any(Employee.class))).willReturn(employeeDTO);

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonPatchString = """
                [
                    {
                        "op": "replace",
                        "path": "/empName",
                        "value": "Sandip Salunkhe"
                    }
                ]""";
        JsonNode jsonNode = objectMapper.readTree(jsonPatchString);
        JsonPatch jsonPatch = JsonPatch.fromJson(jsonNode);

        employeeServicespy.patchEmployee(TEST_EMPLOYEE_ID, jsonPatch);
    }

    Optional<Employee> createEmployee() {
        return Optional.of(new Employee(TEST_EMPLOYEE_ID, TEST_EMPLOYEE_NAME, TEST_EMPLOYEE_SALARY));
    }
}