# AGENTS

## Scope

Instructions for AI coding agents working in this repository.
Read this file first on every session before touching any source file.

---

## Stack

| Component       | Version / Detail                                         |
|-----------------|----------------------------------------------------------|
| Language        | Java 25                                                  |
| Framework       | Spring Boot 4.x                                          |
| ORM             | Spring Data JPA / Hibernate                              |
| Database        | PostgreSQL 16+                                           |
| JSON Patch      | `java-json-tools:json-patch` 1.13 (RFC 6902)             |
| Object mapping  | ModelMapper 3.x                                          |
| Circuit Breaker | Resilience4j 2.x                                         |
| API Docs        | Springdoc OpenAPI 3 (`/swagger-ui.html`)                 |
| Build           | Gradle 9 — Version Catalog (`gradle/libs.versions.toml`) |

---

## Fast Start

```powershell
# Windows
$env:DB_PASSWORD = "your_password"
./gradlew.bat bootRun          # starts on http://localhost:8080
./gradlew.bat test             # run all tests
./gradlew.bat jacocoTestReport # coverage → build/reports/jacoco/test/html/index.html
```

---

## Local Prerequisites

- PostgreSQL on `localhost:5432`
- Database: `javatest` · Schema: `company` · Table: `employee`
- `DB_PASSWORD` environment variable set (never hard-coded)

### Minimal DB Setup

```sql
CREATE DATABASE javatest;
\c javatest
CREATE SCHEMA company;
CREATE TABLE company.employee (
    emp_id            BIGINT PRIMARY KEY,
    emp_name          VARCHAR(255),
    emp_salary        NUMERIC(19,2),
    emp_address       VARCHAR(255),
    emp_created_date  DATE,
    emp_updated_date  DATE,
    emp_created_by    VARCHAR(30),
    emp_update_by     VARCHAR(30)
);
```

---

## Project Layer Map

```
src/main/java/com/cleancoders/postgresqltranscations/
├── config/        JpaConfig.java          — manual DataSource / TxManager wiring
├── controller/    EmployeeController.java — thin REST layer, no business logic
├── service/       EmployeeService.java    — all @Transactional methods
├── repository/    EmployeeRepository.java — JpaRepository + JPQL + native queries
├── entity/        Employee.java           — @Entity, schema="company"
├── dto/           EmployeeDTO.java        — no JPA annotations
├── mapper/        EmployeeMapper.java     — ModelMapper wrapper (@Component)
└── exception/     GlobalExceptionHandler, EmployeeNotFoundException,
                   EmployeeConflictException
```

---

## REST Endpoints

| Method   | Path                                | Description                          |
|----------|-------------------------------------|--------------------------------------|
| `POST`   | `/employees`                        | Create employee                      |
| `GET`    | `/employees/{id}`                   | Get by ID                            |
| `PUT`    | `/employees`                        | Full update                          |
| `PATCH`  | `/employees/{id}`                   | Partial update — RFC 6902 JSON Patch |
| `DELETE` | `/employees/{id}`                   | Delete by ID                         |
| `GET`    | `/employees/salary/{salary}`        | JPQL salary filter                   |
| `GET`    | `/employees/salary/native/{salary}` | Native SQL salary filter             |

---

## Conventions

- Package root: `com.cleancoders.postgresqltranscations` — never change this.
- Preserve the layered structure above; do not put business logic in controllers or entities.
- All mutating service methods must be `@Transactional`; reads use `@Transactional(readOnly = true)`.
- Throw domain exceptions (`EmployeeNotFoundException`, `EmployeeConflictException`); never return `null`.
- Use Gradle Version Catalog (`libs.versions.toml`) for all dependency versions — no inline version strings in `build.gradle`.
- Column names are `snake_case`; Java fields are `camelCase` — always declare `@Column(name = "...")`.
- Controllers must be annotated with `@Tag` and `@Operation` for OpenAPI docs.
- Tests: `@WebMvcTest` for controllers, `@ExtendWith(MockitoExtension.class)` for services.
  Shared test constants live in `src/test/.../constants/`.

---

## Skill System

This repository ships a skill for AI agents:

| Skill                   | File                                            | Use When                                                                              |
|-------------------------|-------------------------------------------------|---------------------------------------------------------------------------------------|
| `postgres-transactions` | `.github/skills/postgres-transactions/SKILL.md` | Adding/editing entities, services, controllers, repositories, DTOs, mappers, or tests |

When the skill applies, load the relevant reference file from
`.github/skills/postgres-transactions/references/` **before** generating code.

---

## References

- [README.md](README.md) — full feature walkthrough
- [build.gradle](build.gradle) — plugin and task config
- [gradle/libs.versions.toml](gradle/libs.versions.toml) — all dependency versions
- [gradle/dependencies.gradle](gradle/dependencies.gradle) — dependency declarations
- [src/main/resources/application.yml](src/main/resources/application.yml) — DB + Resilience4j config
