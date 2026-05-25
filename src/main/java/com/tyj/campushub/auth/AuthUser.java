package com.tyj.campushub.auth;

public record AuthUser(
        Long id,
        String username,
        String password,
        String nickname,
        String avatarUrl,
        Integer role,
        Integer status
) {
}
