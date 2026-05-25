package com.tyj.campushub.auth;

public record RegisterResponse(
        Long userId,
        String username,
        String nickname
) {
}
