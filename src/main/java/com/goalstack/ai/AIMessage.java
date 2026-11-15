package com.goalstack.ai;

/** One message of a conversation sent to an AI provider. */
public record AIMessage(AIRole role, String content) {

    public AIMessage {
        if (role == null || content == null) {
            throw new IllegalArgumentException("An AI message needs a role and content.");
        }
    }

    public static AIMessage system(String content) {
        return new AIMessage(AIRole.SYSTEM, content);
    }

    public static AIMessage user(String content) {
        return new AIMessage(AIRole.USER, content);
    }

    public static AIMessage assistant(String content) {
        return new AIMessage(AIRole.ASSISTANT, content);
    }
}
