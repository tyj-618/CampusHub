package com.tyj.campushub.auth;

public record LoginResponse(
        String token,
        long expiresIn,
        UserSummary user
) {
}
