package com.tyj.campushub.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AuthRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuthRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM `user` WHERE username = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username);
        return count != null && count > 0;
    }

    public void save(String username, String encodedPassword, String nickname) {
        String sql = """
                INSERT INTO `user` (username, password, nickname)
                VALUES (?, ?, ?)
                """;
        jdbcTemplate.update(sql, username, encodedPassword, nickname);
    }

    public Optional<AuthUser> findByUsername(String username) {
        String sql = """
                SELECT id, username, password, nickname, avatar_url, role, status
                FROM `user`
                WHERE username = ?
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new AuthUser(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("nickname"),
                rs.getString("avatar_url"),
                rs.getInt("role"),
                rs.getInt("status")
        ), username).stream().findFirst();
    }
}
