package com.cleancoders.postgresqltranscations.controller;

import com.cleancoders.postgresqltranscations.dto.EmployeeDTO;
import com.cleancoders.postgresqltranscations.exception.EmployeeConflictException;
import com.cleancoders.postgresqltranscations.exception.EmployeeNotFoundException;
import com.cleancoders.postgresqltranscations.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmployeeControllerTest {
    @Autowired
    MockMvc mockMvc;
    @MockBean
    EmployeeService employeeService;
    ObjectMapper mapper = new ObjectMapper();
    //Given_Precondition_When_StateUnderTest_Then_ExpectedBehavior
    @Test
    void Given_EmployeeId_When_GetEmployee_Then_SuccessResponse() throws Exception {
        mockMvc.perform(get("/employees/" + TEST_EMPLOYEE_ID))
                .andExpect(status().isOk());
    }

    @Test
    void Given_EmployeeId_When_GetEmployee_Then_FailureResponse() throws Exception {
        given(employeeService.findEmployee(anyLong())).willThrow(new EmployeeNotFoundException());
        mockMvc.perform(get("/employees/" + TEST_EMPLOYEE_ID))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void Given_NotExistingEmployee_When_CreateEmployee_Then_SuccessResponse() throws Exception {
        EmployeeDTO employeeDTO = createEmployeeDTO();
        given(employeeService.saveEmployee(any(EmployeeDTO.class))).willReturn(employeeDTO);
        String requestJson = mapper.writeValueAsString(employeeDTO);

        mockMvc.perform((post("/employees"))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestJson))
                .andExpect(status().isOk());
    }

    @Test
    void Given_Existing_Employee_When_CreateEmployee_Then_FailureResponse() throws Exception {
        given(employeeService.saveEmployee(any(EmployeeDTO.class)))
                .willThrow(new EmployeeConflictException());
        EmployeeDTO employeeDTO = createEmployeeDTO();
        String requestJson = mapper.writeValueAsString(employeeDTO);
        mockMvc.perform((post("/employees"))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(requestJson))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void Given_EmployeeId_When_DeleteEmployee_Then_SuccessResponse() throws Exception {
        //given(employeeService.deleteEmployee(anyLong()))
        mockMvc.perform(delete("/employees/" + TEST_EMPLOYEE_ID))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void Given_EmployeeExists_When_UpdateEmployee_Then_SuccessResponse() throws Exception {
        EmployeeDTO employeeDTO = createEmployeeDTO();
        String requestJson = mapper.writeValueAsString(employeeDTO);
        mockMvc.perform(put("/employees")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(requestJson))
                .andExpect(status().isOk());
    }
    @Test
    void Given_EmployeeExists_When_PatchEmployee_Then_SuccessResponse() throws Exception {
        EmployeeDTO employeeDTO = createEmployeeDTO();
        given(employeeService.patchEmployee(anyLong(), any(JsonPatch.class)))
                .willReturn(employeeDTO);
        String patchRequest = """
                [
                    {
                        "op": "replace",
                        "path": "/empName",
                        "value": "Sandip Salunkhe"
                    }
                ]""";
        mockMvc.perform(patch("/employees/" + TEST_EMPLOYEE_ID)
                .contentType("application/json-patch+json")
                .content(patchRequest)).andExpect(status().isOk());
    }

    @Test
    void Given_Employee_When_PatchEmployee_JsonError_Then_FailureResponse() throws Exception {
        given(employeeService.patchEmployee(anyLong(), any(JsonPatch.class)))
                .willThrow(new JsonPatchException("Error"));
        String patchRequest = """
                [
                    {
                        "op": "replace",
                        "path": "/empName",
                        "value": "Sandip Salunkhe"
                    }
                ]""";
        mockMvc.perform(patch("/employees/" + TEST_EMPLOYEE_ID)
                .contentType("application/json-patch+json")
                .content(patchRequest))
                .andExpect(status().isInternalServerError());
    }

    private EmployeeDTO createEmployeeDTO() {
        EmployeeDTO employeeDTO = new EmployeeDTO();
        employeeDTO.setEmpId(TEST_EMPLOYEE_ID);
        employeeDTO.setEmpName(TEST_EMPLOYEE_NAME);
        employeeDTO.setEmpSalary(TEST_EMPLOYEE_SALARY);
        return employeeDTO;
    }
}