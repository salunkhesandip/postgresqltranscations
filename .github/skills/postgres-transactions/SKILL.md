---
name: postgres-transactions
description: "PostgreSQL + Spring Boot + JPA transactions skill. Use when working with JPA entities, @Transactional service methods, PostgreSQL schema, Spring Data repositories, DTOs, mapper classes, controller/service/repository layering, JSON Patch endpoints, or unit/integration tests in this project."
argument-hint: "Describe the task precisely, e.g. 'add a new JPA entity for Department', 'write a @Transactional service method to transfer salary', 'add a PATCH endpoint for partial employee update', 'write a @WebMvcTest for the new endpoint'."
---

# PostgreSQL Transactions - Spring Boot Skill

> Use this skill to keep every change aligned with the repository architecture, transaction rules,
> naming conventions, and testing standards. Read this file first; then load the matching
> reference file before generating any code.

---

## Table of Contents

1. [When to Use](#when-to-use)
2. [When NOT to Use](#when-not-to-use)
3. [Agent Workflow](#agent-workflow)
4. [Agent Pre-Code Checklist](#agent-pre-code-checklist)
5. [Definition of Done](#definition-of-done)
6. [Output Contract](#output-contract)
7. [Quick-Decision Table](#quick-decision-table)
8. [File-Path Map](#file-path-map)
9. [Key Project Facts](#key-project-facts)
10. [Entity Field Map](#entity-field-map)
11. [REST Endpoints](#rest-endpoints)
12. [JSON Patch Request Format](#json-patch-request-format)
13. [Resilience4j Circuit Breaker](#resilience4j-circuit-breaker)
14. [Inline Critical Rules](#inline-critical-rules)
15. [Common Agent Mistakes](#common-agent-mistakes)
16. [Code Generation Constraints](#code-generation-constraints)
17. [References](#references)

---

## When to Use

Activate this skill whenever you are touching any of these layers:

| Trigger                                                                      | Layer                     |
|------------------------------------------------------------------------------|---------------------------|
| Adding or editing a JPA `@Entity` mapped to schema `company`                 | Entity                    |
| Writing or modifying `@Transactional` service methods                        | Service                   |
| Creating `@RestController` endpoints with DTOs, mappers, OpenAPI annotations | Controller / DTO / Mapper |
| Adding Spring Data repository methods (JPQL, native, `@Modifying`)           | Repository                |
| Implementing JSON Patch (`RFC 6902`) partial-update endpoints                | Controller + Service      |
| Writing tests with `@WebMvcTest` or `@ExtendWith(MockitoExtension.class)`    | Test                      |
| Configuring `JpaConfig`, `DataSource`, or `JpaTransactionManager`            | Config                    |
| Tuning Resilience4j Circuit Breaker in `application.yml`                     | Config                    |

---

## When NOT to Use

- **Build-script changes only** - edit `build.gradle` and `gradle/libs.versions.toml` directly.
- **Static asset changes** - edit `src/main/resources/static/` or `templates/` directly.
- **Docs-only updates** - update markdown only; do not generate application code.

---

## Agent Workflow

Follow these steps in order. Do not skip steps.

```
1. Read this file (SKILL.md) completely.
2. Identify the layer(s) being changed via the Quick-Decision Table below.
3. Load the matching reference file from ./references/ before writing any code.
4. Complete the Agent Pre-Code Checklist.
5. Write or update all changed files (source + test together).
6. Run: ./gradlew.bat test
7. Confirm Definition of Done.
8. Report output per the Output Contract.
```

> **Rule**: Never generate application code without first loading the matching reference file.

---

## Agent Pre-Code Checklist

Confirm every item before writing a single line of code:

- [ ] I have read the matching reference file for this layer.
- [ ] My new class lives in the correct package (see File-Path Map below).
- [ ] Every entity field has `@Column(name = "snake_case_name")` declared explicitly.
- [ ] Mutating service methods are `@Transactional`; reads are `@Transactional(readOnly = true)`.
- [ ] I throw `EmployeeNotFoundException` or `EmployeeConflictException` instead of returning `null`.
- [ ] Every new controller method has `@Operation(summary = "...")` and the class has `@Tag`.
- [ ] Every `@RequestBody` parameter in the controller has `@Valid`.
- [ ] I have written or updated the matching unit test in the same change.
- [ ] All new dependency versions are in `gradle/libs.versions.toml`, not inline in `build.gradle`.
- [ ] Fenced `java` code blocks in docs use valid Java syntax (no `...` placeholder tokens).

---

## Definition of Done

A change is complete only when ALL of the following are true:

- [ ] Source file lives in the correct layer package.
- [ ] Test file is added or updated in the same change.
- [ ] `./gradlew.bat test` passes (or failure is explicitly explained).
- [ ] No inline dependency version strings are present in `build.gradle`.
- [ ] Change is consistent with `AGENTS.md` and the loaded reference file(s).
- [ ] OpenAPI annotations (`@Tag`, `@Operation`) are present on all new or modified endpoints.

---

## Output Contract

When responding, every agent MUST include:

| Field                | What to say                                                                 |
|----------------------|-----------------------------------------------------------------------------|
| **Layers changed**   | List each layer: entity / service / repository / controller / test / config |
| **Files changed**    | Full relative paths of every file created or modified                       |
| **Rationale**        | One sentence per file explaining why it was changed                         |
| **Tests run**        | Pass/fail result of `./gradlew.bat test`, or reason skipped                 |
| **Checklist status** | Confirm all Pre-Code Checklist items are satisfied                          |

---

## Quick-Decision Table

| Task                                                | Reference File to Load                                                              |
|-----------------------------------------------------|-------------------------------------------------------------------------------------|
| Add / edit `@Entity`                                | [entities.md](./references/entities.md)                                             |
| Write `@Transactional` service method               | [transactions.md](./references/transactions.md)                                     |
| Add REST endpoint, DTO, or mapper                   | [rest-layer.md](./references/rest-layer.md)                                         |
| Add repository query (JPQL / native / `@Modifying`) | [transactions.md](./references/transactions.md)                                     |
| Write controller or service test                    | [testing.md](./references/testing.md)                                               |
| Add JSON Patch endpoint                             | [rest-layer.md](./references/rest-layer.md) + [testing.md](./references/testing.md) |

---

## File-Path Map

> Every source file has exactly one home. Never place business logic in controllers or entities.

```
src/main/java/com/cleancoders/postgresqltranscations/
|-- config/
|   `-- JpaConfig.java              - DataSource, EntityManagerFactory, JpaTransactionManager
|-- controller/
|   `-- EmployeeController.java     - @RestController, thin HTTP layer only
|-- service/
|   `-- EmployeeService.java        - ALL @Transactional methods live here
|-- repository/
|   `-- EmployeeRepository.java     - JpaRepository + @Query (JPQL + native) + @Modifying
|-- entity/
|   `-- Employee.java               - @Entity, schema="company" - NO business logic
|-- dto/
|   `-- EmployeeDTO.java            - No JPA annotations; Bean Validation only
|-- mapper/
|   `-- EmployeeMapper.java         - @Component, ModelMapper wrapper
`-- exception/
    |-- EmployeeNotFoundException.java   - 404 domain exception
    |-- EmployeeConflictException.java   - 409 domain exception
    `-- GlobalExceptionHandler.java      - @RestControllerAdvice

src/test/java/com/cleancoders/postgresqltranscations/
|-- controller/
|   `-- EmployeeControllerTest.java - @WebMvcTest
|-- service/
|   `-- EmployeeServiceTest.java    - @ExtendWith(MockitoExtension.class)
`-- constants/
    `-- EmployeeTestConstants.java  - shared magic values (IDs, names, salaries)
```

---

## Key Project Facts

| Fact                | Value                                                                |
|---------------------|----------------------------------------------------------------------|
| Root package        | `com.cleancoders.postgresqltranscations`                             |
| DB schema           | `company`                                                            |
| DB table            | `employee`                                                           |
| DB connection       | `localhost:5432/javatest`                                            |
| DB password         | `${DB_PASSWORD}` env var - never hard-code                           |
| PK strategy         | Caller-supplied (`@Id`, **no** `@GeneratedValue`)                    |
| Salary type         | `BigDecimal` / `NUMERIC(19,2)` - never `double` or `float`           |
| Date type           | `LocalDate` / `DATE`                                                 |
| Transaction manager | Manual `JpaTransactionManager` wired in `JpaConfig`                  |
| Exception types     | `EmployeeNotFoundException` (404), `EmployeeConflictException` (409) |
| Test constants      | `src/test/.../constants/EmployeeTestConstants`                       |
| API base path       | `/employees`                                                         |
| Swagger UI          | `http://localhost:8080/swagger-ui.html`                              |

---

## Entity Field Map

Full Java-to-DB column mapping for `company.employee`:

| Java field       | DB column          | Java type    | DB type         | Notes                         |
|------------------|--------------------|--------------|-----------------|-------------------------------|
| `empId`          | `emp_id`           | `Long`       | `BIGINT`        | PK - caller-supplied, no auto |
| `empName`        | `emp_name`         | `String`     | `VARCHAR(255)`  |                               |
| `empSalary`      | `emp_salary`       | `BigDecimal` | `NUMERIC(19,2)` | Never `double` or `float`     |
| `empAddress`     | `emp_address`      | `String`     | `VARCHAR(255)`  |                               |
| `empCreatedDate` | `emp_created_date` | `LocalDate`  | `DATE`          |                               |
| `empUpdatedDate` | `emp_updated_date` | `LocalDate`  | `DATE`          |                               |
| `empCreatedBy`   | `emp_created_by`   | `String`     | `VARCHAR(30)`   |                               |
| `empUpdateBy`    | `emp_update_by`    | `String`     | `VARCHAR(30)`   | Note: column name is updateBy |

> Entity classes are excluded from JaCoCo coverage reports (see `build.gradle`).

---

## REST Endpoints

| Method   | Path                                | HTTP Response  | Description                          |
|----------|-------------------------------------|----------------|--------------------------------------|
| `POST`   | `/employees`                        | 201 Created    | Create employee                      |
| `GET`    | `/employees/{id}`                   | 200 OK         | Get by ID                            |
| `PUT`    | `/employees`                        | 200 OK         | Full update                          |
| `PATCH`  | `/employees/{id}`                   | 200 OK         | Partial update - RFC 6902 JSON Patch |
| `DELETE` | `/employees/{id}`                   | 204 No Content | Delete by ID                         |
| `GET`    | `/employees/salary/{salary}`        | 200 OK         | JPQL salary filter                   |
| `GET`    | `/employees/salary/native/{salary}` | 200 OK         | Native SQL salary filter             |

---

## JSON Patch Request Format

The `PATCH /employees/{id}` endpoint consumes `application/json-patch+json` (RFC 6902).

The request body must be a **JSON array** of operation objects:

```json
[
  { "op": "replace", "path": "/empName",   "value": "Alice Smith" },
  { "op": "replace", "path": "/empSalary", "value": 80000 }
]
```

Valid `op` values: `add`, `remove`, `replace`, `move`, `copy`, `test`.

Common mistakes that produce `Unexpected token` or `400 Bad Request`:
- Sending an object `{}` instead of an array `[{}]`
- Using single quotes instead of double quotes
- Trailing comma after the last element
- Missing `Content-Type: application/json-patch+json` header

---

## Resilience4j Circuit Breaker

Two named instances configured in `application.yml`:

| Instance        | Sliding window   | Failure threshold | Wait duration |
|-----------------|------------------|-------------------|---------------|
| `backendA`      | COUNT (50 calls) | 40%               | 15 s          |
| `databaseCalls` | TIME (60 s)      | 60%               | 30 s          |

**States**: `CLOSED` -> `OPEN` (threshold breached) -> `HALF_OPEN` (after wait) -> `CLOSED` (calls succeed)

Apply to service methods like this:

```java
@CircuitBreaker(name = "databaseCalls")
@Transactional(readOnly = true)
public List<EmployeeDTO> findBySalaryGreaterThan(Long salary) {
    return employeeRepository.findBySalaryGreaterThan(salary)
            .stream()
            .map(employeeMapper::toDto)
            .toList();
}
```

> Place `@CircuitBreaker` on the **service** method, never on the controller.

---

## Inline Critical Rules

These are the most important rules from all four reference files, inlined for quick access.

### Entity Rules

- Always declare `@Table(name = "employee", schema = "company")`.
- Use `@Id` without `@GeneratedValue` - caller supplies PK.
- Every field must have `@Column(name = "snake_case_column_name")`.
- No business logic in entity classes.
- No JPA annotations on DTOs.

### Service / Transaction Rules

- Every mutating method (`save`, `update`, `patch`, `delete`, bulk ops) must be `@Transactional`.
- Every read-only query must be `@Transactional(readOnly = true)`.
- Never return `null` - throw `EmployeeNotFoundException` or `EmployeeConflictException`.
- Never write raw SQL/JDBC in a service class - use the repository.
- `@Modifying` is required on JPQL/native `INSERT`, `UPDATE`, or `DELETE` queries; must run inside `@Transactional`.

### Controller / REST Rules

- Controllers are thin: HTTP mapping, `@Valid`, and delegation only. No business logic.
- Every `@RequestBody` must be annotated with `@Valid`.
- JSON Patch endpoints must use `consumes = "application/json-patch+json"`.
- Every endpoint needs `@Operation(summary = "...")`.
- Exception mapping belongs exclusively in `GlobalExceptionHandler`.

### Testing Rules

- Use `@WebMvcTest` for controller tests - do not load the full Spring context.
- Use `@MockitoBean` (Spring Boot 3.4+) to mock `EmployeeService` in web-layer tests; never `@MockBean`.
- Use `@ExtendWith(MockitoExtension.class)` for pure unit tests - no Spring context.
- Use AssertJ `assertThat(...)` for value assertions; `assertThrows(...)` for exceptions.
- Test both the happy path and every exception path (404, 409, 400).
- Use `jsonPath` assertions for every important field in controller tests.
- Reuse constants from `EmployeeTestConstants` - never duplicate magic values.

---

## Common Agent Mistakes

Avoid these known failure patterns:

| Mistake                                        | Correct Approach                                                 |
|------------------------------------------------|------------------------------------------------------------------|
| Omitting `schema = "company"` in `@Table`      | Always set `@Table(name = "employee", schema = "company")`       |
| Using `double` or `float` for salary           | Always use `BigDecimal`                                          |
| Putting `@Transactional` on controller methods | `@Transactional` belongs on **service** methods only             |
| Returning `null` from service methods          | Throw `EmployeeNotFoundException` or `EmployeeConflictException` |
| Using `@MockBean` in tests                     | Use `@MockitoBean` (Spring Boot 3.4+)                            |
| Missing `@Valid` on `@RequestBody` params      | Every `@RequestBody` in the controller must have `@Valid`        |
| Using JUnit 4 `assertEquals` / `assertTrue`    | Use AssertJ `assertThat(...)` instead                            |
| Testing only happy path                        | Always add tests for 404, 409, and 400 error paths               |
| Duplicating magic values in test classes       | Reuse constants from `EmployeeTestConstants`                     |
| Inline version strings in `build.gradle`       | All versions in `gradle/libs.versions.toml` only                 |
| Missing `@Column(name = "...")`                | Every entity field needs explicit column mapping                 |
| Business logic in entity or controller         | Service layer only                                               |
| Missing `@Operation` on controller method      | Every endpoint needs `@Operation(summary = "...")`               |
| Skipping the test for new code                 | Every new method needs a matching test                           |
| Using `...` in fenced `java` doc examples      | Use valid, compilable Java syntax                                |
| Sending `{}` (object) for JSON Patch body      | JSON Patch body must be an array: `[{ "op": ... }]`              |
| Missing `Content-Type` on PATCH requests       | Use `application/json-patch+json` for PATCH endpoint calls       |

---

## Code Generation Constraints

These rules are non-negotiable. Violating any one fails the Definition of Done.

1. **Never** change the root package `com.cleancoders.postgresqltranscations`.
2. **Never** add `@GeneratedValue` to `empId` - PKs are caller-supplied.
3. **Never** write raw JDBC or SQL strings inside a service class.
4. **Never** add a `spring-boot-starter-*` dependency without checking `gradle/dependencies.gradle` first.
5. **Always** add a new endpoint to **both** `EmployeeController` and `EmployeeControllerTest`.
6. **Always** add a new service method to **both** `EmployeeService` and `EmployeeServiceTest`.
7. **Always** use valid Java syntax in fenced `java` code blocks - no placeholder tokens like `...`.
8. **Always** load the matching reference file before generating code for any layer.

---

## References

Load the reference file for your layer **before** generating code:

| Reference File                                  | Contents                                                         |
|-------------------------------------------------|------------------------------------------------------------------|
| [entities.md](./references/entities.md)         | Full `@Entity` template, column map, entity rules                |
| [transactions.md](./references/transactions.md) | Full `@Service` template, JPQL/native query patterns, tx rules   |
| [rest-layer.md](./references/rest-layer.md)     | Full controller, DTO, mapper, exception handler templates        |
| [testing.md](./references/testing.md)           | Full `@WebMvcTest` and `@ExtendWith` test templates, test rules  |

Project-wide references:

- [AGENTS.md](../../../../AGENTS.md) - full session rules, stack versions, conventions
- [README.md](../../../../README.md) - feature walkthrough
- [build.gradle](../../../../build.gradle) - plugin and task config
- [gradle/libs.versions.toml](../../../../gradle/libs.versions.toml) - all dependency versions
- [gradle/dependencies.gradle](../../../../gradle/dependencies.gradle) - dependency declarations
- [src/main/resources/application.yml](../../../../src/main/resources/application.yml) - DB + Resilience4j config
