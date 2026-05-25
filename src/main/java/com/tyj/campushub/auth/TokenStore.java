package com.tyj.campushub.auth;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {

    private static final Duration TOKEN_TTL = Duration.ofHours(2);

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, StoredToken> tokens = new ConcurrentHashMap<>();

    public TokenSession createSession(Long userId) {
        String token = generateToken();
        Instant expiresAt = Instant.now().plus(TOKEN_TTL);
        tokens.put(token, new StoredToken(userId, expiresAt));
        return new TokenSession(token, userId, TOKEN_TTL.toSeconds());
    }

    public Optional<Long> findUserId(String token) {
        StoredToken storedToken = tokens.get(token);
        if (storedToken == null) {
            return Optional.empty();
        }

        if (storedToken.expiresAt().isBefore(Instant.now())) {
            tokens.remove(token);
            return Optional.empty();
        }

        return Optional.of(storedToken.userId());
    }

    public void remove(String token) {
        tokens.remove(token);
    }

    public Optional<String> resolveBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Optional.empty();
        }
        return Optional.of(authorization.substring("Bearer ".length()).trim())
                .filter(token -> !token.isBlank());
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record StoredToken(Long userId, Instant expiresAt) {
    }
}
