package ru.vinyl.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Repository
public class SellerStatsRepository {

    private final JdbcTemplate jdbc;

    public SellerStatsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public SellerStats getStats(long sellerId) {
        return jdbc.queryForObject(
                """
                SELECT
                    COALESCE(SUM(oi.quantity) FILTER (WHERE o.status = 'completed'), 0) AS sold_items,
                    COALESCE(SUM(oi.quantity * oi.price_snapshot) FILTER (WHERE o.status = 'completed'), 0) AS revenue
                FROM order_items oi
                JOIN orders o ON o.id = oi.order_id
                WHERE oi.seller_id = ?
                """,
                (rs, rowNum) -> new SellerStats(rs.getInt("sold_items"), rs.getBigDecimal("revenue")),
                sellerId
        );
    }

    public int countProducts(long sellerId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM products WHERE seller_id = ?",
                Integer.class,
                sellerId
        );
        return count == null ? 0 : count;
    }

    public record SellerStats(int soldItems, BigDecimal revenue) {
    }
}
