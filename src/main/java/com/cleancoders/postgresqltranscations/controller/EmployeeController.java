package com.cleancoders.postgresqltranscations.controller;

import com.cleancoders.postgresqltranscations.dto.EmployeeDTO;
import com.cleancoders.postgresqltranscations.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;
@Controller
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
    @PostMapping
    public ResponseEntity<EmployeeDTO> createEmployee(@Valid @RequestBody EmployeeDTO employee) {
        EmployeeDTO createdEmployeeDTO = employeeService.saveEmployee(employee);
        return ResponseEntity.ok(createdEmployeeDTO);
    }

    @Operation(description = "Retrieve Employee")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee found"),
            @ApiResponse(responseCode = "404", description = "Employee doesn't exist")
    })
    @GetMapping(value = "/{id}")
    public ResponseEntity<EmployeeDTO>  getEmployee(@PathVariable("id") Long id){
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
    @PutMapping()
    public ResponseEntity<EmployeeDTO> updateEmployee(@Valid @RequestBody EmployeeDTO employeeDTO) {
        EmployeeDTO updatedEmployee = employeeService.updateEmployee(employeeDTO);
        return ResponseEntity.ok(updatedEmployee);
    }
}
