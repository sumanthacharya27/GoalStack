package com.goalstack.exception;

import java.util.Map;

/** A business-rule validation problem found in a service (as opposed to a simple annotation check). Gives a 400 response. */
public class ValidationFailedException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public ValidationFailedException(String field, String message) {
        super(message);
        this.fieldErrors = Map.of(field, message);
    }

    public ValidationFailedException(String message) {
        super(message);
        this.fieldErrors = Map.of();
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
