package com.goalstack.ai;

/** The kinds of AI problems, each with the HTTP answer the API gives. The user messages never contain internal details. */
public enum AIErrorType {

    UNAVAILABLE(503, "AI_UNAVAILABLE",
            "The AI assistant is not available right now. You can still create goals manually.", true),
    TIMEOUT(504, "AI_TIMEOUT",
            "The AI assistant took too long to answer. Please try again.", false),
    RATE_LIMITED(429, "AI_RATE_LIMITED",
            "The AI assistant is busy. Please wait a moment and try again.", true),
    BAD_RESPONSE(502, "AI_BAD_RESPONSE",
            "The AI assistant gave an answer that could not be used. Please try again.", false),
    NOT_CONFIGURED(503, "AI_NOT_CONFIGURED",
            "The AI assistant is not set up correctly. You can still create goals manually.", false),
    REQUEST_REJECTED(502, "AI_REQUEST_REJECTED",
            "The AI assistant could not process this request.", false);

    private final int httpStatus;
    private final String code;
    private final String userMessage;
    private final boolean retryable;

    AIErrorType(int httpStatus, String code, String userMessage, boolean retryable) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.userMessage = userMessage;
        this.retryable = retryable;
    }

    public int getHttpStatus() { return httpStatus; }
    public String getCode() { return code; }
    public String getUserMessage() { return userMessage; }

    /** True if trying the same request again may work (a busy or briefly unavailable server). */
    public boolean isRetryable() { return retryable; }
}
