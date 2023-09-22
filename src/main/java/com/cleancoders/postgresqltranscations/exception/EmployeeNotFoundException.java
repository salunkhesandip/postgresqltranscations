package com.cleancoders.postgresqltranscations.exception;

import java.io.Serial;

public class EmployeeNotFoundException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public EmployeeNotFoundException(){
        super();
    }

    public EmployeeNotFoundException(String message) {
        super(message);
    }

    public EmployeeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
