package com.tyj.campushub.auth;

public record UserSummary(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        Integer role
) {
}
