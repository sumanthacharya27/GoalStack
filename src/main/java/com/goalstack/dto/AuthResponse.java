package com.goalstack.dto;

/** Returned after a successful register or login. */
public record AuthResponse(String token, String tokenType, long expiresInSeconds, UserResponse user) {
}
