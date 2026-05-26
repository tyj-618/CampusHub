package com.tyj.campushub.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserProfile> findProfileById(Long userId) {
        String sql = """
                SELECT id, username, nickname, avatar_url, bio, role, status, created_at, updated_at
                FROM `user`
                WHERE id = ?
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new UserProfile(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("nickname"),
                rs.getString("avatar_url"),
                rs.getString("bio"),
                rs.getInt("role"),
                rs.getInt("status"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
        ), userId).stream().findFirst();
    }

    public void updateProfile(Long userId, String nickname, String avatarUrl, String bio) {
        String sql = """
                UPDATE `user`
                SET nickname = ?, avatar_url = ?, bio = ?
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, nickname, avatarUrl, bio, userId);
    }

    public long countNormalPostsByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM post WHERE user_id = ? AND status = 0";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, userId);
        return count == null ? 0 : count;
    }

    public long countNormalCommentsByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM `comment` WHERE user_id = ? AND status = 0";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, userId);
        return count == null ? 0 : count;
    }
}
