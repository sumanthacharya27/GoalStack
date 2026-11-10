package com.goalstack.dto;

import com.goalstack.entity.User;

/** What the React app is told about the logged-in user (never the password hash). */
public record UserResponse(Long id, String name, String email) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
