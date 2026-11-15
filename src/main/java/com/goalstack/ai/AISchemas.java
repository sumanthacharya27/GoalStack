package com.goalstack.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON Schemas that tell the model what shape its answer must have. They match the DTOs in
 * com.goalstack.ai.dto. The schema only controls the SHAPE; every value is still checked by Bean Validation.
 * Size limits are left out on purpose (not every runtime supports them); the DTOs enforce them.
 */
public final class AISchemas {

    public static final String ASSISTANT_TURN = "assistant_turn";
    public static final String GENERATED_PLAN = "generated_plan";

    private AISchemas() {
    }

    /** One conversation turn: the reply to show, whether enough is known to plan, and the facts collected so far. */
    public static Map<String, Object> assistantTurn() {
        Map<String, Object> facts = object(props(
                "desiredOutcome", string(),
                "currentExperience", string(),
                "existingKnowledge", string(),
                "hoursPerWeek", number(),
                "timeframeWeeks", integer(),
                "constraints", array(string())));

        return object(props(
                "reply", string(),
                "readyToGenerate", bool(),
                "facts", facts), "reply", "readyToGenerate");
    }

    /** A full plan: goal title and description, ordered subgoals with hours, and the assumptions made. */
    public static Map<String, Object> generatedPlan() {
        Map<String, Object> subgoal = object(props(
                "title", string(),
                "description", string(),
                "estimatedHours", number()), "title", "estimatedHours");

        return object(props(
                "goalTitle", string(),
                "goalDescription", string(),
                "subgoals", array(subgoal),
                "assumptions", array(string())), "goalTitle", "subgoals");
    }

    // ---------- small helpers ----------

    private static Map<String, Object> object(Map<String, Object> properties, String... required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (required.length > 0) {
            schema.put("required", List.of(required));
        }
        schema.put("additionalProperties", false);
        return schema;
    }

    private static Map<String, Object> props(Object... keysAndValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }

    private static Map<String, Object> string() {
        return Map.of("type", "string");
    }

    private static Map<String, Object> number() {
        return Map.of("type", "number");
    }

    private static Map<String, Object> integer() {
        return Map.of("type", "integer");
    }

    private static Map<String, Object> bool() {
        return Map.of("type", "boolean");
    }

    private static Map<String, Object> array(Map<String, Object> items) {
        return Map.of("type", "array", "items", items);
    }
}
