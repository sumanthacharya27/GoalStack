package com.goalstack.controller;

import com.goalstack.dto.GoalView;
import com.goalstack.entity.Goal;
import com.goalstack.entity.Subgoal;
import com.goalstack.entity.SubgoalStatus;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @Test
    void subgoalHoursBelowTheMinimumAreRejected() throws Exception {
        mockMvc.perform(post("/api/goals/1/subgoals")
                        .principal(USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Step\",\"estimatedHours\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.estimatedHours").exists());
    }

    @Test
    void statusChangeReturnsTheRefreshedGoalProgress() throws Exception {
        Goal goal = new Goal();
        ReflectionTestUtils.setField(goal, "id", 10L);
        Subgoal subgoal = new Subgoal();
        subgoal.setGoal(goal);
        subgoal.setTitle("Step");
        subgoal.setStatus(SubgoalStatus.COMPLETED);
        when(subgoalService.updateStatus(1L, 5L, "COMPLETED")).thenReturn(subgoal);
        when(goalService.getGoalView(1L, 10L)).thenReturn(
                new GoalView(goal, List.of(), 1, 2, new BigDecimal("35.00"), false, false, null));

        mockMvc.perform(patch("/api/subgoals/5/status")
                        .principal(USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goalProgress").value(35.0))
                .andExpect(jsonPath("$.goalComplete").value(false))
                .andExpect(jsonPath("$.subgoal.status").value("COMPLETED"));
    }
}
