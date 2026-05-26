package com.tyj.campushub.admin;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsPost(Long postId) {
        String sql = "SELECT COUNT(*) FROM post WHERE id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, postId);
        return count != null && count > 0;
    }

    public void updatePostStatus(Long postId, int status) {
        String sql = "UPDATE post SET status = ? WHERE id = ?";
        jdbcTemplate.update(sql, status, postId);
    }

    public boolean existsUser(Long userId) {
        String sql = "SELECT COUNT(*) FROM `user` WHERE id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, userId);
        return count != null && count > 0;
    }

    public void updateUserStatus(Long userId, int status) {
        String sql = "UPDATE `user` SET status = ? WHERE id = ?";
        jdbcTemplate.update(sql, status, userId);
    }
}
