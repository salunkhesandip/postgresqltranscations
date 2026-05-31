package com.cleancoders.postgresqltranscations.exception;

import java.time.Instant;

/**
 * Structured JSON error body returned by {@link GlobalExceptionHandler}.
 * Using a record keeps it immutable and boilerplate-free.
 */
public record ErrorResponse(int status, String message, Instant timestamp) {

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, Instant.now());
    }
}

