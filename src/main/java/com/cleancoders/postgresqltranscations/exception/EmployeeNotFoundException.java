package com.cleancoders.postgresqltranscations.exception;

import java.io.Serial;
import java.util.Objects;

/// Thrown when an employee record cannot be found by the given identifier.
/// Maps to **HTTP 404 Not Found** in [GlobalExceptionHandler].
///
/// **Java 21 feature used:** `final` permitted subtype of sealed [EmployeeDomainException]
/// **Java 25 feature used:** Flexible Constructor Bodies (JEP 513) — validation before `super()`
public final class EmployeeNotFoundException extends EmployeeDomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    /// No-arg constructor for cases where the employee ID is not known at the throw site.
    public EmployeeNotFoundException() {
        super("Employee not found");
    }

    /// Constructs the exception with a descriptive message.
    ///
    /// @param message human-readable error detail; must not be `null`
    public EmployeeNotFoundException(String message) {
        // Java 25: Flexible Constructor Bodies — statement BEFORE super() is now legal.
        // Validates the contract without a static helper method.
        Objects.requireNonNull(message, "message must not be null");
        super(message);
    }

    /// Constructs the exception with a descriptive message and a root cause.
    ///
    /// @param message human-readable error detail; must not be `null`
    /// @param cause   the underlying throwable that triggered this exception
    public EmployeeNotFoundException(String message, Throwable cause) {
        Objects.requireNonNull(message, "message must not be null");
        super(message, cause);
    }
}
