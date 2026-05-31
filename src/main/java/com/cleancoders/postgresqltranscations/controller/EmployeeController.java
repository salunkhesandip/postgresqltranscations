package com.cleancoders.postgresqltranscations.controller;

import com.cleancoders.postgresqltranscations.dto.EmployeeDTO;
import com.cleancoders.postgresqltranscations.service.EmployeeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.fge.jsonpatch.JsonPatchException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Employee", description = "Employee management API")
@RestController
@RequestMapping("/employees")
public class EmployeeController {
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @Operation(summary = "Create a new Employee")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Employee created"),
            @ApiResponse(responseCode = "409", description = "Employee already exists")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmployeeDTO> createEmployee(@Valid @RequestBody EmployeeDTO employee) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.saveEmployee(employee));
    }

    @Operation(summary = "Retrieve Employee by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee found"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmployeeDTO> getEmployee(@PathVariable("id") Long id) {
        return ResponseEntity.ok(employeeService.findEmployee(id));
    }

    @Operation(summary = "Delete Employee by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Employee deleted"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> removeEmployee(@PathVariable("id") Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Full update of an Employee")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee updated successfully"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmployeeDTO> updateEmployee(@Valid @RequestBody EmployeeDTO employeeDTO) {
        return ResponseEntity.ok(employeeService.updateEmployee(employeeDTO));
    }

    @Operation(summary = "Partial update (RFC 6902 JSON Patch) of an Employee")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee patched successfully"),
            @ApiResponse(responseCode = "404", description = "Employee not found"),
            @ApiResponse(responseCode = "422", description = "Invalid JSON Patch document")
    })
    @PatchMapping(value = "/{id}", consumes = "application/json-patch+json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmployeeDTO> patchEmployee(@PathVariable("id") Long id,
                                                     @RequestBody String jsonPatchRequest)
            throws JsonPatchException, JsonProcessingException {
        return ResponseEntity.ok(employeeService.patchEmployee(id, jsonPatchRequest));
    }

    @Operation(summary = "List Employees with salary above threshold (JPQL)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee list"),
            @ApiResponse(responseCode = "404", description = "No employees found")
    })
    @GetMapping(value = "/salary/{salary}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EmployeeDTO>> findEmployeesBySalary(@PathVariable("salary") Long salary) {
        return ResponseEntity.ok(employeeService.findEmployeesBySalary(salary));
    }

    @Operation(summary = "List Employees with salary above threshold (native SQL)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee list"),
            @ApiResponse(responseCode = "404", description = "No employees found")
    })
    @GetMapping(value = "/salary/native/{salary}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EmployeeDTO>> findEmployeesBySalaryNative(@PathVariable("salary") Long salary) {
        return ResponseEntity.ok(employeeService.findEmployeesBySalaryNative(salary));
    }

    @Operation(summary = "Delete Employees with salary above threshold")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Employees deleted")
    })
    @DeleteMapping(value = "/salary/{salary}")
    public ResponseEntity<Void> deleteEmployeeWithGreaterSalary(@PathVariable("salary") Long salary) {
        employeeService.deleteEmployeeWithGreaterSalary(salary);
        return ResponseEntity.noContent().build();
    }
}