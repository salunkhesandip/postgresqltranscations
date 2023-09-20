package com.cleancoders.postgresqltranscations.exception;

import java.io.Serial;
public class EmployeeConflictException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public EmployeeConflictException(){
        super();
    }

    public EmployeeConflictException(String message) {
        super(message);
    }

    public EmployeeConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
