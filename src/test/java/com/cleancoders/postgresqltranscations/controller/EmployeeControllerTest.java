package com.cleancoders.postgresqltranscations.controller;

import com.cleancoders.postgresqltranscations.dto.EmployeeDTO;
import com.cleancoders.postgresqltranscations.exception.EmployeeConflictException;
import com.cleancoders.postgresqltranscations.exception.EmployeeNotFoundException;
import com.cleancoders.postgresqltranscations.exception.ServiceUnavailableException;
import com.cleancoders.postgresqltranscations.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatchException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.cleancoders.postgresqltranscations.constants.EmployeeConstTest.TEST_EMPLOYEE_ID;
import static com.cleancoders.postgresqltranscations.constants.EmployeeConstTest.TEST_EMPLOYEE_NAME;
import static com.cleancoders.postgresqltranscations.constants.EmployeeConstTest.TEST_EMPLOYEE_SALARY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    EmployeeService employeeService;

    ObjectMapper mapper = new ObjectMapper();

    // -------------------------------------------------------------------------
    // GET /employees/{id}
    // -------------------------------------------------------------------------

    @Test
    void Given_EmployeeId_When_GetEmployee_Then_SuccessResponse() throws Exception {
        given(employeeService.findEmployee(TEST_EMPLOYEE_ID)).willReturn(createEmployeeDTO());
        mockMvc.perform(get("/employees/" + TEST_EMPLOYEE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empId").value(TEST_EMPLOYEE_ID))
                .andExpect(jsonPath("$.empName").value(TEST_EMPLOYEE_NAME));
    }

    @Test
    void Given_EmployeeId_When_GetEmployee_Then_404Response() throws Exception {
        given(employeeService.findEmployee(anyLong())).willThrow(new EmployeeNotFoundException("not found"));
        mockMvc.perform(get("/employees/" + TEST_EMPLOYEE_ID))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // POST /employees
    // -------------------------------------------------------------------------

    @Test
    void Given_NotExistingEmployee_When_CreateEmployee_Then_201Response() throws Exception {
        EmployeeDTO employeeDTO = createEmployeeDTO();
        given(employeeService.saveEmployee(any(EmployeeDTO.class))).willReturn(employeeDTO);

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(mapper.writeValueAsString(employeeDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.empId").value(TEST_EMPLOYEE_ID));
    }

    @Test
    void Given_ExistingEmployee_When_CreateEmployee_Then_409Response() throws Exception {
        given(employeeService.saveEmployee(any(EmployeeDTO.class)))
                .willThrow(new EmployeeConflictException("conflict"));

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(mapper.writeValueAsString(createEmployeeDTO())))
                .andExpect(status().isConflict());
    }

    // -------------------------------------------------------------------------
    // PUT /employees
    // -------------------------------------------------------------------------

    @Test
    void Given_EmployeeExists_When_UpdateEmployee_Then_200Response() throws Exception {
        EmployeeDTO employeeDTO = createEmployeeDTO();
        given(employeeService.updateEmployee(any(EmployeeDTO.class))).willReturn(employeeDTO);

        mockMvc.perform(put("/employees")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(mapper.writeValueAsString(employeeDTO)))
                .andExpect(status().isOk());
    }

    @Test
    void Given_EmployeeNotExist_When_UpdateEmployee_Then_404Response() throws Exception {
        given(employeeService.updateEmployee(any(EmployeeDTO.class)))
                .willThrow(new EmployeeNotFoundException("not found"));

        mockMvc.perform(put("/employees")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(mapper.writeValueAsString(createEmployeeDTO())))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // DELETE /employees/{id}
    // -------------------------------------------------------------------------

    @Test
    void Given_EmployeeId_When_DeleteEmployee_Then_204Response() throws Exception {
        mockMvc.perform(delete("/employees/" + TEST_EMPLOYEE_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void Given_EmployeeNotExist_When_DeleteEmployee_Then_404Response() throws Exception {
        org.mockito.Mockito.doThrow(new EmployeeNotFoundException("not found"))
                .when(employeeService).deleteEmployee(anyLong());
        mockMvc.perform(delete("/employees/" + TEST_EMPLOYEE_ID))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // PATCH /employees/{id}
    // -------------------------------------------------------------------------

    @Test
    void Given_EmployeeExists_When_PatchEmployee_Then_200Response() throws Exception {
        given(employeeService.patchEmployee(anyLong(), any(String.class)))
                .willReturn(createEmployeeDTO());

        mockMvc.perform(patch("/employees/" + TEST_EMPLOYEE_ID)
                        .contentType("application/json-patch+json")
                        .content(patchBody()))
                .andExpect(status().isOk());
    }

    @Test
    void Given_InvalidPatch_When_PatchEmployee_Then_422Response() throws Exception {
        given(employeeService.patchEmployee(anyLong(), any(String.class)))
                .willThrow(new JsonPatchException("bad patch"));

        mockMvc.perform(patch("/employees/" + TEST_EMPLOYEE_ID)
                        .contentType("application/json-patch+json")
                        .content(patchBody()))
                .andExpect(status().isUnprocessableEntity());
    }

    // -------------------------------------------------------------------------
    // Circuit-breaker 503 tests
    // -------------------------------------------------------------------------

    @Test
    void Given_CircuitOpen_When_GetEmployee_Then_503Response() throws Exception {
        given(employeeService.findEmployee(anyLong()))
                .willThrow(new ServiceUnavailableException("temporarily unavailable"));
        mockMvc.perform(get("/employees/" + TEST_EMPLOYEE_ID))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void Given_CircuitOpen_When_CreateEmployee_Then_503Response() throws Exception {
        given(employeeService.saveEmployee(any(EmployeeDTO.class)))
                .willThrow(new ServiceUnavailableException("temporarily unavailable"));
        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(mapper.writeValueAsString(createEmployeeDTO())))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void Given_CircuitOpen_When_UpdateEmployee_Then_503Response() throws Exception {
        given(employeeService.updateEmployee(any(EmployeeDTO.class)))
                .willThrow(new ServiceUnavailableException("temporarily unavailable"));
        mockMvc.perform(put("/employees")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(mapper.writeValueAsString(createEmployeeDTO())))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void Given_CircuitOpen_When_DeleteEmployee_Then_503Response() throws Exception {
        org.mockito.Mockito.doThrow(new ServiceUnavailableException("temporarily unavailable"))
                .when(employeeService).deleteEmployee(anyLong());
        mockMvc.perform(delete("/employees/" + TEST_EMPLOYEE_ID))
                .andExpect(status().isServiceUnavailable());
    }

    // -------------------------------------------------------------------------
    // Salary endpoints
    // -------------------------------------------------------------------------

    @Test
    void Given_Salary_When_FindBySalary_Then_200Response() throws Exception {
        given(employeeService.findEmployeesBySalary(anyLong()))
                .willReturn(List.of(createEmployeeDTO()));
        mockMvc.perform(get("/employees/salary/" + TEST_EMPLOYEE_SALARY.longValue()))
                .andExpect(status().isOk());
    }

    @Test
    void Given_Salary_When_FindBySalaryNative_Then_200Response() throws Exception {
        given(employeeService.findEmployeesBySalaryNative(anyLong()))
                .willReturn(List.of(createEmployeeDTO()));
        mockMvc.perform(get("/employees/salary/native/" + TEST_EMPLOYEE_SALARY.longValue()))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private EmployeeDTO createEmployeeDTO() {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setEmpId(TEST_EMPLOYEE_ID);
        dto.setEmpName(TEST_EMPLOYEE_NAME);
        dto.setEmpSalary(TEST_EMPLOYEE_SALARY);
        return dto;
    }

    private String patchBody() {
        return """
                [{"op":"replace","path":"/empName","value":"Updated Name"}]""";
    }
}