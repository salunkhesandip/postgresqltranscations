# REF: entities

Package: `com.cleancoders.postgresqltranscations.entity`

## COLUMN MAP — `company.employee`

| Java field       | DB column          | Java type    | DB type         | Notes                             |
|------------------|--------------------|--------------|-----------------|-----------------------------------|
| `empId`          | `emp_id`           | `Long`       | `BIGINT`        | PK — caller-supplied, no auto-gen |
| `empName`        | `emp_name`         | `String`     | `VARCHAR(255)`  |                                   |
| `empSalary`      | `emp_salary`       | `BigDecimal` | `NUMERIC(19,2)` | Never `double`/`float`            |
| `empAddress`     | `emp_address`      | `String`     | `VARCHAR(255)`  |                                   |
| `empCreatedDate` | `emp_created_date` | `LocalDate`  | `DATE`          |                                   |
| `empUpdatedDate` | `emp_updated_date` | `LocalDate`  | `DATE`          |                                   |
| `empCreatedBy`   | `emp_created_by`   | `String`     | `VARCHAR(30)`   |                                   |
| `empUpdateBy`    | `emp_update_by`    | `String`     | `VARCHAR(30)`   | No trailing `d` in column name    |

## ENTITY TEMPLATE

```java
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
    private BigDecimal empSalary;           // NUMERIC(19,2) — never double/float

    @Column(name = "emp_address")
    private String empAddress;

    @Column(name = "emp_created_date")
    private LocalDate empCreatedDate;

    @Column(name = "emp_updated_date")
    private LocalDate empUpdatedDate;

    @Column(name = "emp_created_by")
    private String empCreatedBy;

    @Column(name = "emp_update_by")         // no trailing 'd'
    private String empUpdateBy;

    public Employee() {}                    // required by JPA

    public Long getEmpId() { return empId; }
    public void setEmpId(Long empId) { this.empId = empId; }

    public String getEmpName() { return empName; }
    public void setEmpName(String empName) { this.empName = empName; }

    public BigDecimal getEmpSalary() { return empSalary; }
    public void setEmpSalary(BigDecimal empSalary) { this.empSalary = empSalary; }

    public String getEmpAddress() { return empAddress; }
    public void setEmpAddress(String empAddress) { this.empAddress = empAddress; }

    public LocalDate getEmpCreatedDate() { return empCreatedDate; }
    public void setEmpCreatedDate(LocalDate empCreatedDate) { this.empCreatedDate = empCreatedDate; }

    public LocalDate getEmpUpdatedDate() { return empUpdatedDate; }
    public void setEmpUpdatedDate(LocalDate empUpdatedDate) { this.empUpdatedDate = empUpdatedDate; }

    public String getEmpCreatedBy() { return empCreatedBy; }
    public void setEmpCreatedBy(String empCreatedBy) { this.empCreatedBy = empCreatedBy; }

    public String getEmpUpdateBy() { return empUpdateBy; }
    public void setEmpUpdateBy(String empUpdateBy) { this.empUpdateBy = empUpdateBy; }
}
```

## DTO vs ENTITY

| Concern         | `Employee` (entity)                   | `EmployeeDTO`                          |
|-----------------|---------------------------------------|----------------------------------------|
| JPA annotations | `@Entity`, `@Table`, `@Id`, `@Column` | **None**                               |
| Validation      | None                                  | `@NotNull`, `@NotBlank`, `@DecimalMin` |
| Used in         | Repository `save`/`findById`          | Controller request/response bodies     |
| Mapped by       | Hibernate                             | `EmployeeMapper` (ModelMapper)         |

