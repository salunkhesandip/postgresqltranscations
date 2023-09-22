package com.cleancoders.postgresqltranscations.controller;

import com.cleancoders.postgresqltranscations.dto.EmployeeDTO;
import com.cleancoders.postgresqltranscations.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.validation.Valid;
@Controller
@RequestMapping("/employees")
public class EmployeeController {
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "POST /employees",
            description = "Create a new Employee"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Added Employee"),
            @ApiResponse(responseCode = "409", description = "Employee already exists")
    }
    )

    @PostMapping
    public ResponseEntity<EmployeeDTO> createEmployee(@Valid @RequestBody EmployeeDTO employee) {
        EmployeeDTO createdEmployeeDTO = employeeService.saveEmployee(employee);
        return ResponseEntity.ok().body(createdEmployeeDTO);
    }

    @GetMapping(value = "/id/{id}")
    public ResponseEntity<EmployeeDTO>  getEmployee(@PathVariable("id") int id){
        return ResponseEntity.ok().body(employeeService.findEmployee(id));
    }
}
