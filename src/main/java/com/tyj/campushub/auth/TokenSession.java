package com.tyj.campushub.auth;

public record TokenSession(
        String token,
        Long userId,
        long expiresIn
) {
}
