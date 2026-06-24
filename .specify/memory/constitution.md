<!--
========================================
SYNC IMPACT REPORT
========================================
Version Change: INITIAL → 1.0.0
Date: 2025-01-26

Modified Principles:
- NEW: I. Strict Layered Architecture
- NEW: II. Transaction Boundary Discipline
- NEW: III. Test-First with Coverage (NON-NEGOTIABLE)
- NEW: IV. Clean Code & SOLID Principles
- NEW: V. Resilience & Error Handling

Added Sections:
- Technology Stack Requirements
- Development Workflow & Quality Gates

Templates Requiring Updates:
✅ plan-template.md - Updated Constitution Check section to reflect layered architecture gates
✅ spec-template.md - Aligned edge cases and requirements sections
✅ tasks-template.md - Aligned task categories with transaction/testing discipline

Follow-up TODOs:
- None - All placeholders filled

========================================
-->

# PostgreSQL Transactions Constitution

## Core Principles

### I. Strict Layered Architecture

**MUST** maintain clear separation between layers:
- **Controller Layer**: REST endpoints, request/response handling, HTTP status codes only
- **Service Layer**: Business logic, transaction boundaries, orchestration
- **Repository Layer**: Data access, JPA queries (JPQL and native SQL)
- **DTO/Entity Separation**: DTOs for API contracts, Entities for persistence

**MUST NOT**:
- Expose entities directly in REST responses
- Place business logic in controllers
- Access repositories directly from controllers
- Use `@Transactional` in controllers

**Rationale**: Layered architecture ensures testability, maintainability, and clear separation of concerns. Transaction boundaries belong in the service layer where business operations are defined.

### II. Transaction Boundary Discipline

**MUST** define explicit transaction boundaries:
- Write operations: `@Transactional` (default propagation)
- Read operations: `@Transactional(readOnly = true)`
- Bulk operations: `@Modifying` with `@Transactional`

**MUST** use manual JPA configuration:
- `DataSource` configured explicitly in `JpaConfig`
- `EntityManagerFactory` via `LocalContainerEntityManagerFactoryBean`
- `JpaTransactionManager` wired manually

**MUST NOT**:
- Rely solely on Spring Boot auto-configuration for JPA
- Leave transaction boundaries undefined
- Mix transactional and non-transactional data access patterns

**Rationale**: Explicit transaction management prevents data inconsistency, enables proper resource cleanup, and makes concurrency behavior predictable. Manual JPA configuration provides full control and educational transparency.

### III. Test-First with Coverage (NON-NEGOTIABLE)

**TDD Workflow MANDATORY**:
1. Write test cases covering expected behavior
2. User/team approval of test scenarios
3. Verify tests fail (Red)
4. Implement minimum code to pass (Green)
5. Refactor while maintaining green state

**Coverage Requirements**:
- JaCoCo reporting required on every build
- Controller tests: HTTP status codes, request/response validation
- Service tests: business logic paths, transactional behavior, fallback methods
- Excluded from coverage: entity classes (data carriers only)

**MUST NOT**:
- Write production code before failing tests exist
- Skip test verification before implementation
- Deploy without JaCoCo coverage report

**Rationale**: Test-first development catches defects early, documents intent, and enables confident refactoring. Coverage reporting ensures test discipline is maintained and gaps are visible.

### IV. Clean Code & SOLID Principles

**MUST** adhere to:
- **Single Responsibility**: Each class has one reason to change
- **Open/Closed**: Extend behavior via composition, not modification
- **Liskov Substitution**: Subtypes must be substitutable
- **Interface Segregation**: No fat interfaces
- **Dependency Inversion**: Depend on abstractions

**Code Quality Standards**:
- Meaningful names: `EmployeeService`, `EmployeeMapper`, not `Manager`, `Helper`
- Small methods: focused, single-level of abstraction
- DRY: Extract common patterns (e.g., `ModelMapper` bean shared across mappers)
- Fail fast: Validate early, throw custom exceptions with context

**MUST NOT**:
- Use vague class names (`Util`, `Common`, `Base` without context)
- Create god classes with multiple responsibilities
- Duplicate mapping or validation logic

**Rationale**: SOLID principles reduce coupling, increase cohesion, and make code resilient to change. Clean code accelerates onboarding and reduces cognitive load.

### V. Resilience & Error Handling

**MUST** implement resilience patterns:
- **Circuit Breakers**: Protect write operations (`backendA`) and read operations (`databaseCalls`)
- **Retry Logic**: `@Retry` for transient failures (e.g., `databaseCalls` with 3 attempts, 500ms wait)
- **Fallback Methods**: Throw `ServiceUnavailableException` → HTTP 503
- **Event Logging**: `CircuitBreakerEventLogger` for state transitions

**Error Handling Standards**:
- Custom exceptions: `EmployeeNotFoundException`, `EmployeeConflictException`
- `GlobalExceptionHandler` with consistent `ErrorResponse` structure
- HTTP status alignment: 404 (not found), 409 (conflict), 422 (validation), 503 (unavailable)

**MUST NOT**:
- Expose stack traces in API responses
- Use generic exceptions for business errors
- Ignore circuit breaker state transitions

**Rationale**: Resilience patterns prevent cascading failures and improve system stability. Structured error handling provides client clarity and operational debuggability.

## Technology Stack Requirements

**Language & Framework**:
- Java 25 with toolchain configuration
- Spring Boot 4.x (currently 4.0.5)
- Spring Data JPA with Hibernate ORM

**Database**:
- PostgreSQL 16+ required
- JDBC driver: PostgreSQL 42.6.0+
- Schema: explicit `company` schema with `employee` table

**Build & Tooling**:
- Gradle 9 with version catalog (`gradle/libs.versions.toml`)
- JaCoCo for coverage reporting
- Springdoc OpenAPI 3.0.0 for API documentation

**Libraries**:
- ModelMapper 3.2.6 for DTO/Entity mapping
- Resilience4j 2.4.0 (spring-boot4 variant)
- JSON Patch: `java-json-tools:json-patch` 1.13

**Configuration**:
- Environment variable `DB_PASSWORD` for credentials (no hardcoded secrets)
- Hibernate DDL mode: `update` (manual schema creation expected)
- SQL logging enabled for local development

**Constraints**:
- Use version catalog for all dependencies
- Explicit dependency versions (no `latest` or ranges)
- Entity classes excluded from JaCoCo coverage

## Development Workflow & Quality Gates

**Before Implementation**:
1. Write test cases (controller + service layers)
2. Verify tests fail (Red state)
3. Review test scenarios with team

**Implementation**:
1. Implement minimum code to pass tests (Green state)
2. Refactor while maintaining green
3. Run `./gradlew test jacocoTestReport`

**Quality Gates** (all MUST pass before merge):
- All tests green
- JaCoCo report generated (`build/reports/jacoco/test/html/index.html`)
- No exposed entities in controller responses
- Transaction boundaries defined on service methods
- Circuit breaker configuration for new service methods
- Custom exceptions with GlobalExceptionHandler mappings

**Code Review Checklist**:
- [ ] Layered architecture preserved (Controller → Service → Repository)
- [ ] Transaction boundaries appropriate (`@Transactional` on service methods)
- [ ] Tests written first and failed before implementation
- [ ] SOLID principles followed
- [ ] Resilience patterns applied (circuit breaker/retry where warranted)
- [ ] Error handling with custom exceptions and HTTP status alignment
- [ ] JaCoCo coverage report shows expected coverage

**Documentation Updates**:
- Update README.md if new endpoints or features added
- Update `application.yml` comments for configuration changes
- Swagger annotations on new controller endpoints

## Governance

**Authority**:
- This constitution supersedes all other coding practices and decisions
- Amendments require documented justification, team approval, and migration plan
- Constitution version follows semantic versioning:
  - **MAJOR**: Backward-incompatible principle changes (e.g., removing a layer requirement)
  - **MINOR**: New principles or material additions (e.g., new resilience pattern requirement)
  - **PATCH**: Clarifications, typo fixes, wording improvements

**Compliance**:
- All code reviews MUST verify compliance with constitution principles
- Quality gates (see Development Workflow section) MUST pass before merge
- Deviations MUST be justified in PR description and require explicit approval

**Complexity Justification**:
- Any complexity introduced (e.g., new layer, pattern, library) MUST demonstrate:
  1. Why simpler alternatives are insufficient
  2. How it aligns with constitution principles
  3. What problem it solves

**Runtime Guidance**:
- Use README.md for operational guidance (running, testing, deployment)
- Use inline code comments for non-obvious implementation choices
- Use Swagger UI for API exploration and contract understanding

**Version**: 1.0.0 | **Ratified**: 2025-01-26 | **Last Amended**: 2025-01-26
