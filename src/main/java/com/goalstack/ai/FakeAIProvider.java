package com.goalstack.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A built-in pretend AI for development, demos and tests. It needs no model, no internet and costs nothing.
 * Its answers are scripted: the first user message gets a clarifying question, later ones say "ready", and
 * a plan request returns a five-step demo plan. It can also fail on purpose (see app.ai.fake.failure-mode)
 * so the error screens can be tried without breaking anything.
 */
public class FakeAIProvider implements AIProvider {

    private static final Pattern HOURS = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:hours?|hrs?|h)\\b",
            Pattern.CASE_INSENSITIVE);

    private final AIProperties.Fake config;
    private final ObjectMapper objectMapper;

    public FakeAIProvider(AIProperties.Fake config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
    }

    @Override
    public AIProviderResponse generate(AIProviderRequest request) {
        pause();
        switch (config.failureMode()) {
            case UNAVAILABLE -> throw new AIProviderException(AIErrorType.UNAVAILABLE, "Fake provider: simulated outage");
            case RATE_LIMITED -> throw new AIProviderException(AIErrorType.RATE_LIMITED, "Fake provider: simulated rate limit");
            case TIMEOUT -> throw new AIProviderException(AIErrorType.TIMEOUT, "Fake provider: simulated timeout");
            case BAD_JSON -> {
                return response("Sorry, here is my answer without any JSON.");
            }
            default -> {
                // NONE: carry on
            }
        }

        String schema = request.schemaName();
        if (AISchemas.ASSISTANT_TURN.equals(schema)) {
            return response(toJson(assistantTurn(request.messages())));
        }
        if (AISchemas.GENERATED_PLAN.equals(schema)) {
            return response(toJson(generatedPlan(request.messages())));
        }
        return response("This is the fake AI. Set AI_PROVIDER=ollama to use a real model.");
    }

    private Map<String, Object> assistantTurn(List<AIMessage> messages) {
        List<String> userTexts = messages.stream()
                .filter(m -> m.role() == AIRole.USER)
                .map(AIMessage::content)
                .toList();

        Map<String, Object> facts = new LinkedHashMap<>();
        if (!userTexts.isEmpty()) {
            facts.put("desiredOutcome", shorten(userTexts.get(0), 300));
        }
        Double hours = findHoursPerWeek(String.join(" ", userTexts));
        if (hours != null) {
            facts.put("hoursPerWeek", hours);
        }

        boolean ready = userTexts.size() >= 2;
        String reply = ready
                ? "Thanks, that is enough for a first plan. Press Generate plan when you are ready. (Demo answer from the fake AI.)"
                : "Good goal! To plan it well: what is your experience with this so far, and how many hours per week can you spend? (Demo answer from the fake AI.)";

        Map<String, Object> turn = new LinkedHashMap<>();
        turn.put("reply", reply);
        turn.put("readyToGenerate", ready);
        turn.put("facts", facts);
        return turn;
    }

    private Map<String, Object> generatedPlan(List<AIMessage> messages) {
        String firstUserText = messages.stream()
                .filter(m -> m.role() == AIRole.USER)
                .map(AIMessage::content)
                .findFirst()
                .orElse("My goal");

        String[][] steps = {
                {"Learn the fundamentals", "Cover the basic ideas and vocabulary.", "10"},
                {"Practise with small exercises", "Repeat short exercises until they feel easy.", "25"},
                {"Build a first project", "Apply what you learned in one complete piece of work.", "15"},
                {"Study advanced topics", "Go deeper into the harder parts.", "35"},
                {"Review and polish", "Fix gaps and finish the project.", "15"}
        };
        List<Map<String, Object>> subgoals = new java.util.ArrayList<>();
        for (String[] step : steps) {
            Map<String, Object> subgoal = new LinkedHashMap<>();
            subgoal.put("title", step[0]);
            subgoal.put("description", step[1]);
            subgoal.put("estimatedHours", Double.parseDouble(step[2]));
            subgoals.add(subgoal);
        }

        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("goalTitle", shorten(firstUserText, 120));
        plan.put("goalDescription", "Demo plan from the built-in fake AI. The hours are placeholders, not real estimates.");
        plan.put("subgoals", subgoals);
        plan.put("assumptions", List.of("This is demo data from the fake provider."));
        return plan;
    }

    private static Double findHoursPerWeek(String text) {
        Matcher matcher = HOURS.matcher(text);
        Double found = null;
        while (matcher.find()) {
            found = Double.parseDouble(matcher.group(1));
        }
        return found;
    }

    private void pause() {
        long millis = config.delay().toMillis();
        if (millis > 0) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new AIProviderException(AIErrorType.BAD_RESPONSE, "Fake provider could not write JSON", ex);
        }
    }

    private AIProviderResponse response(String content) {
        return new AIProviderResponse(content, modelName(), null, null, "stop");
    }

    private static String shorten(String text, int max) {
        String trimmed = text.trim();
        return trimmed.length() > max ? trimmed.substring(0, max).trim() : trimmed;
    }

    @Override
    public AIStatus checkStatus() {
        if (config.failureMode() == AIProperties.Fake.FailureMode.UNAVAILABLE) {
            return new AIStatus(name(), modelName(), false, AIStatus.Availability.UNREACHABLE);
        }
        return new AIStatus(name(), modelName(), true, AIStatus.Availability.DEMO);
    }

    @Override
    public String name() {
        return "fake";
    }

    @Override
    public String modelName() {
        return "fake-planner";
    }
}
