package com.goalstack.exception;

/** Wrong email or password, or a token that points to a user that no longer exists. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password.");
    }
}
