# Quickstart: Employee Search Endpoint Validation

**Feature**: Employee Search with Pagination and Filtering  
**Date**: 2025-01-26  
**Endpoint**: `GET /api/employees/search`

## Purpose

This guide provides runnable validation scenarios to verify the employee search endpoint works end-to-end. Each scenario includes prerequisites, curl commands, and expected outcomes.

**Note**: This is a validation/run guide. Full implementation details (models, services, controllers) are documented in `data-model.md`. Complete test suites belong in `tasks.md` and the implementation phase.

---

## Prerequisites

1. **Application Running**: Start the application with `./gradlew bootRun`
2. **Database Setup**: Ensure PostgreSQL is running with `company.employee` table populated
3. **Environment**: Set `DB_PASSWORD` environment variable
4. **Base URL**: Application running on `http://localhost:8080`

### Sample Test Data (Required)

Create sample employees for testing:

```sql
-- Connect to javatest database
\c javatest

-- Insert test employees
INSERT INTO company.employee (emp_id, emp_name, emp_salary, emp_address, emp_created_date, emp_updated_date)
VALUES
  (101, 'John Smith', 50000, 'Engineering Department', '2024-06-15', '2024-06-15'),
  (102, 'Jane Doe', 45000, 'HR Department', '2024-03-20', '2024-03-20'),
  (103, 'John Doe', 60000, 'Marketing Department', '2024-08-10', '2024-08-10'),
  (104, 'Alice Johnson', 75000, 'Engineering Department', '2024-01-05', '2024-01-05'),
  (105, 'Bob Williams', 35000, 'Sales Department', '2024-11-12', '2024-11-12'),
  (106, 'Charlie Brown', 80000, 'Engineering Department', '2023-12-01', '2023-12-01'),
  (107, 'Diana Prince', 55000, 'HR Department', '2024-07-22', '2024-07-22'),
  (108, 'Johnny Cash', 42000, 'Sales Department', '2024-02-14', '2024-02-14'),
  (109, 'Sarah Connor', 90000, 'Engineering Department', '2024-09-30', '2024-09-30'),
  (110, 'Michael Scott', 48000, 'HR Department', '2024-04-18', '2024-04-18');
```

---

## Validation Scenarios

### Scenario 1: Basic Pagination (No Filters)

**Description**: Retrieve all employees with default pagination.

**Test Case**: Default pagination parameters (page=0, size=20)

**Command**:
```bash
curl -X GET "http://localhost:8080/api/employees/search" \
  -H "Accept: application/json" | jq
```

**Expected Response**:
```json
{
  "content": [
    {
      "empId": 101,
      "empName": "John Smith",
      "empSalary": 50000.00,
      "empAddress": "Engineering Department",
      "empCreatedDate": "2024-06-15",
      "empUpdatedDate": "2024-06-15"
    },
    // ... (all 10 employees)
  ],
  "pagination": {
    "totalElements": 10,
    "totalPages": 1,
    "currentPage": 0,
    "pageSize": 20,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

**HTTP Status**: `200 OK`

**Verification**:
- ✓ Response contains all employees (up to 20)
- ✓ `totalElements` equals actual employee count
- ✓ `currentPage` is 0 (first page)
- ✓ `hasNext` is false (only one page)
- ✓ `hasPrevious` is false (first page)

---

### Scenario 2: Custom Page Size

**Description**: Request a specific page size (5 employees per page).

**Test Case**: page=0, size=5

**Command**:
```bash
curl -X GET "http://localhost:8080/api/employees/search?page=0&size=5" \
  -H "Accept: application/json" | jq
```

**Expected Response**:
```json
{
  "content": [
    // ... (exactly 5 employees)
  ],
  "pagination": {
    "totalElements": 10,
    "totalPages": 2,
    "currentPage": 0,
    "pageSize": 5,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

**HTTP Status**: `200 OK`

**Verification**:
- ✓ Response contains exactly 5 employees
- ✓ `totalPages` equals 2 (10 employees / 5 per page)
- ✓ `hasNext` is true (second page exists)
- ✓ `pageSize` matches requested size (5)

---

### Scenario 3: Navigate to Second Page

**Description**: Request the second page of results.

**Test Case**: page=1, size=5

**Command**:
```bash
curl -X GET "http://localhost:8080/api/employees/search?page=1&size=5" \
  -H "Accept: application/json" | jq
```

**Expected Response**:
```json
{
  "content": [
    // ... (remaining 5 employees)
  ],
  "pagination": {
    "totalElements": 10,
    "totalPages": 2,
    "currentPage": 1,
    "pageSize": 5,
    "hasNext": false,
    "hasPrevious": true
  }
}
```

**HTTP Status**: `200 OK`

**Verification**:
- ✓ `currentPage` is 1 (second page)
- ✓ `hasNext` is false (no third page)
- ✓ `hasPrevious` is true (first page exists)
- ✓ Different employees than page 0

---

### Scenario 4: Filter by Name (Case-Insensitive)

**Description**: Search for employees with "john" in their name (case-insensitive).

**Test Case**: name=john (should match "John Smith", "John Doe", "Alice Johnson", "Johnny Cash")

**Command**:
```bash
curl -X GET "http://localhost:8080/api/employees/search?name=john" \
  -H "Accept: application/json" | jq
```

**Expected Response**:
```json
{
  "content": [
    {
      "empId": 101,
      "empName": "John Smith",
      "empSalary": 50000.00,
      "empAddress": "Engineering Department",
      "empCreatedDate": "2024-06-15",
      "empUpdatedDate": "2024-06-15"
    },
    {
      "empId": 103,
      "empName": "John Doe",
      "empSalary": 60000.00,
      "empAddress": "Marketing Department",
      "empCreatedDate": "2024-08-10",
      "empUpdatedDate": "2024-08-10"
    },
    {
      "empId": 104,
      "empName": "Alice Johnson",
      "empSalary": 75000.00,
      "empAddress": "Engineering Department",
      "empCreatedDate": "2024-01-05",
      "empUpdatedDate": "2024-01-05"
    },
    {
      "empId": 108,
      "empName": "Johnny Cash",
      "empSalary": 42000.00,
      "empAddress": "Sales Department",
      "empCreatedDate": "2024-02-14",
      "empUpdatedDate": "2024-02-14"
    }
  ],
  "pagination": {
    "totalElements": 4,
    "totalPages": 1,
    "currentPage": 0,
    "pageSize": 20,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

**HTTP Status**: `200 OK`

**Verification**:
- ✓ Only employees with "john" (case-insensitive) in name are returned
- ✓ Matches partial names: "John Smith", "Johnson", "Johnny"
- ✓ Total elements reflects filtered count (4)

---

### Scenario 5: Filter by Salary Range (Both Bounds)

**Description**: Search for employees with salary between $45,000 and $60,000 (inclusive).

**Test Case**: minSalary=45000, maxSalary=60000

**Command**:
```bash
curl -X GET "http://localhost:8080/api/employees/search?minSalary=45000&maxSalary=60000" \
  -H "Accept: application/json" | jq
```

**Expected Response**:
```json
{
  "content": [
    {
      "empId": 101,
      "empName": "John Smith",
      "empSalary": 50000.00,
      // ...
    },
    {
      "empId": 102,
      "empName": "Jane Doe",
      "empSalary": 45000.00,
      // ...
    },
    {
      "empId": 103,
      "empName": "John Doe",
      "empSalary": 60000.00,
      // ...
    },
    {
      "empId": 107,
      "empName": "Diana Prince",
      "empSalary": 55000.00,
      // ...
    },
    {
      "empId": 110,
      "empName": "Michael Scott",
      "empSalary": 48000.00,
      // ...
    }
  ],
  "pagination": {
    "totalElements": 5,
    "totalPages": 1,
    "currentPage": 0,
    "pageSize": 20,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

**HTTP Status**: `200 OK`

**Verification**:
- ✓ Only employees with salary >= 45000 and <= 60000 are returned
- ✓ Boundary values 45000 and 60000 are included (inclusive range)
- ✓ Employees with salary < 45000 or > 60000 are excluded

---

### Scenario 6: Filter by Salary Range (Minimum Only)

**Description**: Search for employees with salary >= $70,000.

**Test Case**: minSalary=70000 (no maxSalary)

**Command**:
```bash
curl -X GET "http://localhost:8080/api/employees/search?minSalary=70000" \
  -H "Accept: application/json" | jq
```

**Expected Response**:
```json
{
  "content": [
    {
      "empId": 104,
      "empName": "Alice Johnson",
      "empSalary": 75000.00,
      // ...
    },
    {
      "empId": 106,
      "empName": "Charlie Brown",
      "empSalary": 80000.00,
      // ...
    },
    {
      "empId": 109,
      "empName": "Sarah Connor",
      "empSalary": 90000.00,
      // ...
    }
  ],
  "pagination": {
    "totalElements": 3,
    "totalPages": 1,
    "currentPage": 0,
    "pageSize": 20,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

**HTTP Status**: `200 OK`

**Verification**:
- ✓ Only employees with salary >= 70000 are returned
- ✓ No upper bound applied (maxSalary not specified)

---

### Scenario 7: Filter by Creation Date Range

**Description**: Search for employees created between January 2024 and June 2024.

**Test Case**: createdAfter=2024-01-01, createdBefore=2024-06-30

**Command**:
```bash
curl -X GET "http://localhost:8080/api/employees/search?createdAfter=2024-01-01&createdBefore=2024-06-30" \
  -H "Accept: application/json" | jq
```

**Expected Response**:
```json
{
  "content": [
    {
      "empId": 102,
      "empName": "Jane Doe",
      "empCreatedDate": "2024-03-20",
      // ...
    },
    {
      "empId": 104,
      "empName": "Alice Johnson",
      "empCreatedDate": "2024-01-05",
      // ...
    },
    {
      "empId": 108,
      "empName": "Johnny Cash",
      "empCreatedDate": "2024-02-14",
      // ...
    },
    {
      "empId": 110,
      "empName": "Michael Scott",
      "empCreatedDate": "2024-04-18",
      // ...
    },
    {
      "empId": 101,
      "empName": "John Smith",
      "empCreatedDate": "2024-06-15",
      // ...
    }
  ],
  "pagination": {
    "totalElements": 5,
    "totalPages": 1,
    "currentPage": 0,
    "pageSize": 20,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

**HTTP Status**: `200 OK`

**Verification**:
- ✓ Only employees created between 2024-01-01 and 2024-06-30 are returned
- ✓ Employees created before 2024-01-01 or after 2024-06-30 are excluded

---

### Scenario 8: Combined Filters (Name + Salary + Date)

**Description**: Search for employees with "john" in name, salary between $40,000 and $70,000, created in 2024.

**Test Case**: name=john, minSalary=40000, maxSalary=70000, createdAfter=2024-01-01, createdBefore=2024-12-31

**Command**:
```bash
curl -X GET "http://localhost:8080/api/employees/search?name=john&minSalary=40000&maxSalary=70000&createdAfter=2024-01-01&createdBefore=2024-12-31" \
  -H "Accept: application/json" | jq
```

**Expected Response**:
```json
{
  "content": [
    {
      "empId": 101,
      "empName": "John Smith",
      "empSalary": 50000.00,
      "empCreatedDate": "2024-06-15",
      // ...
    },
    {
      "empId": 103,
      "empName": "John Doe",
      "empSalary": 60000.00,
      "empCreatedDate": "2024-08-10",
      // ...
    },
    {
      "empId": 108,
      "empName": "Johnny Cash",
      "empSalary": 42000.00,
      "empCreatedDate": "2024-02-14",
      // ...
    }
  ],
  "pagination": {
    "totalElements": 3,
    "totalPages": 1,
    "currentPage": 0,
    "pageSize": 20,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

**HTTP Status**: `200 OK`

**Verification**:
- ✓ All filters applied with AND logic
- ✓ Alice Johnson excluded (name doesn't match "john" closely enough or salary too high)
- ✓ Only employees matching ALL criteria are returned

---

### Scenario 9: Empty Result Set

**Description**: Search with criteria that match no employees.

**Test Case**: name=Nonexistent

**Command**:
```bash
curl -X GET "http://localhost:8080/api/employees/search?name=Nonexistent" \
  -H "Accept: application/json" | jq
```

**Expected Response**:
```json
{
  "content": [],
  "pagination": {
    "totalElements": 0,
    "totalPages": 0,
    "currentPage": 0,
    "pageSize": 20,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

**HTTP Status**: `200 OK` (not 404 - empty results are valid)

**Verification**:
- ✓ Empty content array
- ✓ totalElements is 0
- ✓ totalPages is 0
- ✓ HTTP 200 (success, just no matches)

---

### Scenario 10: Validation Error - Invalid Page Size

**Description**: Request with page size exceeding maximum (100).

**Test Case**: size=1000 (exceeds maximum of 100)

**Command**:
```bash
curl -X GET "http://localhost:8080/api/employees/search?size=1000" \
  -H "Accept: application/json" | jq
```

**Expected Response**:
```json
{
  "message": "Validation failed",
  "errors": [
    "size: Page size cannot exceed 100"
  ],
  "timestamp": "2025-01-26T10:15:30Z"
}
```

**HTTP Status**: `422 Unprocessable Entity`

**Verification**:
- ✓ HTTP 422 status code
- ✓ Error message indicates validation failure
- ✓ Specific field error mentions "size" parameter

---

### Scenario 11: Validation Error - Invalid Salary Range

**Description**: Request with minSalary > maxSalary.

**Test Case**: minSalary=80000, maxSalary=50000 (invalid range)

**Command**:
```bash
curl -X GET "http://localhost:8080/api/employees/search?minSalary=80000&maxSalary=50000" \
  -H "Accept: application/json" | jq
```

**Expected Response**:
```json
{
  "message": "Validation failed",
  "errors": [
    "Minimum salary must be less than or equal to maximum salary"
  ],
  "timestamp": "2025-01-26T10:15:30Z"
}
```

**HTTP Status**: `422 Unprocessable Entity`

**Verification**:
- ✓ HTTP 422 status code
- ✓ Error message indicates salary range validation failure
- ✓ Request rejected before database query

---

### Scenario 12: Validation Error - Invalid Date Range

**Description**: Request with createdAfter > createdBefore.

**Test Case**: createdAfter=2024-12-31, createdBefore=2024-01-01 (invalid range)

**Command**:
```bash
curl -X GET "http://localhost:8080/api/employees/search?createdAfter=2024-12-31&createdBefore=2024-01-01" \
  -H "Accept: application/json" | jq
```

**Expected Response**:
```json
{
  "message": "Validation failed",
  "errors": [
    "Created after date must be before or equal to created before date"
  ],
  "timestamp": "2025-01-26T10:15:30Z"
}
```

**HTTP Status**: `422 Unprocessable Entity`

**Verification**:
- ✓ HTTP 422 status code
- ✓ Error message indicates date range validation failure

---

### Scenario 13: Resilience - Circuit Breaker Failure

**Description**: Verify circuit breaker behavior when database is unavailable.

**Test Case**: Stop PostgreSQL database and trigger circuit breaker

**Setup**:
```bash
# Stop PostgreSQL temporarily (platform-specific)
# Windows: Stop-Service postgresql-x64-16
# Linux: sudo systemctl stop postgresql
```

**Command**:
```bash
curl -X GET "http://localhost:8080/api/employees/search" \
  -H "Accept: application/json" | jq
```

**Expected Response** (after circuit opens):
```json
{
  "message": "Employee search temporarily unavailable",
  "errors": [
    "Database circuit breaker is OPEN - retry mechanism exhausted"
  ],
  "timestamp": "2025-01-26T10:15:30Z"
}
```

**HTTP Status**: `503 Service Unavailable`

**Verification**:
- ✓ HTTP 503 status code
- ✓ Error indicates service unavailability (not database error)
- ✓ Circuit breaker opens after configured failure threshold
- ✓ Subsequent requests fail fast (no database connection attempts)

**Cleanup**:
```bash
# Restart PostgreSQL
# Windows: Start-Service postgresql-x64-16
# Linux: sudo systemctl start postgresql

# Wait for circuit to close (30 seconds) or restart application
```

---

## Performance Validation

### Load Test (Optional)

**Tool**: Apache Bench (ab) or similar

**Command**:
```bash
ab -n 1000 -c 100 "http://localhost:8080/api/employees/search?size=10"
```

**Expected**:
- Throughput: > 100 requests/second
- P95 latency: < 500ms (for dataset up to 100k employees with proper indexes)
- No failed requests (circuit breaker should remain closed under normal load)

---

## Troubleshooting

### Issue: Empty results when expecting matches

**Check**:
1. Verify test data exists: `SELECT * FROM company.employee;`
2. Check filter parameters for typos (case-insensitive, but spelling matters)
3. Verify date format is ISO 8601 (YYYY-MM-DD)

### Issue: HTTP 503 errors

**Check**:
1. Database is running and accessible
2. Connection string in `application.yml` is correct
3. `DB_PASSWORD` environment variable is set
4. Check circuit breaker state: `http://localhost:8080/actuator/circuitbreakers`

### Issue: Slow response times

**Check**:
1. Database indexes exist on `emp_name`, `emp_salary`, `emp_created_date`
2. Run `EXPLAIN ANALYZE` on generated queries to verify index usage
3. Check dataset size (performance degrades with offset pagination on large datasets)

---

## Success Criteria Verification

| Criteria | Scenario | Verified |
|----------|----------|----------|
| SC-001: Retrieve up to 100 employees per request | Scenario 1, 2 | ✓ |
| SC-002: Results in < 500ms for 100k records | Performance Test | ⚠️ (requires load testing) |
| SC-003: Handle 1000 concurrent requests | Load Test | ⚠️ (requires load testing) |
| SC-004: Return DTOs, not entities | All scenarios | ✓ (verify JSON structure) |
| SC-005: Circuit breaker fails fast | Scenario 13 | ✓ |
| SC-006: Accurate filtered results | Scenarios 4-8 | ✓ |
| SC-007: Accurate API documentation | Verify Swagger UI | ⚠️ (manual check) |
| SC-008: 80% test coverage | JaCoCo report | ⚠️ (run after implementation) |

---

## Next Steps

1. **Implementation**: Follow `tasks.md` for TDD implementation workflow
2. **Testing**: Write automated tests covering all scenarios above
3. **Documentation**: Verify Swagger UI at `http://localhost:8080/swagger-ui.html` matches contract
4. **Performance**: Run load tests with realistic dataset sizes
5. **Indexes**: Create database indexes before production deployment

---

**Reference Links**:
- [data-model.md](./data-model.md) - Complete DTO and entity definitions
- [contracts/search-api.yml](./contracts/search-api.yml) - OpenAPI contract
- [spec.md](./spec.md) - Original feature specification

