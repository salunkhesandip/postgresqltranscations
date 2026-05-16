# JPA Entities & Schema Mapping

## Table of Contents

1. [Package](#package)
2. [Full Column Map](#full-column-map--companyemployee)
3. [Entity Template](#entity-template)
4. [DTO vs Entity — Key Differences](#dto-vs-entity--key-differences)
5. [Naming Conventions](#naming-conventions)
6. [Common Mistakes](#common-mistakes)
7. [Rules](#rules)

---

## Package

```
com.cleancoders.postgresqltranscations.entity
```

---

## Full Column Map — `company.employee`

| Java field       | DB column          | Java type    | DB type         | Constraints                              |
|------------------|--------------------|--------------|-----------------|------------------------------------------|
| `empId`          | `emp_id`           | `Long`       | `BIGINT`        | PK — caller supplies, **no** auto-gen    |
| `empName`        | `emp_name`         | `String`     | `VARCHAR(255)`  |                                          |
| `empSalary`      | `emp_salary`       | `BigDecimal` | `NUMERIC(19,2)` | Never `double` or `float` for money      |
| `empAddress`     | `emp_address`      | `String`     | `VARCHAR(255)`  |                                          |
| `empCreatedDate` | `emp_created_date` | `LocalDate`  | `DATE`          | Set once at creation time                |
| `empUpdatedDate` | `emp_updated_date` | `LocalDate`  | `DATE`          | Updated on every write                   |
| `empCreatedBy`   | `emp_created_by`   | `String`     | `VARCHAR(30)`   |                                          |
| `empUpdateBy`    | `emp_update_by`    | `String`     | `VARCHAR(30)`   | Column name is `emp_update_by` (no 'd')  |

> **Schema**: `company` · **Table**: `employee` · **DB**: `javatest` on `localhost:5432`

---

## Entity Template

```java
package com.cleancoders.postgresqltranscations.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * JPA entity for the company.employee table.
 * No business logic — data carrier only.
 * Excluded from JaCoCo coverage reports (see build.gradle).
 */
@Entity
@Table(name = "employee", schema = "company")
public class Employee {

    @Id
    @Column(name = "emp_id")
    private Long empId;

    @Column(name = "emp_name")
    private String empName;

    @Column(name = "emp_salary")
    private BigDecimal empSalary;       // NUMERIC(19,2) — never double/float

    @Column(name = "emp_address")
    private String empAddress;

    @Column(name = "emp_created_date")
    private LocalDate empCreatedDate;

    @Column(name = "emp_updated_date")
    private LocalDate empUpdatedDate;

    @Column(name = "emp_created_by")
    private String empCreatedBy;

    @Column(name = "emp_update_by")     // NOTE: no trailing 'd' in column name
    private String empUpdateBy;

    // ── Constructors ────────────────────────────────────────────────────────

    public Employee() {}               // JPA requires a no-arg constructor

    // ── Getters & Setters ───────────────────────────────────────────────────

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

> **Lombok alternative**: You may replace the explicit getters/setters with `@Data` from Lombok if Lombok
> is on the classpath. The no-arg constructor is still required; add `@NoArgsConstructor` explicitly.

---

## DTO vs Entity — Key Differences

| Concern         | `Employee` (Entity)                        | `EmployeeDTO`                               |
|-----------------|--------------------------------------------|---------------------------------------------|
| JPA annotations | `@Entity`, `@Table`, `@Id`, `@Column`      | **None** — purely a data carrier            |
| Bean Validation | Not used                                   | `@NotNull`, `@NotBlank`, `@DecimalMin` etc. |
| Serialisation   | Avoid `@JsonIgnore` unless strictly needed | Jackson-serialisable by default             |
| Used in         | Repository `save` / `findById` calls       | Controller request/response bodies          |
| Mapped by       | Hibernate                                  | `EmployeeMapper` (ModelMapper)              |

---

## Naming Conventions

| Element              | Convention   | Example                              |
|----------------------|--------------|--------------------------------------|
| Java field           | `camelCase`  | `empCreatedDate`                     |
| DB column annotation | `snake_case` | `@Column(name = "emp_created_date")` |
| DB column            | `snake_case` | `emp_created_date`                   |
| Entity class         | `PascalCase` | `Employee`                           |
| Table / schema       | `snake_case` | `company.employee`                   |

---

## Common Mistakes

| Mistake                                           | Correct Approach                                          |
|---------------------------------------------------|-----------------------------------------------------------|
| Omitting `schema = "company"` from `@Table`       | Always `@Table(name = "employee", schema = "company")`    |
| Adding `@GeneratedValue` to `empId`               | **Never** — caller always provides the PK                 |
| Using `double` or `float` for `empSalary`         | Always `BigDecimal` with `NUMERIC(19,2)`                  |
| Forgetting `@Column(name = "emp_update_by")`      | Every field needs its `@Column(name = "...")` declaration |
| Putting business logic in the entity              | Service layer only — entity is a data carrier             |
| Adding JPA annotations (`@Entity`, `@Id`) to DTOs | DTOs have zero JPA annotations                            |
| Omitting no-arg constructor                       | Required by JPA specification                             |

---

## Rules

- Always specify `@Table(name = "employee", schema = "company")`.
- Map every field with `@Column(name = "snake_case_column_name")` — no implicit name guessing.
- Use `@Id` **without** `@GeneratedValue` — the caller supplies the PK value.
- Use `BigDecimal` for `empSalary`; never `double` or `float`.
- Use `LocalDate` for date fields; never `java.util.Date` or `java.sql.Date`.
- No business logic in entity classes — no service calls, validations, or computations.
- No JPA annotations on DTOs — keep entity and DTO concerns completely separate.
- Entity classes are excluded from JaCoCo coverage reports (see `build.gradle`).
- A public no-arg constructor is mandatory for JPA proxying.
