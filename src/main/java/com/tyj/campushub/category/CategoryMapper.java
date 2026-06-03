package com.tyj.campushub.category;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CategoryMapper {

    private final JdbcClient jdbcClient;

    public CategoryMapper(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<Category> findEnabledCategories() {
        String sql = """
                SELECT id, name, code, sort_order, status, created_at, updated_at
                FROM category
                WHERE status = 0
                ORDER BY sort_order ASC, id ASC
                """;

        return jdbcClient.sql(sql)
                .query(Category.class)
                .list();
    }
}
