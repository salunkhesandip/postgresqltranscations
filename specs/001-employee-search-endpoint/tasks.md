# Tasks: Employee Search Endpoint with Pagination and Filtering

**Input**: Design documents from `/specs/001-employee-search-endpoint/`

**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓, data-model.md ✓, contracts/search-api.yml ✓, quickstart.md ✓

**Tests**: **INCLUDED** - Following TDD workflow as requested (write tests → verify red → implement → verify green → refactor)

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

All paths relative to repository root: `C:\Sandip-Data\code\repo\postgresqltranscations\`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and validation annotations setup

- [ ] T001 Create validation package structure: `src/main/java/com/cleancoders/postgresqltranscations/validation/`
- [ ] T002 Create specification package structure: `src/main/java/com/cleancoders/postgresqltranscations/specification/`
- [ ] T003 [P] Verify existing EmployeeDTO fields match spec (empId, empName, empSalary, empAddress, empCreatedDate, empUpdatedDate)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Custom Validation Annotations

- [X] T004 [P] Create @ValidSalaryRange annotation in `src/main/java/com/cleancoders/postgresqltranscations/validation/ValidSalaryRange.java`
- [X] T005 [P] Create @ValidDateRange annotation in `src/main/java/com/cleancoders/postgresqltranscations/validation/ValidDateRange.java`
- [X] T006 [P] Implement SalaryRangeValidator in `src/main/java/com/cleancoders/postgresqltranscations/validation/SalaryRangeValidator.java`
- [X] T007 [P] Implement DateRangeValidator in `src/main/java/com/cleancoders/postgresqltranscations/validation/DateRangeValidator.java`

### Base DTOs and Specification Infrastructure

- [X] T008 [P] Create PaginationMetadata DTO in `src/main/java/com/cleancoders/postgresqltranscations/dto/PaginationMetadata.java` with factory method fromPage()
- [X] T009 Create EmployeeSearchCriteria DTO with validation annotations in `src/main/java/com/cleancoders/postgresqltranscations/dto/EmployeeSearchCriteria.java` (depends on T004, T005)
- [X] T010 Create PagedEmployeeResponse DTO in `src/main/java/com/cleancoders/postgresqltranscations/dto/PagedEmployeeResponse.java` (depends on T008)
- [X] T011 Create EmployeeSpecification class (static methods, no implementation yet) in `src/main/java/com/cleancoders/postgresqltranscations/specification/EmployeeSpecification.java`
- [X] T012 Extend EmployeeRepository with JpaSpecificationExecutor<Employee> in `src/main/java/com/cleancoders/postgresqltranscations/repository/EmployeeRepository.java`

### Exception Handling Extension (if needed)

- [X] T013 [P] Review GlobalExceptionHandler - verify MethodArgumentNotValidException handler exists for HTTP 422 validation errors

**Constitution Compliance Checklist**:
- [x] Layered architecture folder structure established (controller, service, repository, dto, entity, specification, validation)
- [x] Transaction management configured per constitution (manual JPA config already exists)
- [x] Test infrastructure ready (JUnit, Mockito, test directories established)
- [x] Error handling framework in place (GlobalExceptionHandler already exists)
- [x] Resilience infrastructure configured (databaseCalls circuit breaker already exists)

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Basic Employee Search with Pagination (Priority: P1) 🎯 MVP

**Goal**: Client applications can retrieve employee records in a pageable format to display in a user interface with efficient data loading and navigation controls.

**Independent Test**: Send GET request with page number and page size parameters, verify response contains correct subset of employees with pagination metadata (totalElements, totalPages, currentPage).

### Tests for User Story 1 - Write FIRST, Ensure RED ⚠️

> **TDD WORKFLOW MANDATORY**: Write these tests FIRST, verify they FAIL (red state) before implementing production code

#### Controller Layer Tests

- [ ] T014 [P] [US1] Test: GET /api/employees/search with default pagination returns HTTP 200 in `src/test/java/com/cleancoders/postgresqltranscations/controller/EmployeeControllerTest.java`
- [ ] T015 [P] [US1] Test: Verify response contains pagination metadata (totalElements, totalPages, currentPage, pageSize, hasNext, hasPrevious) in EmployeeControllerTest
- [ ] T016 [P] [US1] Test: Request page 0 size 10 with 100 employees returns exactly 10 employees in EmployeeControllerTest
- [ ] T017 [P] [US1] Test: Request page 2 size 10 with 25 employees returns 5 employees (last page) in EmployeeControllerTest
- [ ] T018 [P] [US1] Test: Request with no employees returns empty list with total=0, totalPages=0 in EmployeeControllerTest
- [ ] T019 [P] [US1] Test: Verify invalid page size (size=1000) returns HTTP 422 with validation error in EmployeeControllerTest
- [ ] T020 [P] [US1] Test: Verify negative page number (page=-1) returns HTTP 422 in EmployeeControllerTest

#### Service Layer Tests

- [ ] T021 [P] [US1] Test: searchEmployees with default pagination returns PagedEmployeeResponse in `src/test/java/com/cleancoders/postgresqltranscations/service/EmployeeServiceTest.java`
- [ ] T022 [P] [US1] Test: Verify service method has @Transactional(readOnly=true) annotation in EmployeeServiceTest
- [ ] T023 [P] [US1] Test: searchEmployees builds correct Specification for pagination-only scenario in EmployeeServiceTest
- [ ] T024 [P] [US1] Test: Verify ModelMapper converts Employee entities to EmployeeDTOs correctly in EmployeeServiceTest
- [ ] T025 [P] [US1] Test: Circuit breaker fallback method throws ServiceUnavailableException with criteria context in EmployeeServiceTest

#### Run Tests - Verify RED State

- [ ] T026 [US1] Run `./gradlew test` and confirm ALL User Story 1 tests FAIL (red state) - DO NOT PROCEED until tests fail

### Implementation for User Story 1 - Make Tests GREEN

- [X] T027 [P] [US1] Implement EmployeeService.searchEmployees method with @Transactional(readOnly=true), @CircuitBreaker, @Retry in `src/main/java/com/cleancoders/postgresqltranscations/service/EmployeeService.java`
- [X] T028 [P] [US1] Implement EmployeeService.searchEmployeesFallback method throwing ServiceUnavailableException in EmployeeService.java
- [X] T029 [P] [US1] Implement PageRequest creation logic in searchEmployees method (page, size parameters)
- [X] T030 [US1] Implement Specification.where(null) base spec composition logic in searchEmployees method (depends on T027)
- [X] T031 [US1] Add entity-to-DTO mapping using ModelMapper (page.map) in searchEmployees method (depends on T027)
- [X] T032 [US1] Add PagedEmployeeResponse.fromPage conversion in searchEmployees method (depends on T027, T031)
- [X] T033 [US1] Implement EmployeeController.search endpoint (GET /api/employees/search) in `src/main/java/com/cleancoders/postgresqltranscations/controller/EmployeeController.java`
- [X] T034 [US1] Add @Valid EmployeeSearchCriteria parameter binding to controller method (depends on T033)
- [X] T035 [P] [US1] Add Swagger annotations (@Operation, @ApiResponses, @Parameter) to search endpoint in EmployeeController.java

#### Verify GREEN State

- [ ] T036 [US1] Run `./gradlew test` and confirm ALL User Story 1 tests PASS (green state)
- [ ] T037 [US1] Manually test with curl per quickstart.md Scenario 1 (basic pagination) and Scenario 2 (custom page size)

**Constitution Compliance Checklist**:
- [ ] Layered architecture: EmployeeController → EmployeeService → EmployeeRepository separation maintained
- [ ] Transaction boundaries: `@Transactional(readOnly = true)` on EmployeeService.searchEmployees
- [ ] DTOs exposed in controller response (PagedEmployeeResponse), not Employee entities
- [ ] Circuit breaker `databaseCalls` configured with fallback method
- [ ] Validation annotations on EmployeeSearchCriteria trigger HTTP 422 errors

**Checkpoint**: User Story 1 (MVP) is fully functional - basic pagination works, tests pass, ready for demo

---

## Phase 4: User Story 2 - Filter by Employee Name (Priority: P2)

**Goal**: Client applications can search for employees by their name (partial or full match) to quickly find specific individuals.

**Independent Test**: Send GET request with name filter parameter (e.g., "?name=John") and verify only employees with matching names are returned, combined with pagination.

### Tests for User Story 2 - Write FIRST, Ensure RED ⚠️

#### Specification Layer Tests

- [ ] T038 [P] [US2] Test: EmployeeSpecification.hasName with non-null name returns LIKE predicate in `src/test/java/com/cleancoders/postgresqltranscations/specification/EmployeeSpecificationTest.java`
- [ ] T039 [P] [US2] Test: EmployeeSpecification.hasName with null or blank name returns null predicate in EmployeeSpecificationTest
- [ ] T040 [P] [US2] Test: Verify case-insensitive matching (LOWER function) in EmployeeSpecificationTest
- [ ] T041 [P] [US2] Test: Verify partial match pattern (%name%) in EmployeeSpecificationTest

#### Controller Layer Tests

- [ ] T042 [P] [US2] Test: GET /api/employees/search?name=john returns only matching employees in EmployeeControllerTest
- [ ] T043 [P] [US2] Test: Name filter is case-insensitive ("john" matches "John Smith", "JOHN DOE") in EmployeeControllerTest
- [ ] T044 [P] [US2] Test: Name filter with pagination (name=Smith&page=0&size=10) combines correctly in EmployeeControllerTest

#### Service Layer Tests

- [ ] T045 [P] [US2] Test: searchEmployees applies EmployeeSpecification.hasName when criteria.name is provided in EmployeeServiceTest
- [ ] T046 [P] [US2] Test: searchEmployees composes name spec with AND logic in EmployeeServiceTest

#### Run Tests - Verify RED State

- [ ] T047 [US2] Run `./gradlew test` and confirm ALL User Story 2 tests FAIL (red state)

### Implementation for User Story 2 - Make Tests GREEN

- [X] T048 [US2] Implement EmployeeSpecification.hasName static method with LIKE and LOWER logic in `src/main/java/com/cleancoders/postgresqltranscations/specification/EmployeeSpecification.java`
- [X] T049 [US2] Update EmployeeService.searchEmployees to compose name specification when criteria.name is provided (depends on T048)
- [X] T050 [US2] Add name filter handling with spec.and() composition in searchEmployees method

#### Verify GREEN State

- [ ] T051 [US2] Run `./gradlew test` and confirm ALL User Story 2 tests PASS (green state)
- [ ] T052 [US2] Manually test with curl per quickstart.md Scenario 4 (filter by name)

**Checkpoint**: User Stories 1 AND 2 both functional - pagination + name filtering work independently

---

## Phase 5: User Story 3 - Filter by Salary Range (Priority: P2)

**Goal**: Client applications can filter employees by salary range (minimum and/or maximum) for reporting and organizational analysis.

**Independent Test**: Send GET request with minSalary and/or maxSalary parameters and verify only employees within the specified range are returned.

### Tests for User Story 3 - Write FIRST, Ensure RED ⚠️

#### Validation Tests

- [ ] T053 [P] [US3] Test: SalaryRangeValidator allows valid range (minSalary <= maxSalary) in `src/test/java/com/cleancoders/postgresqltranscations/validation/SalaryRangeValidatorTest.java`
- [ ] T054 [P] [US3] Test: SalaryRangeValidator rejects invalid range (minSalary > maxSalary) in SalaryRangeValidatorTest
- [ ] T055 [P] [US3] Test: SalaryRangeValidator allows only minSalary or only maxSalary in SalaryRangeValidatorTest

#### Specification Layer Tests

- [ ] T056 [P] [US3] Test: EmployeeSpecification.hasSalaryBetween with both bounds returns BETWEEN predicate in EmployeeSpecificationTest
- [ ] T057 [P] [US3] Test: EmployeeSpecification.hasSalaryBetween with only minSalary returns greaterThanOrEqualTo predicate in EmployeeSpecificationTest
- [ ] T058 [P] [US3] Test: EmployeeSpecification.hasSalaryBetween with only maxSalary returns lessThanOrEqualTo predicate in EmployeeSpecificationTest
- [ ] T059 [P] [US3] Test: EmployeeSpecification.hasSalaryBetween with null bounds returns null predicate in EmployeeSpecificationTest

#### Controller Layer Tests

- [ ] T060 [P] [US3] Test: GET /api/employees/search?minSalary=40000&maxSalary=60000 returns employees in range in EmployeeControllerTest
- [ ] T061 [P] [US3] Test: minSalary=50000 (no max) returns employees with salary >= 50000 in EmployeeControllerTest
- [ ] T062 [P] [US3] Test: maxSalary=50000 (no min) returns employees with salary <= 50000 in EmployeeControllerTest
- [ ] T063 [P] [US3] Test: Invalid salary range (minSalary > maxSalary) returns HTTP 422 with validation error in EmployeeControllerTest

#### Service Layer Tests

- [ ] T064 [P] [US3] Test: searchEmployees applies EmployeeSpecification.hasSalaryBetween when criteria has salary filters in EmployeeServiceTest

#### Run Tests - Verify RED State

- [ ] T065 [US3] Run `./gradlew test` and confirm ALL User Story 3 tests FAIL (red state)

### Implementation for User Story 3 - Make Tests GREEN

- [X] T066 [US3] Implement EmployeeSpecification.hasSalaryBetween static method with BETWEEN/GTE/ LTE logic in `src/main/java/com/cleancoders/postgresqltranscations/specification/EmployeeSpecification.java`
- [X] T067 [US3] Update EmployeeService.searchEmployees to compose salary specification when criteria has minSalary or maxSalary (depends on T066)

#### Verify GREEN State

- [ ] T068 [US3] Run `./gradlew test` and confirm ALL User Story 3 tests PASS (green state)
- [ ] T069 [US3] Manually test with curl per quickstart.md Scenarios 5, 6 (salary range filtering)

**Checkpoint**: User Stories 1, 2, AND 3 all functional - pagination + name + salary filtering work independently

---

## Phase 6: User Story 4 - Filter by Creation Date Range (Priority: P3)

**Goal**: Client applications can filter employees by their record creation date to track hiring trends and generate time-based reports.

**Independent Test**: Send GET request with createdAfter and/or createdBefore date parameters and verify only employees created within the date range are returned.

### Tests for User Story 4 - Write FIRST, Ensure RED ⚠️

#### Validation Tests

- [ ] T070 [P] [US4] Test: DateRangeValidator allows valid range (createdAfter <= createdBefore) in `src/test/java/com/cleancoders/postgresqltranscations/validation/DateRangeValidatorTest.java`
- [ ] T071 [P] [US4] Test: DateRangeValidator rejects invalid range (createdAfter > createdBefore) in DateRangeValidatorTest
- [ ] T072 [P] [US4] Test: DateRangeValidator allows only createdAfter or only createdBefore in DateRangeValidatorTest

#### Specification Layer Tests

- [ ] T073 [P] [US4] Test: EmployeeSpecification.hasCreatedDateBetween with both bounds returns BETWEEN predicate in EmployeeSpecificationTest
- [ ] T074 [P] [US4] Test: EmployeeSpecification.hasCreatedDateBetween with only createdAfter returns greaterThanOrEqualTo predicate in EmployeeSpecificationTest
- [ ] T075 [P] [US4] Test: EmployeeSpecification.hasCreatedDateBetween with only createdBefore returns lessThanOrEqualTo predicate in EmployeeSpecificationTest
- [ ] T076 [P] [US4] Test: EmployeeSpecification.hasCreatedDateBetween with null bounds returns null predicate in EmployeeSpecificationTest

#### Controller Layer Tests

- [ ] T077 [P] [US4] Test: GET /api/employees/search?createdAfter=2024-05-01&createdBefore=2024-11-01 returns employees in date range in EmployeeControllerTest
- [ ] T078 [P] [US4] Test: createdAfter only returns employees created on or after that date in EmployeeControllerTest
- [ ] T079 [P] [US4] Test: Invalid date format returns HTTP 400 (Spring MVC default) in EmployeeControllerTest
- [ ] T080 [P] [US4] Test: Invalid date range (createdAfter > createdBefore) returns HTTP 422 in EmployeeControllerTest

#### Service Layer Tests

- [ ] T081 [P] [US4] Test: searchEmployees applies EmployeeSpecification.hasCreatedDateBetween when criteria has date filters in EmployeeServiceTest

#### Run Tests - Verify RED State

- [ ] T082 [US4] Run `./gradlew test` and confirm ALL User Story 4 tests FAIL (red state)

### Implementation for User Story 4 - Make Tests GREEN

- [X] T083 [US4] Implement EmployeeSpecification.hasCreatedDateBetween static method with BETWEEN/GTE/LTE logic in `src/main/java/com/cleancoders/postgresqltranscations/specification/EmployeeSpecification.java`
- [X] T084 [US4] Update EmployeeService.searchEmployees to compose date specification when criteria has createdAfter or createdBefore (depends on T083)

#### Verify GREEN State

- [ ] T085 [US4] Run `./gradlew test` and confirm ALL User Story 4 tests PASS (green state)
- [ ] T086 [US4] Manually test with curl per quickstart.md Scenario 7 (date range filtering)

**Checkpoint**: User Stories 1-4 all functional - all individual filters work independently with pagination

---

## Phase 7: User Story 5 - Combined Filters with Pagination (Priority: P3)

**Goal**: Client applications can apply multiple filters simultaneously (name, salary range, date range) while maintaining pagination support for refined search results.

**Independent Test**: Send GET request with multiple filter parameters and pagination, verify all filters are applied correctly and results are properly paginated.

### Tests for User Story 5 - Write FIRST, Ensure RED ⚠️

#### Controller Layer Tests

- [ ] T087 [P] [US5] Test: GET /api/employees/search with name + salary + date filters applies all criteria with AND logic in EmployeeControllerTest
- [ ] T088 [P] [US5] Test: Combined filters with pagination returns correct paginated subset in EmployeeControllerTest
- [ ] T089 [P] [US5] Test: Combined filters resulting in zero matches returns empty page with total=0 in EmployeeControllerTest
- [ ] T090 [P] [US5] Test: No filters provided returns all employees with default pagination in EmployeeControllerTest

#### Service Layer Tests

- [ ] T091 [P] [US5] Test: searchEmployees composes all specifications with AND logic when multiple criteria provided in EmployeeServiceTest
- [ ] T092 [P] [US5] Test: Verify correct SQL generated for combined filters (check repository call with complex spec) in EmployeeServiceTest

#### Integration Tests (Full Stack)

- [ ] T093 [P] [US5] Integration test: End-to-end combined filter scenario with real database in `src/test/java/com/cleancoders/postgresqltranscations/integration/EmployeeSearchIntegrationTest.java`
- [ ] T094 [P] [US5] Integration test: Verify pagination metadata accuracy with combined filters in EmployeeSearchIntegrationTest

#### Run Tests - Verify RED State

- [ ] T095 [US5] Run `./gradlew test` and confirm ALL User Story 5 tests FAIL (red state)

### Implementation for User Story 5 - Make Tests GREEN

> **Note**: Most implementation already complete from previous user stories. This phase verifies composition logic.

- [X] T096 [US5] Verify EmployeeService.searchEmployees correctly composes all specifications (should already work from US1-US4)
- [X] T097 [US5] Add any missing null checks or edge case handling in searchEmployees method

#### Verify GREEN State

- [ ] T098 [US5] Run `./gradlew test` and confirm ALL User Story 5 tests PASS (green state)
- [ ] T099 [US5] Manually test with curl per quickstart.md Scenario 8 (combined filters)

**Checkpoint**: All user stories (1-5) fully functional - complete search capability with all filters working together

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories, performance optimization, documentation

### Database Optimization

- [ ] T100 [P] Create database indexes for search performance:
  ```sql
  CREATE INDEX idx_lower_emp_name ON company.employee (LOWER(emp_name));
  CREATE INDEX idx_emp_salary ON company.employee (emp_salary);
  CREATE INDEX idx_emp_created_date ON company.employee (emp_created_date);
  ```

### Documentation & API Contract

- [ ] T101 [P] Verify Swagger UI at http://localhost:8080/swagger-ui.html displays search endpoint correctly with all parameters
- [ ] T102 [P] Update README.md with employee search endpoint documentation (if not auto-generated)
- [ ] T103 [P] Verify contracts/search-api.yml matches implemented endpoint behavior

### Code Quality & Coverage

- [ ] T104 Run `./gradlew clean test jacocoTestReport` and verify coverage meets 80% threshold for controller and service layers
- [ ] T105 Review JaCoCo report at `build/reports/jacoco/test/html/index.html` - ensure entities excluded from coverage
- [ ] T106 [P] Code refactoring: Extract common test utilities if test duplication found
- [ ] T107 [P] Code refactoring: Verify all magic numbers replaced with constants (page size max, defaults)

### Resilience & Error Handling Verification

- [ ] T108 Test circuit breaker behavior: Stop PostgreSQL, trigger searchEmployees, verify HTTP 503 response
- [ ] T109 Verify circuit breaker event logging: Check logs for state transitions (CLOSED → OPEN → HALF_OPEN)
- [ ] T110 [P] Test retry mechanism: Verify retry attempts logged (simulate transient database failure)

### Quickstart Validation (End-to-End)

- [ ] T111 Run all scenarios from quickstart.md (Scenarios 1-13) and verify expected outcomes
- [ ] T112 Verify validation error scenarios return HTTP 422 with structured error messages
- [ ] T113 Verify empty result scenarios return HTTP 200 with empty content array

### Performance Testing (Optional but Recommended)

- [ ] T114 [P] Load test with Apache Bench: `ab -n 1000 -c 100 "http://localhost:8080/api/employees/search?size=10"`
- [ ] T115 [P] Verify P95 latency < 500ms for dataset with proper indexes

**Constitution Final Compliance Check**:
- [ ] All EmployeeService methods have appropriate `@Transactional(readOnly = true)` annotations
- [ ] All controller endpoints return DTOs (PagedEmployeeResponse), not Employee entities
- [ ] JaCoCo report generated with entity classes excluded from coverage
- [ ] Circuit breaker `databaseCalls` configured on EmployeeService.searchEmployees with fallback method
- [ ] Custom validation annotations (@ValidSalaryRange, @ValidDateRange) trigger HTTP 422 via GlobalExceptionHandler
- [ ] Tests follow Red-Green-Refactor discipline (verified by task execution order)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-7)**: All depend on Foundational phase completion
  - User Story 1 (P1) - MVP: Can start after Phase 2
  - User Story 2 (P2): Can start after Phase 2 (independent of US1)
  - User Story 3 (P2): Can start after Phase 2 (independent of US1, US2)
  - User Story 4 (P3): Can start after Phase 2 (independent of US1-US3)
  - User Story 5 (P3): Logical extension of US1-US4 (tests composition)
- **Polish (Phase 8)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1 - MVP)**: Independent - pagination only
- **User Story 2 (P2)**: Adds name filtering - builds on US1's service structure
- **User Story 3 (P2)**: Adds salary filtering - independent of US2
- **User Story 4 (P3)**: Adds date filtering - independent of US2, US3
- **User Story 5 (P3)**: Tests combination of US1-US4 filters

### TDD Workflow (MANDATORY for Each User Story)

1. **Write Tests First**: All test tasks for a user story
2. **Verify RED**: Run tests, confirm they FAIL
3. **Implement Code**: Minimum code to pass tests
4. **Verify GREEN**: Run tests, confirm they PASS
5. **Refactor**: Improve code while keeping tests green
6. **Manual Validation**: Run quickstart.md scenarios

### Parallel Opportunities

- **Setup (Phase 1)**: All tasks can run in parallel
- **Foundational (Phase 2)**:
  - T004-T007 (validation annotations) can run in parallel
  - T008-T011 (DTOs and specs) can run after T004-T007
- **Within Each User Story**:
  - All test-writing tasks marked [P] can run in parallel
  - Implementation tasks have dependencies on specs/services
- **Across User Stories** (after Phase 2 complete):
  - US2, US3, US4 can be developed in parallel by different team members
  - US1 should complete first (MVP), then others can start
- **Polish (Phase 8)**: Most tasks marked [P] can run in parallel

---

## Parallel Example: User Story 1

```bash
# Write all tests in parallel (Red phase):
Task T014-T025: All controller and service tests for US1

# Verify RED state:
Task T026: ./gradlew test (all fail)

# Implement in sequence (Green phase):
Task T027-T035: Service and controller implementation

# Verify GREEN state:
Task T036: ./gradlew test (all pass)
Task T037: Manual curl validation
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. ✅ Complete Phase 1: Setup (T001-T003)
2. ✅ Complete Phase 2: Foundational (T004-T013) - **CRITICAL BLOCKER**
3. ✅ Complete Phase 3: User Story 1 (T014-T037)
4. **STOP and VALIDATE**: Test US1 independently with quickstart.md Scenarios 1-3
5. Deploy/demo basic pagination if ready

### Incremental Delivery (Recommended)

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → **Deploy/Demo (MVP!)**
3. Add User Story 2 → Test independently → Deploy/Demo (pagination + name search)
4. Add User Story 3 → Test independently → Deploy/Demo (+ salary filtering)
5. Add User Story 4 → Test independently → Deploy/Demo (+ date filtering)
6. Add User Story 5 → Test composition → Deploy/Demo (complete feature)
7. Complete Phase 8: Polish → Production-ready

### Parallel Team Strategy

With multiple developers (after Phase 2 complete):

1. **Team completes Setup + Foundational together** (T001-T013)
2. Once Foundational is done:
   - **Developer A**: User Story 1 (T014-T037) - MVP priority
   - **Developer B**: User Story 2 (T038-T052) - starts after US1 service structure visible
   - **Developer C**: User Story 3 (T053-T069) - can start in parallel with US2
   - **Developer D**: User Story 4 (T070-T086) - can start in parallel with US2, US3
3. User Story 5 (T087-T099) - Final integration by any developer
4. Phase 8: Polish - distributed across team

---

## Summary

**Total Tasks**: 115 tasks across 8 phases

**Task Breakdown by Phase**:
- Phase 1 (Setup): 3 tasks
- Phase 2 (Foundational): 10 tasks (**CRITICAL BLOCKER**)
- Phase 3 (User Story 1 - MVP): 24 tasks (14 tests + 10 implementation + verification)
- Phase 4 (User Story 2): 15 tasks (9 tests + 4 implementation + verification)
- Phase 5 (User Story 3): 17 tasks (11 tests + 4 implementation + verification)
- Phase 6 (User Story 4): 17 tasks (11 tests + 4 implementation + verification)
- Phase 7 (User Story 5): 13 tasks (7 tests + 4 implementation + verification)
- Phase 8 (Polish): 16 tasks (documentation, coverage, performance, validation)

**Task Breakdown by User Story**:
- **User Story 1** (P1 - MVP): 24 tasks - Basic pagination
- **User Story 2** (P2): 15 tasks - Name filtering
- **User Story 3** (P2): 17 tasks - Salary filtering
- **User Story 4** (P3): 17 tasks - Date filtering
- **User Story 5** (P3): 13 tasks - Combined filters

**Parallel Opportunities**: 47 tasks marked [P] can run in parallel within their phase

**TDD Coverage**: 52 test tasks included (covering controller, service, specification, validation, and integration layers)

**MVP Scope** (Recommended first delivery):
- Phase 1: Setup (3 tasks)
- Phase 2: Foundational (10 tasks)
- Phase 3: User Story 1 only (24 tasks)
- **Total MVP**: 37 tasks

**Suggested MVP Scope**: Deliver User Story 1 only (basic pagination) first. This establishes the complete infrastructure (validation, specifications, DTOs, service, controller) and provides immediate value. Subsequent user stories add incremental filtering capabilities.

**Independent Test Criteria Verified**: Each user story phase includes independent test verification to ensure stories can be tested and delivered separately.

---

## Notes

- **[P] tasks**: Different files, no dependencies - can execute in parallel
- **[Story] label**: Maps task to specific user story (US1, US2, US3, US4, US5) for traceability
- **TDD MANDATORY**: Tests must be written first and verified RED before implementation
- **Constitution Compliance**: All tasks aligned with layered architecture, transaction boundaries, test-first discipline, SOLID principles, and resilience patterns
- **File Paths**: All paths are exact and ready for implementation
- **Checkpoints**: Each user story phase ends with independent validation checkpoint
- **Format Validation**: ✅ ALL tasks follow required checklist format (checkbox, ID, optional [P] marker, story label for US phases, file paths in descriptions)

