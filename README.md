# PostgreSQL Transactions - Spring Boot

A Spring Boot 4 sample application for learning PostgreSQL-backed transaction management with Spring Data JPA, JSON Patch, Resilience4j circuit breakers and retries, and controller/service-level test coverage.

## What This Repository Demonstrates

| Topic | Implementation |
|-------|----------------|
| Read/write transaction boundaries | `EmployeeService` methods annotated with `@Transactional` and `@Transactional(readOnly = true)` |
| Explicit JPA infrastructure | `JpaConfig` wires `DataSource`, `EntityManagerFactory`, and `JpaTransactionManager` manually |
| DTO to entity mapping | `AppConfig` provides a shared strict `ModelMapper` bean used by `EmployeeMapper` |
| JPQL and native SQL access | `EmployeeRepository.findBySalaryGreaterThan` and `findBySalaryGreaterThanNative` |
| Bulk delete with `@Modifying` | `EmployeeRepository.deleteUsersBySalaryGreater` and `DELETE /employees/salary/{salary}` |
| RFC 6902 JSON Patch | `PATCH /employees/{id}` |
| Circuit breaker plus retry behavior | `EmployeeService` + `application.yml` (`backendA`, `databaseCalls`) |
| 503 fallback handling | `ServiceUnavailableException` mapped by `GlobalExceptionHandler` |
| Circuit breaker state transition logging | `CircuitBreakerEventLogger` |
| API and operational visibility | Swagger UI plus Spring Boot Actuator endpoints |

## Tech Stack

| Component | Version / Detail |
|-----------|------------------|
| Language | Java 25 |
| Framework | Spring Boot 4.0.5 |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| JDBC driver | PostgreSQL 42.6.0 |
| JSON Patch | `java-json-tools:json-patch` 1.13 |
| Object mapping | ModelMapper 3.2.6 |
| Resilience | Resilience4j 2.4.0 (`resilience4j-spring-boot4`) |
| API docs | Springdoc OpenAPI 3.0.0 |
| Build | Gradle 9 with Version Catalog |
| Coverage | JaCoCo |

## Project Structure

```text
postgresqltranscations/
|-- build.gradle
|-- settings.gradle
|-- gradle/
|   |-- libs.versions.toml
|   `-- dependencies.gradle
`-- src/
    |-- main/
    |   |-- java/com/cleancoders/postgresqltranscations/
    |   |   |-- PostgresqltranscationsApplication.java
    |   |   |-- config/
    |   |   |   |-- AppConfig.java
    |   |   |   |-- CircuitBreakerEventLogger.java
    |   |   |   `-- JpaConfig.java
    |   |   |-- controller/
    |   |   |   `-- EmployeeController.java
    |   |   |-- dto/
    |   |   |   `-- EmployeeDTO.java
    |   |   |-- entity/
    |   |   |   `-- Employee.java
    |   |   |-- exception/
    |   |   |   |-- EmployeeConflictException.java
    |   |   |   |-- EmployeeNotFoundException.java
    |   |   |   |-- ErrorResponse.java
    |   |   |   |-- GlobalExceptionHandler.java
    |   |   |   `-- ServiceUnavailableException.java
    |   |   |-- mapper/
    |   |   |   `-- EmployeeMapper.java
    |   |   |-- repository/
    |   |   |   `-- EmployeeRepository.java
    |   |   `-- service/
    |   |       `-- EmployeeService.java
    |   `-- resources/
    |       `-- application.yml
    `-- test/
        `-- java/com/cleancoders/postgresqltranscations/
            |-- constants/
            |   `-- EmployeeConstTest.java
            |-- controller/
            |   `-- EmployeeControllerTest.java
            `-- service/
                `-- EmployeeServiceTest.java
```

## Prerequisites

- Java 25
- PostgreSQL 16+ running on `localhost:5432`
- Database `javatest`
- Schema `company`
- Environment variable `DB_PASSWORD` set before startup

## Database Setup

Create the database and schema once:

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

The application also sets `spring.jpa.hibernate.ddl-auto=update`, so Hibernate can evolve the table definition on startup after the initial schema exists.

## Configuration

Set the database password with an environment variable instead of hard-coding credentials:

```powershell
$env:DB_PASSWORD = "your_password"
```

```bash
export DB_PASSWORD=your_password
```

Important runtime settings from `application.yml`:

- `spring.datasource.url=jdbc:postgresql://localhost:5432/javatest`
- `spring.jpa.hibernate.ddl-auto=update`
- Hibernate SQL and bind logging are enabled for local visibility
- Actuator exposes `health`, `info`, `metrics`, and `circuitbreakers`
- `backendA` protects write operations with a count-based circuit breaker
- `databaseCalls` protects reads with a time-based circuit breaker plus retry

## Running the Application

Windows:

```powershell
$env:DB_PASSWORD = "your_password"
./gradlew.bat bootRun
```

macOS / Linux:

```bash
export DB_PASSWORD=your_password
./gradlew bootRun
```

Useful local URLs:

- Application: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Actuator health: `http://localhost:8080/actuator/health`
- Actuator circuit breakers: `http://localhost:8080/actuator/circuitbreakers`

## REST API

All request bodies use `EmployeeDTO`, which requires `empId`, `empName`, and `empSalary`.

| Method | Endpoint | Description | Success | Common error responses |
|--------|----------|-------------|---------|------------------------|
| `POST` | `/employees` | Create an employee | `201 Created` | `400`, `409`, `503` |
| `GET` | `/employees/{id}` | Fetch one employee by id | `200 OK` | `404`, `503` |
| `PUT` | `/employees` | Full update of an existing employee | `200 OK` | `400`, `404`, `503` |
| `PATCH` | `/employees/{id}` | Partial update using JSON Patch | `200 OK` | `404`, `422`, `503` |
| `DELETE` | `/employees/{id}` | Delete one employee by id | `204 No Content` | `404`, `503` |
| `GET` | `/employees/salary/{salary}` | List employees with salary above a threshold using JPQL | `200 OK` | `404`, `503` |
| `GET` | `/employees/salary/native/{salary}` | List employees with salary above a threshold using native SQL | `200 OK` | `404`, `503` |
| `DELETE` | `/employees/salary/{salary}` | Bulk delete employees above a salary threshold | `204 No Content` | `503` |

### Create Employee Example

```http
POST /employees
Content-Type: application/json

{
  "empId": 1,
  "empName": "Alice",
  "empSalary": 75000.00
}
```

### JSON Patch Example

```http
PATCH /employees/1
Content-Type: application/json-patch+json

[
  { "op": "replace", "path": "/empName", "value": "Alice Smith" },
  { "op": "replace", "path": "/empSalary", "value": 80000.00 }
]
```

Supported patch operations come from RFC 6902: `add`, `remove`, `replace`, `move`, `copy`, and `test`.

## Transaction and Resilience Model

| Area | Current behavior |
|------|------------------|
| Write operations | `saveEmployee`, `updateEmployee`, `patchEmployee`, `deleteEmployee`, and bulk delete run inside `@Transactional` and are protected by circuit breaker `backendA` |
| Read operations | `findEmployee`, `findEmployeesBySalary`, and `findEmployeesBySalaryNative` use `@Transactional(readOnly = true)`, `@Retry(name = "databaseCalls")`, and circuit breaker `databaseCalls` |
| Fallback strategy | Fallback methods throw `ServiceUnavailableException`, which `GlobalExceptionHandler` turns into HTTP `503 Service Unavailable` |
| Logging | `CircuitBreakerEventLogger` logs state transitions, rejected calls, failures, and successes |
| Manual JPA setup | `JpaConfig` uses `DriverManagerDataSource`, `LocalContainerEntityManagerFactoryBean`, and `JpaTransactionManager` instead of relying solely on auto-configuration |

Configured Resilience4j instances:

| Instance | Window type | Window size | Failure threshold | Open-state wait | Notes |
|----------|-------------|-------------|-------------------|-----------------|-------|
| `backendA` | `COUNT_BASED` | 50 calls | 40% | 15s | Applied to write paths |
| `databaseCalls` | `TIME_BASED` | 60s | 60% | 30s | Applied to reads and combined with retry |

Retry configuration for `databaseCalls` uses up to 3 attempts with a `500ms` wait for transient data access failures.

## Testing

Windows:

```powershell
./gradlew.bat test
./gradlew.bat jacocoTestReport
```

macOS / Linux:

```bash
./gradlew test
./gradlew jacocoTestReport
```

What the current tests cover:

- Controller status-code behavior for `200`, `201`, `204`, `404`, `409`, `422`, and `503`
- Service-layer success paths and fallback methods
- Patch flow, salary queries, and delete operations

Coverage report output:

- `build/reports/jacoco/test/html/index.html`
