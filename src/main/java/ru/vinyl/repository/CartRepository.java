package ru.vinyl.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.vinyl.domain.CartItem;

import java.util.List;

@Repository
public class CartRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<CartItem> mapper = (rs, rowNum) -> {
        CartItem item = new CartItem();
        item.setProductId(rs.getLong("product_id"));
        item.setQuantity(rs.getInt("quantity"));
        item.setTitle(rs.getString("title"));
        item.setArtist(rs.getString("artist"));
        item.setPrice(rs.getBigDecimal("price"));
        item.setCoverUrl(rs.getString("cover_url"));
        item.setStock(rs.getInt("stock"));
        return item;
    };

    public CartRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CartItem> findByUser(long userId) {
        return jdbc.query(
                """
                SELECT ci.product_id, ci.quantity, p.title, p.artist, p.price, p.cover_url, p.stock
                FROM cart_items ci
                JOIN products p ON p.id = ci.product_id
                WHERE ci.user_id = ?
                ORDER BY ci.created_at DESC
                """,
                mapper,
                userId
        );
    }

    public int countItems(long userId) {
        Integer count = jdbc.queryForObject(
                "SELECT COALESCE(SUM(quantity), 0) FROM cart_items WHERE user_id = ?",
                Integer.class,
                userId
        );
        return count == null ? 0 : count;
    }

    public void addOrIncrease(long userId, long productId, int quantity) {
        jdbc.update(
                """
                INSERT INTO cart_items (user_id, product_id, quantity)
                VALUES (?, ?, ?)
                ON CONFLICT (user_id, product_id)
                DO UPDATE SET quantity = cart_items.quantity + EXCLUDED.quantity, updated_at = NOW()
                """,
                userId,
                productId,
                quantity
        );
    }

    public void updateQuantity(long userId, long productId, int quantity) {
        jdbc.update(
                "UPDATE cart_items SET quantity = ?, updated_at = NOW() WHERE user_id = ? AND product_id = ?",
                quantity,
                userId,
                productId
        );
    }

    public void remove(long userId, long productId) {
        jdbc.update("DELETE FROM cart_items WHERE user_id = ? AND product_id = ?", userId, productId);
    }

    public void clear(long userId) {
        jdbc.update("DELETE FROM cart_items WHERE user_id = ?", userId);
    }
}
