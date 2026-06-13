---
description: 'Code review instructions for the PostgreSQL Transactions project'
applyTo: '**'
---

# Code Review Instructions — PostgreSQL Transactions

## Project Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 25 |
| Framework | Spring Boot 4.1.0 |
| Database | PostgreSQL via Spring Data JPA / Hibernate |
| JSON Patch | `com.github.java-json-tools:json-patch` |
| DTO Mapping | ModelMapper (STRICT strategy, configured in `AppConfig`) |
| JSON | Jackson `ObjectMapper` (Spring-managed bean in `AppConfig`) |
| Resilience | Resilience4j circuit breakers (`@CircuitBreaker`, `@Retry`) |
| API Docs | SpringDoc OpenAPI |
| Build | Gradle + `gradle/libs.versions.toml` version catalog |
| Tests | JUnit 5 + Mockito, `@WebMvcTest` for controller slices, JaCoCo |

## Review Priorities

### 🔴 CRITICAL — Block merge
- Security vulnerabilities, exposed credentials, data corruption
- Logic errors that produce wrong results
- Breaking API contract changes without versioning

### 🟡 IMPORTANT — Requires discussion
- SOLID principle violations, excessive duplication
- Missing tests for critical paths
- Performance issues (N+1 queries, missing indexes)
- Deviations from established layering patterns

### 🟢 SUGGESTION — Non-blocking
- Readability improvements, naming clarity
- Minor best-practice deviations
- Missing or incomplete OpenAPI/Javadoc

---

## Code Quality

### Clean Code
- Single Responsibility: each class/method does one thing
- Methods should stay small and focused (< 20–30 lines)
- No magic strings or numbers — extract to named constants
- No commented-out code; use TODO with a ticket reference if needed
- Use `Optional` idiomatically with `orElseThrow()`, not `isPresent()` + `get()`

#### ❌ BAD — verbose Optional check (current pattern in `EmployeeService`)
```java
Optional<Employee> existingEmployee = employeeRepository.findById(id);
if (existingEmployee.isPresent()) {
    return mapper.convertToEmployeeDTO(existingEmployee.get());
} else {
    throw new EmployeeNotFoundException("Employee " + id + " not found");
}
```

#### ✅ GOOD — idiomatic Optional
```java
return employeeRepository.findById(id)
        .map(mapper::convertToEmployeeDTO)
        .orElseThrow(() -> new EmployeeNotFoundException("Employee " + id + " not found"));
```

---

## Dependency Injection

Prefer **constructor injection** with `final` fields. Never instantiate Spring-managed beans manually inside a class.

#### ❌ BAD — manual instantiation (current issue in `EmployeeService`)
```java
@Service
public class EmployeeService {
    private final ObjectMapper objectMapper = new ObjectMapper(); // bypasses Spring bean
}
```

#### ✅ GOOD — inject from `AppConfig` bean
```java
@Service
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper mapper;
    private final ObjectMapper objectMapper;

    public EmployeeService(EmployeeRepository employeeRepository,
                           EmployeeMapper mapper,
                           ObjectMapper objectMapper) {
        this.employeeRepository = employeeRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }
}
```

Also applies to `EmployeeMapper` — `ModelMapper` must be injected, not newed up.

---

## Layer Separation — DTOs vs Entities

DTOs are the API contract. JPA annotations (`@Entity`, `@Id`, `@Column`) belong **only** on entities.

#### ❌ BAD — JPA annotation in DTO (current issue in `EmployeeDTO`)
```java
public class EmployeeDTO implements Serializable {
    @Id               // JPA annotation has no meaning here
    private Long empId;
}
```

#### ✅ GOOD — use validation annotations only
```java
public class EmployeeDTO implements Serializable {
    @NotNull
    @Positive(message = "Employee ID must be a positive number")
    private Long empId;

    @NotEmpty
    private String empName;

    @NotNull
    private Long empSalary;
}
```

---

## REST API Standards

| Method | Expected Status | Notes |
|--------|----------------|-------|
| POST | 201 Created | Use `ResponseEntity.status(HttpStatus.CREATED).body(...)` |
| GET | 200 OK / 404 | 404 thrown via exception → `GlobalExceptionHandler` |
| PUT | 200 OK | |
| PATCH | 200 OK | |
| DELETE | 204 No Content | `ResponseEntity.noContent().build()` |

#### ❌ BAD — POST returns 200 (current issue in `EmployeeController`)
```java
@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public ResponseEntity<EmployeeDTO> createEmployee(@Valid @RequestBody EmployeeDTO employee) {
    EmployeeDTO createdEmployeeDTO = employeeService.saveEmployee(employee);
    return ResponseEntity.ok(createdEmployeeDTO); // should be 201
}
```

#### ✅ GOOD — correct status code
```java
@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public ResponseEntity<EmployeeDTO> createEmployee(@Valid @RequestBody EmployeeDTO employee) {
    return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.saveEmployee(employee));
}
```

---

## Error Handling

Never swallow exceptions or return a generic 500 without surfacing the cause. Delegate handling to `GlobalExceptionHandler`.

#### ❌ BAD — exception silenced in `EmployeeController.patchEmployee`
```java
try {
    patchedEmployee = employeeService.patchEmployee(id, jsonPatchRequest);
} catch (JsonPatchException | JsonProcessingException e) {
    return ResponseEntity.internalServerError().build(); // no info, hard to debug
}
```

#### ✅ GOOD — translate to meaningful HTTP response via GlobalExceptionHandler
```java
// In EmployeeController — translate to a domain exception
public ResponseEntity<EmployeeDTO> patchEmployee(@PathVariable Long id,
                                                 @RequestBody JsonPatch patch) {
    try {
        return ResponseEntity.ok(employeeService.patchEmployee(id, patch));
    } catch (JsonPatchException | JsonProcessingException e) {
        throw new IllegalArgumentException("Invalid JSON Patch request: " + e.getMessage(), e);
    }
}

// In GlobalExceptionHandler — handle centrally
@ExceptionHandler(IllegalArgumentException.class)
protected ResponseEntity<Object> handleBadRequest(IllegalArgumentException ex) {
    return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
}
```

---

## Transaction Management

- `@Transactional` belongs on **service** methods only — never on controllers
- Read-only queries must use `@Transactional(readOnly = true)`
- Preserve audit fields (`empCreatedDate`) — never overwrite them on update

#### ❌ BAD — creation date overwritten on update (current bug in `EmployeeService.updateEmployee`)
```java
@Transactional
public EmployeeDTO updateEmployee(EmployeeDTO employeeDTO) {
    if (employeeRepository.existsById(employeeDTO.getEmpId())) {
        Employee employee = new Employee(employeeDTO.getEmpId(), employeeDTO.getEmpName(), employeeDTO.getEmpSalary());
        employee.setEmpCreatedDate(LocalDate.now()); // ❌ destroys original creation date
        employee.setEmpUpdatedDate(LocalDate.now());
        return mapper.convertToEmployeeDTO(employeeRepository.save(employee));
    }
    // ...
}
```

#### ✅ GOOD — load existing entity, preserve creation date
```java
@Transactional
public EmployeeDTO updateEmployee(EmployeeDTO employeeDTO) {
    Employee existing = employeeRepository.findById(employeeDTO.getEmpId())
            .orElseThrow(() -> new EmployeeNotFoundException(
                    "Employee " + employeeDTO.getEmpId() + " not found"));

    existing.setEmpName(employeeDTO.getEmpName());
    existing.setEmpSalary(employeeDTO.getEmpSalary());
    existing.setEmpUpdatedDate(LocalDate.now()); // empCreatedDate untouched
    return mapper.convertToEmployeeDTO(employeeRepository.save(existing));
}
```

---

## Repository

Do not re-declare methods already provided by `JpaRepository<Employee, Long>`.

#### ❌ BAD — redundant method declaration (current issue in `EmployeeRepository`)
```java
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findById(Long empId); // already provided by JpaRepository
}
```

#### ✅ GOOD — only add custom queries
```java
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    // JpaRepository already provides: findById, existsById, save, deleteById, findAll, etc.
    // Add only custom derived queries or @Query methods here
}
```

---

## Spring Component Annotations

Use `@Component` for infrastructure helpers (mappers, converters). `@Service` implies business logic.

#### ❌ BAD — `@Service` on a mapper (current issue in `EmployeeMapper`)
```java
@Service  // misleading — EmployeeMapper has no business logic
public class EmployeeMapper { ... }
```

#### ✅ GOOD
```java
@Component
public class EmployeeMapper { ... }
```

---

## Testing Standards

### Service Tests
Use `@ExtendWith(MockitoExtension.class)` — no Spring context needed.

#### ❌ BAD — missing assertions (current pattern in `EmployeeServiceTest`)
```java
@Test
void Given_EmployeeDTO_SaveEmployee_SavedEmployee() {
    given(employeeRepository.findById(anyLong())).willReturn(Optional.empty());
    employeeService.saveEmployee(employeeDTO); // no assertion!
}
```

#### ✅ GOOD — assert result and verify repository interaction
```java
@Test
void Given_NewEmployee_When_SaveEmployee_Then_ReturnsPersistedDTO() {
    given(employeeRepository.findById(TEST_EMPLOYEE_ID)).willReturn(Optional.empty());
    given(mapper.convertToEmployee(any(EmployeeDTO.class)))
            .willReturn(new Employee(TEST_EMPLOYEE_ID, TEST_EMPLOYEE_NAME, TEST_EMPLOYEE_SALARY));
    given(employeeRepository.save(any(Employee.class)))
            .willReturn(new Employee(TEST_EMPLOYEE_ID, TEST_EMPLOYEE_NAME, TEST_EMPLOYEE_SALARY));
    given(mapper.convertToEmployeeDTO(any(Employee.class))).willReturn(employeeDTO);

    EmployeeDTO result = employeeService.saveEmployee(employeeDTO);

    assertNotNull(result);
    assertEquals(TEST_EMPLOYEE_ID, result.getEmpId());
    verify(employeeRepository).save(any(Employee.class));
}
```

### Controller Tests
Use `@WebMvcTest` — loads only the web layer. Mock the service with `@MockitoBean`.

#### ❌ BAD — wrong status assertion (current issue in `EmployeeControllerTest`)
```java
@Test
void Given_NotExistingEmployee_When_CreateEmployee_Then_SuccessResponse() throws Exception {
    mockMvc.perform(post("/employees").contentType(APPLICATION_JSON).content(requestJson))
            .andExpect(status().isOk()); // wrong — POST should assert isCreated()
}
```

#### ✅ GOOD — assert 201 and response body fields
```java
@Test
void Given_NewEmployee_When_CreateEmployee_Then_201Created() throws Exception {
    EmployeeDTO dto = createEmployeeDTO();
    given(employeeService.saveEmployee(any(EmployeeDTO.class))).willReturn(dto);

    mockMvc.perform(post("/employees")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.empId").value(TEST_EMPLOYEE_ID))
            .andExpect(jsonPath("$.empName").value(TEST_EMPLOYEE_NAME));
}
```

### Test Naming Convention
Follow **Given_Precondition_When_StateUnderTest_Then_ExpectedBehavior** consistently across all tests.

---

## Security

- Database credentials must come from environment variables / `application.yml` — never hardcoded in `JpaConfig` or any class
- All user inputs validated with Bean Validation (`@Valid`, `@NotNull`, `@NotEmpty`, `@Positive`)
- Spring Data JPA uses parameterized queries by default — verify any `@Query` with native SQL does not concatenate user input
- Dependency versions managed in `gradle/libs.versions.toml` — flag outdated or known-vulnerable versions

---

## OpenAPI Documentation

Every endpoint must have `@Operation` and `@ApiResponses` with status codes that match the actual implementation.

#### ❌ BAD — docs say 200 but implementation returns 201 (current issue in `EmployeeController`)
```java
@ApiResponse(responseCode = "200", description = "Added Employee")
@PostMapping(...)
public ResponseEntity<EmployeeDTO> createEmployee(...) { ... }
```

#### ✅ GOOD
```java
@Operation(summary = "Create a new Employee")
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "Employee created successfully"),
    @ApiResponse(responseCode = "409", description = "Employee already exists")
})
@PostMapping(...)
public ResponseEntity<EmployeeDTO> createEmployee(...) { ... }
```

---

## Review Checklist

### Code Quality
- [ ] No manual `new ObjectMapper()` or `new ModelMapper()` — use Spring beans
- [ ] `Optional` used with `orElseThrow()`, not `isPresent()` + `get()`
- [ ] No JPA annotations in DTO classes
- [ ] Copy constructors and object creation are correct (no self-assignment bugs)
- [ ] No redundant repository method declarations
- [ ] `@Component` used for mappers, `@Service` for business logic only

### REST API
- [ ] POST → 201, DELETE → 204, GET → 200/404, PUT/PATCH → 200
- [ ] `@Valid` on all `@RequestBody` parameters
- [ ] OpenAPI `@ApiResponse` codes match actual response codes

### Transactions & Data Integrity
- [ ] `@Transactional` on service methods only
- [ ] Read-only queries use `@Transactional(readOnly = true)`
- [ ] `empCreatedDate` never overwritten in update operations
- [ ] DELETE verifies resource existence before deletion

### Error Handling
- [ ] No swallowed exceptions
- [ ] All exception types handled in `GlobalExceptionHandler`
- [ ] Meaningful error messages (include entity ID in messages)

### Testing
- [ ] Service tests use `@ExtendWith(MockitoExtension.class)` — no Spring context
- [ ] Controller tests use `@WebMvcTest`
- [ ] Every test has at least one specific assertion
- [ ] Test names follow `Given_X_When_Y_Then_Z` pattern

### Build
- [ ] New dependencies added to `gradle/libs.versions.toml`, not hardcoded in `dependencies.gradle`
- [ ] Spring Boot starter versions resolved through BOM — not pinned manually

---

## Comment Format

```
**[🔴/🟡/🟢] Priority — Category: Title**

What is wrong and where.

**Why this matters:**
Impact on correctness, security, maintainability.

**Suggested fix:**
<corrected Java code snippet>
```
