package com.goalstack.controller;

import com.goalstack.ai.AIErrorType;
import com.goalstack.ai.AIProvider;
import com.goalstack.ai.AIProviderException;
import com.goalstack.ai.AIStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AIStatusController.class)
@AutoConfigureMockMvc(addFilters = false)
class AIStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AIProvider aiProvider;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void reportsARunningRealProvider() throws Exception {
        when(aiProvider.checkStatus()).thenReturn(
                new AIStatus("ollama", "llama3.1:8b", true, AIStatus.Availability.AVAILABLE));

        mockMvc.perform(get("/api/ai/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("ollama"))
                .andExpect(jsonPath("$.model").value("llama3.1:8b"))
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.availability").value("AVAILABLE"));
    }

    @Test
    void explainsDemoMode() throws Exception {
        when(aiProvider.checkStatus()).thenReturn(
                new AIStatus("fake", "fake-planner", true, AIStatus.Availability.DEMO));

        mockMvc.perform(get("/api/ai/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availability").value("DEMO"))
                .andExpect(jsonPath("$.message").value(containsString("Demo mode")));
    }

    @Test
    void reportsAMissingModelWithoutInternalDetails() throws Exception {
        when(aiProvider.checkStatus()).thenReturn(
                new AIStatus("ollama", "llama3.1:8b", false, AIStatus.Availability.MODEL_MISSING));

        mockMvc.perform(get("/api/ai/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.message").value(containsString("not installed")))
                .andExpect(content().string(not(containsString("localhost"))));
    }

    @Test
    void aProviderProblemBecomesASafeJsonErrorWithACode() throws Exception {
        when(aiProvider.checkStatus()).thenThrow(
                new AIProviderException(AIErrorType.UNAVAILABLE, "secret detail: connect to http://localhost:11434 failed"));

        mockMvc.perform(get("/api/ai/status"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value(containsString("create goals manually")))
                .andExpect(content().string(not(containsString("secret detail"))))
                .andExpect(content().string(not(containsString("11434"))));
    }
}
