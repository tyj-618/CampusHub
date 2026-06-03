package com.tyj.campushub.post;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ViewCountMapper {

    private final JdbcTemplate jdbcTemplate;

    public ViewCountMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void increaseViewCount(Long postId, long delta) {
        String sql = """
                UPDATE post_stat
                SET view_count = view_count + ?,
                    hot_score = hot_score + ?
                WHERE post_id = ?
                """;
        jdbcTemplate.update(sql, delta, delta, postId);
    }
}
