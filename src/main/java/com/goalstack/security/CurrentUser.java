package com.goalstack.security;

import java.security.Principal;

/** Reads the logged-in user's id from the JWT. The token's "subject" is the user id. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id(Principal principal) {
        return Long.valueOf(principal.getName());
    }
}
