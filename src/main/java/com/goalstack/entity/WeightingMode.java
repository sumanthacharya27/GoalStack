package com.goalstack.entity;

/** How the weights of a goal's subgoals are decided. */
public enum WeightingMode {
    /** Goals made before Version 3: every subgoal has the same weight. No hours are stored. */
    EQUAL,
    /** Goals made in Version 3 or later: weight = subgoal hours / total hours. */
    EFFORT
}
