package com.cleancoders.postgresqltranscations.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "employee")
public class Employee {
    @Id
    @Column(name = "emp_id")
    private Long empId;
    @Column(name = "emp_name")
    private String empName;
    @Column(name = "emp_salary")
    private Long empSalary;

    @Column(name = "emp_created_date")
    private LocalDate empCreatedDate;

    @Column(name = "emp_updated_date")
    private LocalDate empUpdatedDate;

    public Employee() {
    }

    public Employee(Long empId, String empName, Long empSalary) {
        this.empId = empId;
        this.empName = empName;
        this.empSalary = empSalary;
    }

    public Employee(Employee employee) {
        this.empId = empId;
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

    public Long getEmpSalary() {
        return empSalary;
    }

    public void setEmpSalary(Long empSalary) {
        this.empSalary = empSalary;
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

    @Override
    public String toString() {
        return "Employee{" +
                "empId=" + empId +
                ", empName='" + empName + '\'' +
                ", empSalary=" + empSalary +
                ", empCreatedDate=" + empCreatedDate +
                ", empUpdatedDate=" + empUpdatedDate +
                '}';
    }
}
