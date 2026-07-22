package com.cleancoders.postgresqltranscations.exception;

import java.io.Serial;
import java.util.Objects;

/// Thrown by circuit-breaker fallback methods when the underlying DB call cannot
/// be completed because the circuit is **OPEN**.
/// Maps to **HTTP 503 Service Unavailable** in [GlobalExceptionHandler].
///
/// The exception message includes the **correlation ID** sourced from
/// [com.cleancoders.postgresqltranscations.context.RequestContext#CORRELATION_ID]
/// (a Scoped Value — JEP 506) to aid distributed tracing.
///
/// **Java 21 feature used:** `final` permitted subtype of sealed [EmployeeDomainException]
/// **Java 25 feature used:** Flexible Constructor Bodies (JEP 513) — validation before `super()`
public final class ServiceUnavailableException extends EmployeeDomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    /// Constructs the exception with a descriptive message.
    ///
    /// @param message human-readable unavailability detail; must not be `null`
    public ServiceUnavailableException(String message) {
        // Java 25: Flexible Constructor Bodies — validate before delegating to super.
        Objects.requireNonNull(message, "message must not be null");
        super(message);
    }

    /// Constructs the exception with a descriptive message and the triggering cause.
    ///
    /// @param message human-readable unavailability detail; must not be `null`
    /// @param cause   the throwable that opened the circuit (e.g. a DB error)
    public ServiceUnavailableException(String message, Throwable cause) {
        Objects.requireNonNull(message, "message must not be null");
        super(message, cause);
    }
}
