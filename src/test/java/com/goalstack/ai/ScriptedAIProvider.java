package com.goalstack.ai;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** A provider for tests: returns the answers (or failures) you queue, in order, and remembers every request. */
class ScriptedAIProvider implements AIProvider {

    final List<AIProviderRequest> requests = new ArrayList<>();
    private final Deque<Object> script = new ArrayDeque<>();

    ScriptedAIProvider reply(String content) {
        script.add(new AIProviderResponse(content, "scripted", null, null, "stop"));
        return this;
    }

    ScriptedAIProvider fail(AIErrorType type) {
        script.add(new AIProviderException(type, "scripted failure " + type));
        return this;
    }

    @Override
    public AIProviderResponse generate(AIProviderRequest request) {
        requests.add(request);
        Object next = script.poll();
        if (next == null) {
            throw new IllegalStateException("The test script has no more answers");
        }
        if (next instanceof AIProviderException exception) {
            throw exception;
        }
        return (AIProviderResponse) next;
    }

    @Override
    public AIStatus checkStatus() {
        return new AIStatus(name(), modelName(), true, AIStatus.Availability.AVAILABLE);
    }

    @Override
    public String name() {
        return "scripted";
    }

    @Override
    public String modelName() {
        return "scripted-model";
    }

    static AIProviderRequest simpleRequest() {
        return AIProviderRequest.structured(List.of(AIMessage.system("Be helpful."), AIMessage.user("Hello")),
                AISchemas.ASSISTANT_TURN, AISchemas.assistantTurn(), 0.2, 300);
    }
}
