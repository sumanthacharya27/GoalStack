package com.goalstack.controller;

import com.goalstack.exception.ResourceNotFoundException;
import com.goalstack.service.GoalService;
import com.goalstack.service.SubgoalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security filters are switched off here (addFilters = false) so these tests only check
 * validation and error handling. The logged-in user is supplied directly with .principal(...).
 */
@WebMvcTest({GoalController.class, SubgoalController.class})
@AutoConfigureMockMvc(addFilters = false)
class GoalControllerTest {

    private static final TestingAuthenticationToken USER = new TestingAuthenticationToken("1", null);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoalService goalService;

    @MockitoBean
    private SubgoalService subgoalService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void blankTitleIsRejectedWithFieldError() throws Exception {
        mockMvc.perform(post("/api/goals")
                        .principal(USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").value("Title is required."));
    }

    @Test
    void titleLongerThan120CharactersIsRejected() throws Exception {
        String longTitle = "a".repeat(121);
        mockMvc.perform(post("/api/goals")
                        .principal(USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + longTitle + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void missingGoalReturns404Json() throws Exception {
        when(goalService.getGoalView(1L, 99L)).thenThrow(new ResourceNotFoundException("Goal with id 99 was not found."));

        mockMvc.perform(get("/api/goals/99").principal(USER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Goal with id 99 was not found."));
    }
}
