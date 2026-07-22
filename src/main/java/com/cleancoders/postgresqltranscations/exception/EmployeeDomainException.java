package com.cleancoders.postgresqltranscations.exception;

/// Sealed base class for all domain exceptions in the Employee service.
///
/// Permitted subtypes: [EmployeeNotFoundException], [EmployeeConflictException],
/// [ServiceUnavailableException]. Because this hierarchy is **sealed**, the compiler
/// guarantees that a pattern-matching `switch` in [GlobalExceptionHandler] is
/// **exhaustive** — if a new exception type is ever added here but not handled,
/// the code will fail to compile.
///
/// **Java 21+ feature used:** Sealed Classes (JEP 409)
/// **Java 23+ feature used:** Markdown Javadoc (`///`) (JEP 467)
public abstract sealed class EmployeeDomainException extends RuntimeException
        permits EmployeeNotFoundException, EmployeeConflictException, ServiceUnavailableException {

    protected EmployeeDomainException(String message) {
        super(message);
    }

    protected EmployeeDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}

