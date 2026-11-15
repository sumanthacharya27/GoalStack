package com.goalstack.dto;

import java.math.BigDecimal;

/** Returned after a status change: the updated subgoal plus the goal's refreshed progress. */
public record StatusResponse(SubgoalResponse subgoal, BigDecimal goalProgress, boolean goalComplete) {
}
