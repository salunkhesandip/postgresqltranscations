---
name: postgres-transactions
description: "PostgreSQL + Spring Boot + JPA transactions skill. Use when working with JPA entities, @Transactional service methods, PostgreSQL schema, Spring Data repositories, DTOs, mapper classes, controller/service/repository layering, JSON Patch endpoints, or unit/integration tests in this project."
argument-hint: "Describe the task precisely, e.g. 'add a new JPA entity for Department', 'write a @Transactional service method', 'add a PATCH endpoint', 'write a @WebMvcTest for the new endpoint'."
---

# SKILL: postgres-transactions

## WHEN TO USE
- Adding/editing a JPA `@Entity` mapped to schema `company`
- Writing/modifying `@Transactional` service methods
- Creating `@RestController` endpoints, DTOs, or mappers
- Adding repository queries (JPQL, native, `@Modifying`)
- Implementing JSON Patch (`RFC 6902`) partial-update endpoints
- Writing `@WebMvcTest` or `@ExtendWith(MockitoExtension.class)` tests
- Configuring `JpaConfig`, `DataSource`, or Resilience4j

## NOT FOR
- Build-script-only changes → edit `build.gradle` / `libs.versions.toml` directly
- Static asset or docs-only changes

## STEPS
1. Identify layer(s) via the reference table below.
2. Load matching reference file from `./references/` before writing any code.
3. Write/update source + test in the same change.
4. Run `./gradlew.bat test`.
5. Deliver output per OUTPUT FORMAT.

### Reference Table
| Layer / Task                                | Load                                                 |
|---------------------------------------------|------------------------------------------------------|
| `@Entity`                                   | `references/entities.md`                             |
| `@Transactional` service / repository query | `references/transactions.md`                         |
| Controller, DTO, mapper                     | `references/rest-layer.md`                           |
| JSON Patch endpoint                         | `references/rest-layer.md` + `references/testing.md` |
| Tests                                       | `references/testing.md`                              |

## CONSTRAINTS

### Package / Structure
- Root package: `com.cleancoders.postgresqltranscations` — never change
- No business logic in controllers or entities
- New endpoint → update both `EmployeeController` AND `EmployeeControllerTest`
- New service method → update both `EmployeeService` AND `EmployeeServiceTest`

### Entity
- `@Table(name = "employee", schema = "company")` — always
- `@Id` with no `@GeneratedValue`; caller supplies PK
- Every field: explicit `@Column(name = "snake_case_name")`
- `empSalary` → `BigDecimal` / `NUMERIC(19,2)`; never `double`/`float`
- `empCreatedDate` / `empUpdatedDate` → `LocalDate`

### Service / Transaction
- Mutating methods: `@Transactional`; reads: `@Transactional(readOnly = true)`
- Throw `EmployeeNotFoundException` (404) or `EmployeeConflictException` (409); never return `null`
- No raw JDBC/SQL in service class; delegate to repository only
- `@Modifying` required on JPQL/native write queries; must be inside `@Transactional`
- `@CircuitBreaker` on service methods only — never on controller

### Controller
- Thin: HTTP mapping, `@Valid`, delegation only
- Every `@RequestBody` → `@Valid`
- JSON Patch → `consumes = "application/json-patch+json"`
- Every endpoint → `@Operation(summary = "...")`; class → `@Tag`
- Exception handling exclusively in `GlobalExceptionHandler`

### Dependencies
- All versions in `gradle/libs.versions.toml`; no inline version strings in `build.gradle`

### Testing
- Controller tests: `@WebMvcTest`; mock service with `@MockitoBean` — not `@MockBean`
- Service tests: `@ExtendWith(MockitoExtension.class)`; no Spring context
- Assertions: AssertJ `assertThat`; `jsonPath` for controller response fields
- Cover happy path + all error paths (404, 409, 400)
- Reuse `EmployeeTestConstants`; never duplicate magic values

## OUTPUT FORMAT
```json
{
  "layers_changed": ["entity | service | repository | controller | dto | mapper | config | test"],
  "files_changed": ["relative/path/to/File.java"],
  "tests_run": "pass | fail | skipped — reason",
  "constraints_satisfied": true
}
```

## EXAMPLES

**JSON Patch body** — most error-prone case:
```json
[
  { "op": "replace", "path": "/empName",   "value": "Alice Smith" },
  { "op": "replace", "path": "/empSalary", "value": 80000 }
]
```
- Body must be an array `[{…}]`, not an object `{…}`
- Required header: `Content-Type: application/json-patch+json`

---

## KEY FACTS
| Fact             | Value                                                                    |
|------------------|--------------------------------------------------------------------------|
| DB               | `localhost:5432/javatest`, schema `company`, table `employee`            |
| DB password      | `${DB_PASSWORD}` env var — never hard-code                               |
| PK               | Caller-supplied `Long empId`; no `@GeneratedValue`                       |
| API base         | `/employees`                                                             |
| Circuit breakers | `backendA` (COUNT/50, 40%, 15 s), `databaseCalls` (TIME/60 s, 60%, 30 s) |
| Swagger          | `http://localhost:8080/swagger-ui.html`                                  |

## ENTITY FIELD MAP
| Java field       | DB column          | Java type    | DB type         |
|------------------|--------------------|--------------|-----------------|
| `empId`          | `emp_id`           | `Long`       | `BIGINT` PK     |
| `empName`        | `emp_name`         | `String`     | `VARCHAR(255)`  |
| `empSalary`      | `emp_salary`       | `BigDecimal` | `NUMERIC(19,2)` |
| `empAddress`     | `emp_address`      | `String`     | `VARCHAR(255)`  |
| `empCreatedDate` | `emp_created_date` | `LocalDate`  | `DATE`          |
| `empUpdatedDate` | `emp_updated_date` | `LocalDate`  | `DATE`          |
| `empCreatedBy`   | `emp_created_by`   | `String`     | `VARCHAR(30)`   |
| `empUpdateBy`    | `emp_update_by`    | `String`     | `VARCHAR(30)`   |
