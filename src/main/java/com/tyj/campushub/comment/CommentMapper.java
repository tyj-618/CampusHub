package com.tyj.campushub.comment;

import com.tyj.campushub.post.PageQueryResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class CommentMapper {

    private final JdbcTemplate jdbcTemplate;

    public CommentMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsNormalPost(Long postId) {
        String sql = "SELECT COUNT(*) FROM post WHERE id = ? AND status = 0";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, postId);
        return count != null && count > 0;
    }

    public Long saveComment(Long postId, Long userId, String content) {
        String sql = """
                INSERT INTO `comment` (post_id, user_id, content)
                VALUES (?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, postId);
            ps.setLong(2, userId);
            ps.setString(3, content.trim());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void increaseCommentCount(Long postId) {
        String sql = """
                UPDATE post_stat
                SET comment_count = comment_count + 1,
                    hot_score = hot_score + 3
                WHERE post_id = ?
                """;
        jdbcTemplate.update(sql, postId);
    }

    public void decreaseCommentCount(Long postId) {
        String sql = """
                UPDATE post_stat
                SET comment_count = GREATEST(comment_count - 1, 0),
                    hot_score = GREATEST(hot_score - 3, 0)
                WHERE post_id = ?
                """;
        jdbcTemplate.update(sql, postId);
    }

    public PageQueryResult<CommentPageItem> findCommentsByPostId(Long postId, int page, int size) {
        String countSql = "SELECT COUNT(*) FROM `comment` WHERE post_id = ? AND status = 0";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, postId);

        String sql = """
                SELECT c.id, c.post_id, c.user_id, c.content, c.created_at,
                       u.nickname AS author_nickname, u.avatar_url AS author_avatar_url
                FROM `comment` c
                JOIN `user` u ON c.user_id = u.id
                WHERE c.post_id = ? AND c.status = 0
                ORDER BY c.created_at ASC, c.id ASC
                LIMIT ? OFFSET ?
                """;

        List<CommentPageItem> records = jdbcTemplate.query(
                sql,
                this::mapCommentPageItem,
                postId,
                size,
                (page - 1) * size
        );

        return new PageQueryResult<>(total == null ? 0 : total, records);
    }

    public PageQueryResult<MyCommentItem> findCommentsByUserId(Long userId, int page, int size) {
        String countSql = "SELECT COUNT(*) FROM `comment` WHERE user_id = ? AND status = 0";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, userId);

        String sql = """
                SELECT c.id, c.post_id, p.title AS post_title, c.content, c.created_at
                FROM `comment` c
                JOIN post p ON c.post_id = p.id
                WHERE c.user_id = ? AND c.status = 0
                ORDER BY c.created_at DESC, c.id DESC
                LIMIT ? OFFSET ?
                """;

        List<MyCommentItem> records = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new MyCommentItem(
                        rs.getLong("id"),
                        rs.getLong("post_id"),
                        rs.getString("post_title"),
                        rs.getString("content"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ),
                userId,
                size,
                (page - 1) * size
        );

        return new PageQueryResult<>(total == null ? 0 : total, records);
    }

    public Optional<CommentDetail> findDetailById(Long commentId) {
        String sql = """
                SELECT c.id, c.post_id, c.user_id, p.user_id AS post_author_id,
                       c.content, c.status, c.created_at, c.updated_at
                FROM `comment` c
                JOIN post p ON c.post_id = p.id
                WHERE c.id = ?
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new CommentDetail(
                rs.getLong("id"),
                rs.getLong("post_id"),
                rs.getLong("user_id"),
                rs.getLong("post_author_id"),
                rs.getString("content"),
                rs.getInt("status"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
        ), commentId).stream().findFirst();
    }

    public void softDeleteComment(Long commentId) {
        String sql = "UPDATE `comment` SET status = 1 WHERE id = ?";
        jdbcTemplate.update(sql, commentId);
    }

    private CommentPageItem mapCommentPageItem(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new CommentPageItem(
                rs.getLong("id"),
                rs.getLong("post_id"),
                rs.getLong("user_id"),
                rs.getString("content"),
                rs.getString("author_nickname"),
                rs.getString("author_avatar_url"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
