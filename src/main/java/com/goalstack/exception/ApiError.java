package com.goalstack.exception;

import java.util.Map;

/** The JSON shape of every error the API returns. Never contains stack traces. */
public record ApiError(int status, String message, Map<String, String> fieldErrors) {
}
