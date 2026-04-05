package ru.vinyl.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.vinyl.domain.Order;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class OrderRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<Order> mapper = (rs, rowNum) -> {
        Order order = new Order();
        order.setId(rs.getLong("id"));
        order.setBuyerId(rs.getLong("buyer_id"));
        order.setStatus(rs.getString("status"));
        order.setTotalAmount(rs.getBigDecimal("total_amount"));
        order.setPaymentStatus(rs.getString("payment_status"));
        order.setCancellationReason(rs.getString("cancellation_reason"));
        order.setCreatedAt(JdbcSupport.getOffsetDateTime(rs, "created_at"));
        order.setUpdatedAt(JdbcSupport.getOffsetDateTime(rs, "updated_at"));
        try {
            order.setBuyerFirstName(rs.getString("first_name"));
            order.setBuyerLastName(rs.getString("last_name"));
            order.setBuyerEmail(rs.getString("email"));
        } catch (java.sql.SQLException ignored) {
        }
        return order;
    };

    public OrderRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Order> findActiveByBuyer(long buyerId) {
        return jdbc.query(
                """
                SELECT id, buyer_id, status, total_amount, payment_status, cancellation_reason, created_at, updated_at
                FROM orders
                WHERE buyer_id = ? AND status NOT IN ('completed', 'cancelled')
                ORDER BY created_at DESC
                """,
                mapper,
                buyerId
        );
    }

    public List<Order> findHistoryByBuyer(long buyerId) {
        return jdbc.query(
                """
                SELECT id, buyer_id, status, total_amount, payment_status, cancellation_reason, created_at, updated_at
                FROM orders
                WHERE buyer_id = ? AND status IN ('completed', 'cancelled')
                ORDER BY created_at DESC
                """,
                mapper,
                buyerId
        );
    }

    public List<Order> findAll() {
        return jdbc.query(
                """
                SELECT o.id, o.buyer_id, o.status, o.total_amount, o.payment_status, o.cancellation_reason,
                       o.created_at, o.updated_at,
                       u.first_name, u.last_name, u.email
                FROM orders o
                JOIN users u ON u.id = o.buyer_id
                ORDER BY o.created_at DESC
                """,
                mapper
        );
    }

    public java.util.Optional<Order> findById(long orderId) {
        return jdbc.query(
                """
                SELECT o.id, o.buyer_id, o.status, o.total_amount, o.payment_status, o.cancellation_reason,
                       o.created_at, o.updated_at,
                       u.first_name, u.last_name, u.email
                FROM orders o
                JOIN users u ON u.id = o.buyer_id
                WHERE o.id = ?
                """,
                mapper,
                orderId
        ).stream().findFirst();
    }

    public long create(long buyerId, BigDecimal total) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO orders (buyer_id, status, total_amount, payment_status)
                    VALUES (?, 'new', ?, 'paid')
                    """,
                    new String[]{"id"}
            );
            ps.setLong(1, buyerId);
            ps.setBigDecimal(2, total);
            return ps;
        }, keyHolder);
        return JdbcSupport.extractGeneratedId(keyHolder);
    }

    public void addItem(long orderId, Long productId, Long sellerId, String title, String artist,
                        BigDecimal price, int quantity) {
        jdbc.update(
                """
                INSERT INTO order_items (
                    order_id, product_id, seller_id, title_snapshot, artist_snapshot, price_snapshot, quantity
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                orderId,
                productId,
                sellerId,
                title,
                artist,
                price,
                quantity
        );
    }

    public void updateStatus(long orderId, String status, String cancellationReason) {
        jdbc.update(
                """
                UPDATE orders
                SET status = ?, cancellation_reason = ?, updated_at = NOW()
                WHERE id = ?
                """,
                status,
                cancellationReason,
                orderId
        );
    }

    public void updatePaymentStatus(long orderId, String paymentStatus) {
        jdbc.update(
                "UPDATE orders SET payment_status = ?, updated_at = NOW() WHERE id = ?",
                paymentStatus,
                orderId
        );
    }

    public List<OrderItemRow> findItemsByOrderId(long orderId) {
        return jdbc.query(
                """
                SELECT product_id, quantity
                FROM order_items
                WHERE order_id = ?
                """,
                (rs, rowNum) -> new OrderItemRow(
                        rs.getObject("product_id") != null ? rs.getLong("product_id") : null,
                        rs.getInt("quantity")
                ),
                orderId
        );
    }

    public OrderStats countStats() {
        return jdbc.queryForObject(
                """
                SELECT COUNT(*) AS order_count,
                       COALESCE(SUM(total_amount) FILTER (WHERE status = 'completed'), 0) AS revenue
                FROM orders
                """,
                (rs, rowNum) -> new OrderStats(rs.getInt("order_count"), rs.getBigDecimal("revenue"))
        );
    }

    public BuyerOrderStats buyerStats(long buyerId) {
        return jdbc.queryForObject(
                """
                SELECT COALESCE(SUM(total_amount), 0) AS total_spent,
                       COUNT(*) FILTER (WHERE status = 'completed') AS completed_orders
                FROM orders WHERE buyer_id = ?
                """,
                (rs, rowNum) -> new BuyerOrderStats(rs.getBigDecimal("total_spent"), rs.getInt("completed_orders")),
                buyerId
        );
    }

    public record OrderStats(int orderCount, BigDecimal revenue) {
    }

    public record OrderItemRow(Long productId, int quantity) {
    }

    public record BuyerOrderStats(BigDecimal totalSpent, int completedOrders) {
    }
}
