package com.cleancoders.postgresqltranscations.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public class EmployeeDTO implements Serializable {
    @NotNull
    @Positive(message = "Employee ID must be a positive number")
    private Long empId;
    @NotEmpty
    private String empName;
    @NotNull
    private BigDecimal empSalary;

    private String empAddress;       // Organizational unit or location
    private LocalDate empCreatedDate; // Record creation timestamp
    private LocalDate empUpdatedDate; // Last modification timestamp

    public Long getEmpId() {
        return empId;
    }

    public void setEmpId(Long empId) {
        this.empId = empId;
    }

    public String getEmpName() {
        return empName;
    }

    public void setEmpName(String empName) {
        this.empName = empName;
    }

    public BigDecimal getEmpSalary() {
        return empSalary;
    }

    public void setEmpSalary(BigDecimal empSalary) {
        this.empSalary = empSalary;
    }

    public String getEmpAddress() {
        return empAddress;
    }

    public void setEmpAddress(String empAddress) {
        this.empAddress = empAddress;
    }

    public LocalDate getEmpCreatedDate() {
        return empCreatedDate;
    }

    public void setEmpCreatedDate(LocalDate empCreatedDate) {
        this.empCreatedDate = empCreatedDate;
    }

    public LocalDate getEmpUpdatedDate() {
        return empUpdatedDate;
    }

    public void setEmpUpdatedDate(LocalDate empUpdatedDate) {
        this.empUpdatedDate = empUpdatedDate;
    }
}
