package com.tyj.campushub.notice;

import com.tyj.campushub.post.PageQueryResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class NoticeMapper {

    private final JdbcTemplate jdbcTemplate;

    public NoticeMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveNotice(CreateNoticeCommand command) {
        String sql = """
                INSERT INTO notice (receiver_id, sender_id, post_id, comment_id, type, content)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(
                sql,
                command.receiverId(),
                command.senderId(),
                command.postId(),
                command.commentId(),
                command.type(),
                command.content()
        );
    }

    public PageQueryResult<NoticeItem> findNoticesByReceiverId(Long receiverId, int page, int size, Integer readStatus) {
        List<Object> params = new ArrayList<>();
        String whereClause = buildWhereClause(params, receiverId, readStatus);

        String countSql = "SELECT COUNT(*) FROM notice n" + whereClause;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());

        String sql = """
                SELECT n.id, n.receiver_id, n.sender_id,
                       u.nickname AS sender_nickname, u.avatar_url AS sender_avatar_url,
                       n.post_id, n.comment_id, n.type, n.content, n.read_status, n.created_at
                FROM notice n
                JOIN `user` u ON n.sender_id = u.id
                """ + whereClause + " ORDER BY n.created_at DESC, n.id DESC LIMIT ? OFFSET ?";

        List<Object> queryParams = new ArrayList<>(params);
        queryParams.add(size);
        queryParams.add((page - 1) * size);

        List<NoticeItem> records = jdbcTemplate.query(sql, (rs, rowNum) -> new NoticeItem(
                rs.getLong("id"),
                rs.getLong("receiver_id"),
                rs.getLong("sender_id"),
                rs.getString("sender_nickname"),
                rs.getString("sender_avatar_url"),
                rs.getObject("post_id", Long.class),
                rs.getObject("comment_id", Long.class),
                rs.getInt("type"),
                rs.getString("content"),
                rs.getInt("read_status"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), queryParams.toArray());

        return new PageQueryResult<>(total == null ? 0 : total, records);
    }

    public long countUnreadByReceiverId(Long receiverId) {
        String sql = "SELECT COUNT(*) FROM notice WHERE receiver_id = ? AND read_status = 0";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, receiverId);
        return count == null ? 0 : count;
    }

    public int markRead(Long noticeId, Long receiverId) {
        String sql = "UPDATE notice SET read_status = 1 WHERE id = ? AND receiver_id = ? AND read_status = 0";
        return jdbcTemplate.update(sql, noticeId, receiverId);
    }

    public int markAllRead(Long receiverId) {
        String sql = "UPDATE notice SET read_status = 1 WHERE receiver_id = ? AND read_status = 0";
        return jdbcTemplate.update(sql, receiverId);
    }

    public boolean existsByIdAndReceiverId(Long noticeId, Long receiverId) {
        String sql = "SELECT COUNT(*) FROM notice WHERE id = ? AND receiver_id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, noticeId, receiverId);
        return count != null && count > 0;
    }

    private String buildWhereClause(List<Object> params, Long receiverId, Integer readStatus) {
        StringBuilder where = new StringBuilder(" WHERE n.receiver_id = ?");
        params.add(receiverId);

        if (readStatus != null) {
            where.append(" AND n.read_status = ?");
            params.add(readStatus);
        }

        return where.toString();
    }
}
