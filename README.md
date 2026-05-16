# PostgreSQL Transactions — Spring Boot

A hands-on Spring Boot application for learning **PostgreSQL database transactions**, **JPA/Hibernate**, **JPQL vs Native queries**, **JSON Patch**, and **Resilience4j Circuit Breaker** patterns.

---

## What This Project Covers

| Topic | Where |
|-------|-------|
| `@Transactional` (read/write) | `EmployeeService` |
| Explicit `JpaTransactionManager` setup | `JpaConfig` |
| JPQL query | `EmployeeRepository.findBySalaryGreaterThan` |
| Native SQL query | `EmployeeRepository.findBySalaryGreaterThanNative` |
| `@Modifying` bulk delete | `EmployeeRepository.deleteUsersBySalaryGreater` |
| JSON Patch (`RFC 6902`) partial update | `PATCH /employees/{id}` |
| Resilience4j Circuit Breaker | `application.yml` — `backendA`, `databaseCalls` |
| Global exception handling | `GlobalExceptionHandler` |
| OpenAPI / Swagger UI | `/swagger-ui.html` |

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 25 |
| Framework | Spring Boot 4.x |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| JSON Patch | `java-json-tools:json-patch` 1.13 |
| Object mapping | ModelMapper 3.x |
| Circuit Breaker | Resilience4j 2.x |
| API Docs | Springdoc OpenAPI 3 |
| Build | Gradle 9 (Version Catalog) |

---

## Project Structure

```
postgresqltranscations/
├── build.gradle
├── settings.gradle
├── gradle/
│   ├── libs.versions.toml          # Centralized versions
│   └── dependencies.gradle
└── src/
    ├── main/
    │   ├── java/com/cleancoders/postgresqltranscations/
    │   │   ├── PostgresqltranscationsApplication.java
    │   │   ├── config/
    │   │   │   └── JpaConfig.java           # Explicit DataSource, EntityManagerFactory,
    │   │   │                                #   JpaTransactionManager + @EnableTransactionManagement
    │   │   ├── controller/
    │   │   │   └── EmployeeController.java  # REST endpoints (CRUD + PATCH + salary queries)
    │   │   ├── service/
    │   │   │   └── EmployeeService.java     # @Transactional methods
    │   │   ├── repository/
    │   │   │   └── EmployeeRepository.java  # JpaRepository + JPQL + Native queries
    │   │   ├── entity/
    │   │   │   └── Employee.java            # @Entity — schema "company", table "employee"
    │   │   ├── dto/
    │   │   │   └── EmployeeDTO.java
    │   │   ├── mapper/
    │   │   │   └── EmployeeMapper.java      # ModelMapper wrapper
    │   │   └── exception/
    │   │       ├── EmployeeNotFoundException.java
    │   │       ├── EmployeeConflictException.java
    │   │       └── GlobalExceptionHandler.java
    │   └── resources/
    │       └── application.yml              # DB config + Resilience4j Circuit Breaker config
    └── test/
        └── java/com/cleancoders/postgresqltranscations/
            ├── controller/
            │   └── EmployeeControllerTest.java
            └── service/
```

---

## Prerequisites

- **Java 25** (or adjust `languageVersion` in `build.gradle`)
- **PostgreSQL** running locally on port `5432`
- Database: `javatest`, schema: `company`, table: `employee`

---

## Database Setup

```sql
CREATE DATABASE javatest;

\c javatest

CREATE SCHEMA company;

CREATE TABLE company.employee (
    emp_id            BIGINT PRIMARY KEY,
    emp_name          VARCHAR(255),
    emp_salary        NUMERIC(19, 2),
    emp_address       VARCHAR(255),
    emp_created_date  DATE,
    emp_updated_date  DATE,
    emp_created_by    VARCHAR(30),
    emp_update_by     VARCHAR(30)
);
```

Hibernate's `ddl-auto: update` will create/alter the table automatically on startup if you prefer.

---

## Configuration

Set the database password via environment variable (never hard-code credentials):

```bash
# Windows PowerShell
$env:DB_PASSWORD = "your_password"

# bash / macOS / Linux
export DB_PASSWORD=your_password
```

Key settings in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/javatest
    username: postgres
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
```

---

## Running the Application

```bash
./gradlew bootRun
```

App starts on **http://localhost:8080**.  
Swagger UI: **http://localhost:8080/swagger-ui.html**

---

## REST API

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/employees` | Create a new employee |
| `GET` | `/employees/{id}` | Get employee by ID |
| `PUT` | `/employees` | Full update of an employee |
| `PATCH` | `/employees/{id}` | Partial update via JSON Patch (`RFC 6902`) |
| `DELETE` | `/employees/{id}` | Delete employee by ID |
| `GET` | `/employees/salary/{salary}` | Find employees with salary > value (JPQL) |
| `GET` | `/employees/salary/native/{salary}` | Find employees with salary > value (Native SQL) |

### Example — Create Employee

```json
POST /employees
Content-Type: application/json

{
  "empId": 1,
  "empName": "Alice",
  "empSalary": 75000.00
}
```

### Example — JSON Patch (`RFC 6902`)

```json
PATCH /employees/1
Content-Type: application/json-patch+json

[
  { "op": "replace", "path": "/empName", "value": "Alice Smith" },
  { "op": "replace", "path": "/empSalary", "value": 80000.00 }
]
```

Supported operations: `add`, `remove`, `replace`, `move`, `copy`, `test`.

---

## Key Concepts

### `@Transactional`

```java
@Transactional              // read-write transaction — begins on entry, commits on return
public EmployeeDTO saveEmployee(EmployeeDTO dto) { ... }

@Transactional(readOnly = true)  // read-only — Hibernate skips dirty checking; DB may optimize
public EmployeeDTO findEmployee(Long id) { ... }
```

### Explicit Transaction Manager (`JpaConfig`)

Rather than relying on Spring Boot's auto-configuration, this project manually wires:
- `DataSource` → `DriverManagerDataSource`
- `LocalContainerEntityManagerFactoryBean` (Hibernate as JPA provider)
- `JpaTransactionManager` bound to the entity manager factory

This demonstrates how Spring manages transactions internally.

### JPQL vs Native Query

```java
// JPQL — works across any JPA-compliant database (entity/field names, not table/column names)
@Query("SELECT e FROM Employee e WHERE e.empSalary > :salary")
List<Employee> findBySalaryGreaterThan(@Param("salary") Long salary);

// Native SQL — database-specific, uses actual table/column names
@Query(value = "SELECT * FROM Employee WHERE emp_salary > :salary", nativeQuery = true)
List<Employee> findBySalaryGreaterThanNative(@Param("salary") Long salary);
```

### `@Modifying` Bulk Operation

```java
@Modifying
@Query("DELETE FROM Employee e WHERE e.empSalary > ?1")
void deleteUsersBySalaryGreater(Long salary);
```

`@Modifying` is required for any `INSERT`, `UPDATE`, or `DELETE` JPQL/native query. It bypasses the first-level cache — must be used inside a `@Transactional` method.

### Resilience4j Circuit Breaker

Two named instances are configured in `application.yml`:

| Instance | Sliding window | Failure threshold | Wait before retry |
|----------|---------------|-------------------|-------------------|
| `backendA` | COUNT (50 calls) | 40% | 15 s |
| `databaseCalls` | TIME (60 s) | 60% | 30 s |

States: `CLOSED` → `OPEN` (on threshold breach) → `HALF_OPEN` (after wait) → `CLOSED` (if calls succeed)

---

## Running Tests

```bash
./gradlew test
```

```bash
# With JaCoCo HTML coverage report
./gradlew test jacocoTestReport
# Open: build/reports/jacoco/test/html/index.html
```
