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

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public GlobalExceptionHandler() {
        super();
    }

    @ExceptionHandler(EmployeeConflictException.class)
    protected ResponseEntity<ErrorResponse> handleConflict(EmployeeConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    protected ResponseEntity<ErrorResponse> handleNotFound(EmployeeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    /**
     * Handles {@link ServiceUnavailableException} thrown by circuit-breaker fallback methods.
     * Returns HTTP 503 so callers know the service is temporarily degraded.
     */
    @ExceptionHandler(ServiceUnavailableException.class)
    protected ResponseEntity<ErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of(HttpStatus.SERVICE_UNAVAILABLE.value(), ex.getMessage()));
    }

    /**
     * Handles malformed or invalid JSON Patch documents.
     * Returns HTTP 422 Unprocessable Entity — the request was well-formed JSON
     * but the patch semantics were invalid.
     */
    @ExceptionHandler({JsonPatchException.class, JsonProcessingException.class})
    protected ResponseEntity<ErrorResponse> handleJsonPatch(Exception ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(HttpStatus.UNPROCESSABLE_ENTITY.value(),
                        "Invalid JSON Patch: " + ex.getMessage()));
    }

    /**
     * Overrides the parent handler for {@code @Valid} constraint violations.
     * Returns HTTP 400 with a structured {@link ErrorResponse} body instead of the default ProblemDetail.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                   HttpHeaders headers,
                                                                   HttpStatusCode status,
                                                                   WebRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse(ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), detail));
    }
}
