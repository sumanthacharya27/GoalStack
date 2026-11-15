package com.goalstack.service;

import com.goalstack.ai.AIMessage;
import com.goalstack.ai.AIProviderRequest;
import com.goalstack.ai.AISchemas;
import com.goalstack.ai.StructuredAIClient;
import com.goalstack.ai.dto.AssistantTurn;
import com.goalstack.ai.dto.GeneratedPlan;
import com.goalstack.dto.AIChatMessageRequest;
import com.goalstack.dto.AIChatRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Small planning workflow shared by the chat UI and the AI provider abstraction.
 * Chat history is deliberately kept in the browser for this demo; no chat table is required.
 */
@Service
public class AIPlanningService {

    private static final String SYSTEM_PROMPT = """
            You are GoalStack, a concise and encouraging goal-planning assistant.
            Help the user turn one goal into practical, ordered subgoals.
            Ask one useful follow-up question at a time about their experience, available time,
            deadline or constraints. Do not claim you have saved anything.
            For the assistant_turn schema, return a short helpful reply and set readyToGenerate
            to true once the goal and at least one useful planning detail are understood.
            Keep replies simple and suitable for a student demo.
            """;

    private static final String PLAN_PROMPT = """
            Create a practical plan from the conversation. Return a clear goal title and description,
            and 3 to 6 ordered subgoals. Every subgoal must have a realistic estimatedHours value
            between 0.25 and 1000. Use specific actions, not vague labels. Do not include markdown.
            """;

    private final StructuredAIClient aiClient;

    public AIPlanningService(StructuredAIClient aiClient) {
        this.aiClient = aiClient;
    }

    public AssistantTurn chat(AIChatRequest request) {
        List<AIMessage> messages = toMessages(request);
        messages.add(0, AIMessage.system(SYSTEM_PROMPT));
        return aiClient.generate(
                AIProviderRequest.structured(messages, AISchemas.ASSISTANT_TURN,
                        AISchemas.assistantTurn(), 0.25, 1000),
                AssistantTurn.class);
    }

    public GeneratedPlan generatePlan(AIChatRequest request) {
        List<AIMessage> messages = toMessages(request);
        messages.add(0, AIMessage.system(SYSTEM_PROMPT + "\n" + PLAN_PROMPT));
        return aiClient.generate(
                AIProviderRequest.structured(messages, AISchemas.GENERATED_PLAN,
                        AISchemas.generatedPlan(), 0.2, 2200),
                GeneratedPlan.class);
    }

    private List<AIMessage> toMessages(AIChatRequest request) {
        List<AIMessage> messages = new ArrayList<>();
        // Keep the most recent turns to avoid sending an unbounded prompt.
        List<AIChatMessageRequest> source = request.messages();
        int start = Math.max(0, source.size() - 12);
        for (int i = start; i < source.size(); i++) {
            AIChatMessageRequest item = source.get(i);
            String content = item.content().trim();
            if (item.role() == AIChatMessageRequest.ChatRole.USER) {
                messages.add(AIMessage.user(content));
            } else {
                messages.add(AIMessage.assistant(content));
            }
        }
        return messages;
    }
}
