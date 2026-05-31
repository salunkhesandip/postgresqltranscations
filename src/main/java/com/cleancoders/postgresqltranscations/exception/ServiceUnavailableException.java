package com.cleancoders.postgresqltranscations.exception;

import java.io.Serial;

/**
 * Thrown by circuit-breaker fallback methods when the underlying DB call
 * cannot be completed because the circuit is OPEN.
 * Mapped to HTTP 503 Service Unavailable by {@link GlobalExceptionHandler}.
 */
public class ServiceUnavailableException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

