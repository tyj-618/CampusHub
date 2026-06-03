package com.tyj.campushub.post;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class PostMapper {

    private final JdbcTemplate jdbcTemplate;

    public PostMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsEnabledCategory(Long categoryId) {
        String sql = "SELECT COUNT(*) FROM category WHERE id = ? AND status = 0";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, categoryId);
        return count != null && count > 0;
    }

    public Long savePost(Long userId, CreatePostRequest request) {
        String sql = """
                INSERT INTO post (user_id, category_id, title, content)
                VALUES (?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, userId);
            ps.setLong(2, request.categoryId());
            ps.setString(3, request.title().trim());
            ps.setString(4, request.content().trim());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void savePostStat(Long postId) {
        String sql = "INSERT INTO post_stat (post_id) VALUES (?)";
        jdbcTemplate.update(sql, postId);
    }

    public Optional<PostDetail> findDetailById(Long postId) {
        String sql = """
                SELECT p.id, p.user_id, p.category_id, p.title, p.content, p.status,
                       p.created_at, p.updated_at,
                       c.name AS category_name, c.code AS category_code,
                       u.nickname AS author_nickname, u.avatar_url AS author_avatar_url, u.role AS author_role,
                       ps.view_count, ps.like_count, ps.comment_count, ps.hot_score
                FROM post p
                JOIN category c ON p.category_id = c.id
                JOIN `user` u ON p.user_id = u.id
                JOIN post_stat ps ON p.id = ps.post_id
                WHERE p.id = ?
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new PostDetail(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getLong("category_id"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getInt("status"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime(),
                rs.getString("category_name"),
                rs.getString("category_code"),
                rs.getString("author_nickname"),
                rs.getString("author_avatar_url"),
                rs.getInt("author_role"),
                rs.getInt("view_count"),
                rs.getInt("like_count"),
                rs.getInt("comment_count"),
                rs.getDouble("hot_score")
        ), postId).stream().findFirst();
    }

    public void increaseViewCount(Long postId) {
        String sql = """
                UPDATE post_stat
                SET view_count = view_count + 1,
                    hot_score = hot_score + 1
                WHERE post_id = ?
                """;
        jdbcTemplate.update(sql, postId);
    }

    public PageQueryResult<PostListItem> findPosts(int page, int size, Long categoryId, String keyword, String sort) {
        List<Object> params = new ArrayList<>();
        String whereClause = buildPostWhereClause(params, categoryId, keyword, null);
        long total = countPosts(whereClause, params);

        String orderBy = "hot".equalsIgnoreCase(sort) ? "ps.hot_score DESC, p.created_at DESC" : "p.created_at DESC";
        String sql = buildListSql(whereClause, orderBy);
        List<Object> queryParams = new ArrayList<>(params);
        queryParams.add(size);
        queryParams.add((page - 1) * size);

        List<PostListItem> records = jdbcTemplate.query(sql, this::mapPostListItem, queryParams.toArray());
        return new PageQueryResult<>(total, records);
    }

    public PageQueryResult<PostListItem> findPostsByUserId(Long userId, int page, int size) {
        List<Object> params = new ArrayList<>();
        String whereClause = buildPostWhereClause(params, null, null, userId);
        long total = countPosts(whereClause, params);

        String sql = buildListSql(whereClause, "p.created_at DESC");
        List<Object> queryParams = new ArrayList<>(params);
        queryParams.add(size);
        queryParams.add((page - 1) * size);

        List<PostListItem> records = jdbcTemplate.query(sql, this::mapPostListItem, queryParams.toArray());
        return new PageQueryResult<>(total, records);
    }

    public List<PostListItem> findHotPosts(int limit, Long categoryId) {
        List<Object> params = new ArrayList<>();
        String whereClause = buildPostWhereClause(params, categoryId, null, null);
        String sql = buildListSql(whereClause, "ps.hot_score DESC, p.created_at DESC");
        params.add(limit);
        params.add(0);
        return jdbcTemplate.query(sql, this::mapPostListItem, params.toArray());
    }

    public void updatePost(Long postId, UpdatePostRequest request) {
        String sql = """
                UPDATE post
                SET category_id = ?, title = ?, content = ?
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, request.categoryId(), request.title().trim(), request.content().trim(), postId);
    }

    public void softDeletePost(Long postId) {
        String sql = "UPDATE post SET status = 1 WHERE id = ?";
        jdbcTemplate.update(sql, postId);
    }

    public boolean existsLike(Long postId, Long userId) {
        String sql = "SELECT COUNT(*) FROM post_like WHERE post_id = ? AND user_id = ? AND status = 0";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, postId, userId);
        return count != null && count > 0;
    }

    private long countPosts(String whereClause, List<Object> params) {
        String sql = """
                SELECT COUNT(*)
                FROM post p
                JOIN category c ON p.category_id = c.id
                JOIN `user` u ON p.user_id = u.id
                JOIN post_stat ps ON p.id = ps.post_id
                """ + whereClause;
        Long total = jdbcTemplate.queryForObject(sql, Long.class, params.toArray());
        return total == null ? 0 : total;
    }

    private String buildListSql(String whereClause, String orderBy) {
        return """
                SELECT p.id, p.title, p.content, p.category_id,
                       c.name AS category_name, c.code AS category_code,
                       u.id AS author_id, u.nickname AS author_nickname, u.avatar_url AS author_avatar_url,
                       ps.view_count, ps.like_count, ps.comment_count, ps.hot_score,
                       p.created_at
                FROM post p
                JOIN category c ON p.category_id = c.id
                JOIN `user` u ON p.user_id = u.id
                JOIN post_stat ps ON p.id = ps.post_id
                """ + whereClause + " ORDER BY " + orderBy + " LIMIT ? OFFSET ?";
    }

    private String buildPostWhereClause(List<Object> params, Long categoryId, String keyword, Long userId) {
        StringBuilder where = new StringBuilder(" WHERE p.status = 0");

        if (categoryId != null) {
            where.append(" AND p.category_id = ?");
            params.add(categoryId);
        }

        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (p.title LIKE ? OR p.content LIKE ?)");
            String likeKeyword = "%" + keyword.trim() + "%";
            params.add(likeKeyword);
            params.add(likeKeyword);
        }

        if (userId != null) {
            where.append(" AND p.user_id = ?");
            params.add(userId);
        }

        return where.toString();
    }

    private PostListItem mapPostListItem(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new PostListItem(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getLong("category_id"),
                rs.getString("category_name"),
                rs.getString("category_code"),
                rs.getLong("author_id"),
                rs.getString("author_nickname"),
                rs.getString("author_avatar_url"),
                rs.getInt("view_count"),
                rs.getInt("like_count"),
                rs.getInt("comment_count"),
                rs.getDouble("hot_score"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
