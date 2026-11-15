package com.goalstack.ai;

/**
 * Any problem with an AI provider. The message is for the log only (it may mention the vendor's error text);
 * what the user sees comes from {@link AIErrorType#getUserMessage()}.
 */
public class AIProviderException extends RuntimeException {

    private final AIErrorType type;

    public AIProviderException(AIErrorType type, String internalDetail) {
        super(internalDetail);
        this.type = type;
    }

    public AIProviderException(AIErrorType type, String internalDetail, Throwable cause) {
        super(internalDetail, cause);
        this.type = type;
    }

    public AIErrorType getType() {
        return type;
    }
}
