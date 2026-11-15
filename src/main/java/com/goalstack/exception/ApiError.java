package com.goalstack.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * The JSON shape of every error the API returns. Never contains stack traces.
 * "code" is a short machine-readable name (for example AI_UNAVAILABLE) and is only present for some errors.
 */
public record ApiError(int status, String message, Map<String, String> fieldErrors,
                       @JsonInclude(JsonInclude.Include.NON_NULL) String code) {

    public ApiError(int status, String message, Map<String, String> fieldErrors) {
        this(status, message, fieldErrors, null);
    }
}
