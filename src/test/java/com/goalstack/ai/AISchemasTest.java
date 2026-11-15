package com.goalstack.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AISchemasTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void assistantTurnSchemaRequiresReplyAndReadyFlag() {
        JsonNode schema = objectMapper.valueToTree(AISchemas.assistantTurn());

        assertThat(schema.path("type").asText()).isEqualTo("object");
        assertThat(schema.path("additionalProperties").asBoolean(true)).isFalse();
        assertThat(schema.path("required")).extracting(JsonNode::asText).containsExactlyInAnyOrder("reply", "readyToGenerate");
        assertThat(schema.path("properties").path("readyToGenerate").path("type").asText()).isEqualTo("boolean");
        JsonNode facts = schema.path("properties").path("facts");
        assertThat(facts.path("properties").path("hoursPerWeek").path("type").asText()).isEqualTo("number");
        assertThat(facts.path("properties").path("constraints").path("type").asText()).isEqualTo("array");
        assertThat(facts.has("required")).isFalse(); // every fact is optional
    }

    @Test
    void generatedPlanSchemaRequiresTitleAndSubgoalsWithHours() {
        JsonNode schema = objectMapper.valueToTree(AISchemas.generatedPlan());

        assertThat(schema.path("required")).extracting(JsonNode::asText).containsExactlyInAnyOrder("goalTitle", "subgoals");
        JsonNode subgoalItem = schema.path("properties").path("subgoals").path("items");
        assertThat(subgoalItem.path("required")).extracting(JsonNode::asText).containsExactlyInAnyOrder("title", "estimatedHours");
        assertThat(subgoalItem.path("properties").path("estimatedHours").path("type").asText()).isEqualTo("number");
    }

    @Test
    void theModelIsNotAskedForWeightsOrDeadlines() throws Exception {
        String json = objectMapper.writeValueAsString(AISchemas.generatedPlan());

        assertThat(json).doesNotContain("weight").doesNotContain("deadline");
    }

    @Test
    void schemasCanBeWrittenAsJson() throws Exception {
        assertThat(objectMapper.writeValueAsString(AISchemas.assistantTurn())).startsWith("{").contains("\"reply\"");
        assertThat(objectMapper.writeValueAsString(AISchemas.generatedPlan())).startsWith("{").contains("\"subgoals\"");
    }
}
