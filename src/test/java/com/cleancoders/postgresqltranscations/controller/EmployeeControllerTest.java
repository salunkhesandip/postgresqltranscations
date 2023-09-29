package com.cleancoders.postgresqltranscations.controller;

import com.cleancoders.postgresqltranscations.exception.EmployeeNotFoundException;
import com.cleancoders.postgresqltranscations.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.cleancoders.postgresqltranscations.constants.EmployeeConstTest.TEST_EMPLOYEE_ID;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmployeeControllerTest {
    @Autowired
    MockMvc mockMvc;
    @MockBean
    EmployeeService employeeService;

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
}