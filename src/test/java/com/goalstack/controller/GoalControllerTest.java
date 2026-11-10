package com.goalstack.controller;

import com.goalstack.exception.ResourceNotFoundException;
import com.goalstack.service.GoalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({GoalController.class, SubgoalController.class})
class GoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoalService goalService;

    @MockitoBean
    private com.goalstack.service.SubgoalService subgoalService;

    @Test
    void blankTitleIsRejectedWithFieldError() throws Exception {
        mockMvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").value("Title is required."));
    }

    @Test
    void titleLongerThan120CharactersIsRejected() throws Exception {
        String longTitle = "a".repeat(121);
        mockMvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + longTitle + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void missingGoalReturns404Json() throws Exception {
        when(goalService.getGoalView(99L)).thenThrow(new ResourceNotFoundException("Goal with id 99 was not found."));

        mockMvc.perform(get("/api/goals/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Goal with id 99 was not found."));
    }
}
