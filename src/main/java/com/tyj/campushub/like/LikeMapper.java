package com.tyj.campushub.like;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class LikeMapper {

    private final JdbcTemplate jdbcTemplate;

    public LikeMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsNormalPost(Long postId) {
        String sql = "SELECT COUNT(*) FROM post WHERE id = ? AND status = 0";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, postId);
        return count != null && count > 0;
    }

    public Optional<LikeRecord> findByPostIdAndUserId(Long postId, Long userId) {
        String sql = """
                SELECT id, post_id, user_id, status
                FROM post_like
                WHERE post_id = ? AND user_id = ?
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new LikeRecord(
                rs.getLong("id"),
                rs.getLong("post_id"),
                rs.getLong("user_id"),
                rs.getInt("status")
        ), postId, userId).stream().findFirst();
    }

    public void saveLike(Long postId, Long userId) {
        String sql = """
                INSERT INTO post_like (post_id, user_id, status)
                VALUES (?, ?, 0)
                """;
        jdbcTemplate.update(sql, postId, userId);
    }

    public void activateLike(Long likeId) {
        String sql = "UPDATE post_like SET status = 0 WHERE id = ?";
        jdbcTemplate.update(sql, likeId);
    }

    public void cancelLike(Long likeId) {
        String sql = "UPDATE post_like SET status = 1 WHERE id = ?";
        jdbcTemplate.update(sql, likeId);
    }

    public void increaseLikeCount(Long postId) {
        String sql = """
                UPDATE post_stat
                SET like_count = like_count + 1,
                    hot_score = hot_score + 2
                WHERE post_id = ?
                """;
        jdbcTemplate.update(sql, postId);
    }

    public void decreaseLikeCount(Long postId) {
        String sql = """
                UPDATE post_stat
                SET like_count = GREATEST(like_count - 1, 0),
                    hot_score = GREATEST(hot_score - 2, 0)
                WHERE post_id = ?
                """;
        jdbcTemplate.update(sql, postId);
    }

    public int findLikeCount(Long postId) {
        String sql = "SELECT like_count FROM post_stat WHERE post_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, postId);
        return count == null ? 0 : count;
    }
}
