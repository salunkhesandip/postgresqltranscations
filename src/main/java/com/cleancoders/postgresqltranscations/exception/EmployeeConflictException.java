package com.cleancoders.postgresqltranscations.exception;

import java.io.Serial;
import java.util.Objects;

/// Thrown when an employee creation request conflicts with an existing record.
/// Maps to **HTTP 409 Conflict** in [GlobalExceptionHandler].
///
/// **Java 21 feature used:** `final` permitted subtype of sealed [EmployeeDomainException]
/// **Java 25 feature used:** Flexible Constructor Bodies (JEP 513) — validation before `super()`
public final class EmployeeConflictException extends EmployeeDomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    /// No-arg constructor for cases where no additional context is available.
    public EmployeeConflictException() {
        super("Employee already exists");
    }

    /// Constructs the exception with a descriptive message.
    ///
    /// @param message human-readable conflict detail; must not be `null`
    public EmployeeConflictException(String message) {
        // Java 25: Flexible Constructor Bodies — validate before delegating to super.
        Objects.requireNonNull(message, "message must not be null");
        super(message);
    }

    /// Constructs the exception with a descriptive message and a root cause.
    ///
    /// @param message human-readable conflict detail; must not be `null`
    /// @param cause   the underlying throwable that triggered this exception
    public EmployeeConflictException(String message, Throwable cause) {
        Objects.requireNonNull(message, "message must not be null");
        super(message, cause);
    }
}
