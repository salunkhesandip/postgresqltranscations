package com.cleancoders.postgresqltranscations.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.fge.jsonpatch.JsonPatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/// Centralised HTTP error mapping for all domain and infrastructure exceptions.
///
/// **Java 21 features used:**
/// - **Pattern Matching for `switch`** (JEP 441) — one `@ExceptionHandler` for the
///   entire sealed [EmployeeDomainException] hierarchy. The `switch` is **exhaustive**
///   because [EmployeeDomainException] is sealed: the compiler enforces that every
///   permitted subtype (`EmployeeNotFoundException`, `EmployeeConflictException`,
///   `ServiceUnavailableException`) is covered. Adding a new subtype without
///   updating this switch causes a **compile error**, not a silent 500.
/// - **Sealed Classes** (JEP 409) — the exhaustiveness guarantee described above.
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public GlobalExceptionHandler() {
        super();
    }

    /// Handles all [EmployeeDomainException] subtypes with a single method.
    ///
    /// **Java 21: Pattern Matching for switch (JEP 441)**
    /// The `switch` is exhaustive — no `default` branch is needed or allowed.
    /// Attempting to add a new `permits` subtype without a matching `case` here
    /// produces a compile-time error.
    @ExceptionHandler(EmployeeDomainException.class)
    protected ResponseEntity<ErrorResponse> handleDomainException(EmployeeDomainException ex) {
        // Java 21: switch with type patterns — replaces three separate @ExceptionHandler methods.
        // Exhaustive because EmployeeDomainException is sealed.
        return switch (ex) {
            case EmployeeNotFoundException e ->
                    ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(), e.getMessage()));

            case EmployeeConflictException e ->
                    ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), e.getMessage()));

            case ServiceUnavailableException e ->
                    ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                            .body(ErrorResponse.of(HttpStatus.SERVICE_UNAVAILABLE.value(), e.getMessage()));
        };
    }

    /// Handles malformed or semantically invalid JSON Patch documents.
    /// Returns **HTTP 422 Unprocessable Entity** — the request was well-formed JSON
    /// but the patch semantics were invalid.
    @ExceptionHandler({JsonPatchException.class, JsonProcessingException.class})
    protected ResponseEntity<ErrorResponse> handleJsonPatch(Exception ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(HttpStatus.UNPROCESSABLE_ENTITY.value(),
                        "Invalid JSON Patch: " + ex.getMessage()));
    }

    /// Overrides the parent handler for `@Valid` constraint violations.
    /// Returns **HTTP 422 Unprocessable Entity** with a structured [ErrorResponse] body
    /// listing every field that failed validation.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                   HttpHeaders headers,
                                                                   HttpStatusCode status,
                                                                   WebRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse(ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(HttpStatus.UNPROCESSABLE_ENTITY.value(), detail));
    }
}
