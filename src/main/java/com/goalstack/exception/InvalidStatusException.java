package com.goalstack.exception;

/** Thrown when a subgoal status is not PENDING, IN_PROGRESS or COMPLETED. */
public class InvalidStatusException extends RuntimeException {

    public InvalidStatusException(String message) {
        super(message);
    }
}
