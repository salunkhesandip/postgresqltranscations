# REF: testing

Test root: `src/test/java/com/cleancoders/postgresqltranscations/`

| Sub-package   | Class                    | Annotation                            |
|---------------|--------------------------|---------------------------------------|
| `controller/` | `EmployeeControllerTest` | `@WebMvcTest`                         |
| `service/`    | `EmployeeServiceTest`    | `@ExtendWith(MockitoExtension.class)` |
| `constants/`  | `EmployeeTestConstants`  | — shared values only                  |

- Controller tests: `@WebMvcTest` + `MockMvc`; mock service with `@MockitoBean` (not `@MockBean`)
- Service tests: `@ExtendWith(MockitoExtension.class)`; no Spring context; no `@SpringBootTest`
- Assertions: AssertJ `assertThat`; `jsonPath` for every key response field
- Cover happy path + 404 / 409 / 400 for every operation
- All magic values from `EmployeeTestConstants` — never inline literals

## SHARED CONSTANTS TEMPLATE

```java
package com.cleancoders.postgresqltranscations.constants;

import java.math.BigDecimal;
import java.time.LocalDate;

public class EmployeeTestConstants {

    private EmployeeTestConstants() {}

    public static final Long      EMP_ID           = 1L;
    public static final Long      UNKNOWN_EMP_ID   = 99L;
    public static final String    EMP_NAME         = "Alice";
    public static final String    EMP_NAME_UPDATED = "Alice Smith";
    public static final BigDecimal EMP_SALARY      = new BigDecimal("50000.00");
    public static final BigDecimal EMP_SALARY_HIGH = new BigDecimal("80000.00");
    public static final String    EMP_ADDRESS      = "123 Main St";
    public static final String    EMP_CREATED_BY   = "admin";
    public static final LocalDate EMP_CREATED_DATE = LocalDate.of(2024, 1, 15);
}
```

Import in both test classes:
```java
import static com.cleancoders.postgresqltranscations.constants.EmployeeTestConstants.*;
```

## CONTROLLER TEST TEMPLATE

```java
package com.cleancoders.postgresqltranscations.controller;

import com.cleancoders.postgresqltranscations.dto.EmployeeDTO;
import com.cleancoders.postgresqltranscations.exception.EmployeeConflictException;
import com.cleancoders.postgresqltranscations.exception.EmployeeNotFoundException;
import com.cleancoders.postgresqltranscations.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;

import static com.cleancoders.postgresqltranscations.constants.EmployeeTestConstants.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeController.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmployeeControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean  private EmployeeService employeeService;
    @Autowired private ObjectMapper objectMapper;

    // CREATE — 201
    @Test
    void createEmployee_returns201() throws Exception {
        var dto = new EmployeeDTO(EMP_ID, EMP_NAME, EMP_SALARY);
        when(employeeService.saveEmployee(any())).thenReturn(dto);

        mockMvc.perform(post("/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.empId").value(EMP_ID))
            .andExpect(jsonPath("$.empName").value(EMP_NAME))
            .andExpect(jsonPath("$.empSalary").value(50000.00));
    }

    @Test
    void createEmployee_returns409_whenAlreadyExists() throws Exception {
        var dto = new EmployeeDTO(EMP_ID, EMP_NAME, EMP_SALARY);
        when(employeeService.saveEmployee(any()))
            .thenThrow(new EmployeeConflictException("Employee already exists: " + EMP_ID));

        mockMvc.perform(post("/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isConflict());
    }

    @Test
    void createEmployee_returns400_whenBodyInvalid() throws Exception {
        var invalidDto = new EmployeeDTO(null, "", EMP_SALARY);

        mockMvc.perform(post("/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
            .andExpect(status().isBadRequest());
    }

    // GET — 200 / 404
    @Test
    void getEmployee_returns200() throws Exception {
        var dto = new EmployeeDTO(EMP_ID, EMP_NAME, EMP_SALARY);
        when(employeeService.findEmployee(EMP_ID)).thenReturn(dto);

        mockMvc.perform(get("/employees/{id}", EMP_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.empId").value(EMP_ID))
            .andExpect(jsonPath("$.empName").value(EMP_NAME));
    }

    @Test
    void getEmployee_returns404_whenNotFound() throws Exception {
        when(employeeService.findEmployee(UNKNOWN_EMP_ID))
            .thenThrow(new EmployeeNotFoundException("Not found: " + UNKNOWN_EMP_ID));

        mockMvc.perform(get("/employees/{id}", UNKNOWN_EMP_ID))
            .andExpect(status().isNotFound());
    }

    // PUT — 200 / 404
    @Test
    void updateEmployee_returns200() throws Exception {
        var dto = new EmployeeDTO(EMP_ID, EMP_NAME_UPDATED, EMP_SALARY_HIGH);
        when(employeeService.updateEmployee(any())).thenReturn(dto);

        mockMvc.perform(put("/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.empName").value(EMP_NAME_UPDATED));
    }

    @Test
    void updateEmployee_returns404_whenNotFound() throws Exception {
        var dto = new EmployeeDTO(UNKNOWN_EMP_ID, EMP_NAME, EMP_SALARY);
        when(employeeService.updateEmployee(any()))
            .thenThrow(new EmployeeNotFoundException("Not found: " + UNKNOWN_EMP_ID));

        mockMvc.perform(put("/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isNotFound());
    }

    // PATCH — 200 / 404
    @Test
    void patchEmployee_returns200() throws Exception {
        var patched = new EmployeeDTO(EMP_ID, EMP_NAME_UPDATED, EMP_SALARY_HIGH);
        when(employeeService.patchEmployee(eq(EMP_ID), any())).thenReturn(patched);

        mockMvc.perform(patch("/employees/{id}", EMP_ID)
                .contentType("application/json-patch+json")
                .content("""
                    [{ "op": "replace", "path": "/empName", "value": "Alice Smith" }]
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.empName").value(EMP_NAME_UPDATED));
    }

    @Test
    void patchEmployee_returns404_whenNotFound() throws Exception {
        when(employeeService.patchEmployee(eq(UNKNOWN_EMP_ID), any()))
            .thenThrow(new EmployeeNotFoundException("Not found: " + UNKNOWN_EMP_ID));

        mockMvc.perform(patch("/employees/{id}", UNKNOWN_EMP_ID)
                .contentType("application/json-patch+json")
                .content("""
                    [{ "op": "replace", "path": "/empName", "value": "Ghost" }]
                    """))
            .andExpect(status().isNotFound());
    }

    // DELETE — 204 / 404
    @Test
    void deleteEmployee_returns204() throws Exception {
        doNothing().when(employeeService).deleteEmployee(EMP_ID);

        mockMvc.perform(delete("/employees/{id}", EMP_ID))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteEmployee_returns404_whenNotFound() throws Exception {
        doThrow(new EmployeeNotFoundException("Not found: " + UNKNOWN_EMP_ID))
            .when(employeeService).deleteEmployee(UNKNOWN_EMP_ID);

        mockMvc.perform(delete("/employees/{id}", UNKNOWN_EMP_ID))
            .andExpect(status().isNotFound());
    }

    // SALARY FILTER — 200
    @Test
    void bySalary_returns200_withMatchingEmployees() throws Exception {
        var dto = new EmployeeDTO(EMP_ID, EMP_NAME, EMP_SALARY_HIGH);
        when(employeeService.findBySalaryGreaterThan(50000L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/employees/salary/{salary}", 50000L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].empName").value(EMP_NAME));
    }

    @Test
    void bySalaryNative_returns200_withMatchingEmployees() throws Exception {
        var dto = new EmployeeDTO(EMP_ID, EMP_NAME, EMP_SALARY_HIGH);
        when(employeeService.findBySalaryGreaterThanNative(50000L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/employees/salary/native/{salary}", 50000L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].empName").value(EMP_NAME));
    }
}
```

## SERVICE TEST TEMPLATE

```java
package com.cleancoders.postgresqltranscations.service;

import com.cleancoders.postgresqltranscations.dto.EmployeeDTO;
import com.cleancoders.postgresqltranscations.entity.Employee;
import com.cleancoders.postgresqltranscations.exception.EmployeeConflictException;
import com.cleancoders.postgresqltranscations.exception.EmployeeNotFoundException;
import com.cleancoders.postgresqltranscations.mapper.EmployeeMapper;
import com.cleancoders.postgresqltranscations.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;

import static com.cleancoders.postgresqltranscations.constants.EmployeeTestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private EmployeeMapper mapper;
    @InjectMocks private EmployeeService employeeService;

    // CREATE — happy path
    @Test
    void saveEmployee_returnsDTO_whenEmployeeIsNew() {
        var dto    = new EmployeeDTO(EMP_ID, EMP_NAME, EMP_SALARY);
        var entity = new Employee();
        when(employeeRepository.findById(EMP_ID)).thenReturn(Optional.empty());
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(employeeRepository.save(entity)).thenReturn(entity);
        when(mapper.toDTO(entity)).thenReturn(dto);

        assertThat(employeeService.saveEmployee(dto).getEmpName()).isEqualTo(EMP_NAME);
        verify(employeeRepository).save(entity);
    }

    // CREATE — conflict
    @Test
    void saveEmployee_throwsConflict_whenEmployeeAlreadyExists() {
        when(employeeRepository.findById(EMP_ID)).thenReturn(Optional.of(new Employee()));

        assertThrows(EmployeeConflictException.class,
            () -> employeeService.saveEmployee(new EmployeeDTO(EMP_ID, EMP_NAME, EMP_SALARY)));
        verify(employeeRepository, never()).save(any());
    }

    // READ — happy path
    @Test
    void findEmployee_returnsDTO_whenFound() {
        var entity = new Employee();
        var dto    = new EmployeeDTO(EMP_ID, EMP_NAME, EMP_SALARY);
        when(employeeRepository.findById(EMP_ID)).thenReturn(Optional.of(entity));
        when(mapper.toDTO(entity)).thenReturn(dto);

        assertThat(employeeService.findEmployee(EMP_ID).getEmpId()).isEqualTo(EMP_ID);
    }

    // READ — not found
    @Test
    void findEmployee_throwsNotFound_whenMissing() {
        when(employeeRepository.findById(UNKNOWN_EMP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.findEmployee(UNKNOWN_EMP_ID))
            .isInstanceOf(EmployeeNotFoundException.class)
            .hasMessageContaining(String.valueOf(UNKNOWN_EMP_ID));
    }

    // UPDATE — happy path
    @Test
    void updateEmployee_returnsUpdatedDTO_whenFound() {
        var dto    = new EmployeeDTO(EMP_ID, EMP_NAME_UPDATED, EMP_SALARY_HIGH);
        var entity = new Employee();
        when(employeeRepository.findById(EMP_ID)).thenReturn(Optional.of(entity));
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(employeeRepository.save(entity)).thenReturn(entity);
        when(mapper.toDTO(entity)).thenReturn(dto);

        assertThat(employeeService.updateEmployee(dto).getEmpName()).isEqualTo(EMP_NAME_UPDATED);
    }

    // UPDATE — not found
    @Test
    void updateEmployee_throwsNotFound_whenMissing() {
        var dto = new EmployeeDTO(UNKNOWN_EMP_ID, EMP_NAME, EMP_SALARY);
        when(employeeRepository.findById(UNKNOWN_EMP_ID)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> employeeService.updateEmployee(dto));
    }

    // DELETE — happy path
    @Test
    void deleteEmployee_deletesSuccessfully_whenFound() {
        when(employeeRepository.findById(EMP_ID)).thenReturn(Optional.of(new Employee()));

        employeeService.deleteEmployee(EMP_ID);

        verify(employeeRepository).deleteById(EMP_ID);
    }

    // DELETE — not found
    @Test
    void deleteEmployee_throwsNotFound_whenMissing() {
        when(employeeRepository.findById(UNKNOWN_EMP_ID)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class,
            () -> employeeService.deleteEmployee(UNKNOWN_EMP_ID));
        verify(employeeRepository, never()).deleteById(any());
    }

    // SALARY FILTER
    @Test
    void findBySalaryGreaterThan_returnsListOfDTOs() {
        var entity = new Employee();
        var dto    = new EmployeeDTO(EMP_ID, EMP_NAME, EMP_SALARY_HIGH);
        when(employeeRepository.findBySalaryGreaterThan(50000L)).thenReturn(List.of(entity));
        when(mapper.toDTO(entity)).thenReturn(dto);

        List<EmployeeDTO> result = employeeService.findBySalaryGreaterThan(50000L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getEmpName()).isEqualTo(EMP_NAME);
    }
}
```

