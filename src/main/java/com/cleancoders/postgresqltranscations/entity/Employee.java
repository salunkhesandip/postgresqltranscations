package com.cleancoders.postgresqltranscations.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employee", schema = "company")
public class Employee {
    @Id
    @Column(name = "emp_id")
    private Long empId;

    @Column(name = "emp_name")
    private String empName;

    @Column(name = "emp_salary")
    private BigDecimal empSalary;

    @Column(name = "emp_address")
    private String empAddress;

    @Column(name = "emp_created_date")
    private LocalDate empCreatedDate;

    @Column(name = "emp_updated_date")
    private LocalDate empUpdatedDate;

    @Column(name = "emp_created_by", length = 30)
    private String empCreatedBy;

    @Column(name = "emp_update_by", length = 30)
    private String empUpdateBy;

    public Employee() {
    }

    public Employee(Long empId, String empName, java.math.BigDecimal empSalary) {
        this.empId = empId;
        this.empName = empName;
        this.empSalary = empSalary;
    }

    public Employee(Employee employee) {
        this.empId = employee.empId;
        this.empName = employee.empName;
        this.empSalary = employee.empSalary;
        this.empAddress = employee.empAddress;
        this.empCreatedDate = employee.empCreatedDate;
        this.empUpdatedDate = employee.empUpdatedDate;
        this.empCreatedBy = employee.empCreatedBy;
        this.empUpdateBy = employee.empUpdateBy;
    }
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

    public String getEmpCreatedBy() {
        return empCreatedBy;
    }

    public void setEmpCreatedBy(String empCreatedBy) {
        this.empCreatedBy = empCreatedBy;
    }

    public String getEmpUpdateBy() {
        return empUpdateBy;
    }

    public void setEmpUpdateBy(String empUpdateBy) {
        this.empUpdateBy = empUpdateBy;
    }

    @Override
    public String toString() {
        return "Employee{" +
                "empId=" + empId +
                ", empName='" + empName + '\'' +
                ", empSalary=" + empSalary +
                ", empAddress='" + empAddress + '\'' +
                ", empCreatedDate=" + empCreatedDate +
                ", empUpdatedDate=" + empUpdatedDate +
                ", empCreatedBy='" + empCreatedBy + '\'' +
                ", empUpdateBy='" + empUpdateBy + '\'' +
                '}';
    }
}
