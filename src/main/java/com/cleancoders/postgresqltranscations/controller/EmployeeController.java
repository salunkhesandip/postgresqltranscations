package com.cleancoders.postgresqltranscations.controller;

import com.cleancoders.postgresqltranscations.dto.EmployeeDTO;
import com.cleancoders.postgresqltranscations.service.EmployeeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

import javax.validation.Valid;
@RestController
@RequestMapping("/employees")
public class EmployeeController {
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @Operation(description = "Create a new Employee")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Added Employee"),
            @ApiResponse(responseCode = "409", description = "Employee already exists")
    }
    )
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmployeeDTO> createEmployee(@Valid @RequestBody EmployeeDTO employee) {
        EmployeeDTO createdEmployeeDTO = employeeService.saveEmployee(employee);
        return ResponseEntity.ok(createdEmployeeDTO);
    }

    @Operation(description = "Retrieve Employee")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee found"),
            @ApiResponse(responseCode = "404", description = "Employee doesn't exist")
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmployeeDTO>  getEmployee(@PathVariable("id") Long id) {
        return ResponseEntity.ok(employeeService.findEmployee(id));
    }

    @Operation(description = "Delete Employee")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Employee deleted")})
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> removeEmployee(@PathVariable("id") Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(description = "Update Employee")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee updated successfully"),
            @ApiResponse(responseCode = "404", description = "Employee not found")
    })
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmployeeDTO> updateEmployee(@Valid @RequestBody EmployeeDTO employeeDTO) {
        EmployeeDTO updatedEmployee = employeeService.updateEmployee(employeeDTO);
        return ResponseEntity.ok(updatedEmployee);
    }

    @Operation(description = "Partial Update/Patch Employee")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee patched successfully")
    })
    @PatchMapping(value = "/{id}", consumes = "application/json-patch+json")
    public ResponseEntity<EmployeeDTO> patchEmployee(@PathVariable("id") Long id,
                                                     @RequestBody JsonPatch jsonPatchRequest) {
        EmployeeDTO patchedEmployee;
        try {
            patchedEmployee = employeeService.patchEmployee(id, jsonPatchRequest);
        } catch (JsonPatchException | JsonProcessingException e) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok(patchedEmployee);
    }
}
