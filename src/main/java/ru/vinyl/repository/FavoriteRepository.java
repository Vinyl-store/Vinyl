package ru.vinyl.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.vinyl.domain.Product;

import java.util.List;

@Repository
public class FavoriteRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<Product> mapper = (rs, rowNum) -> {
        Product product = new Product();
        product.setId(rs.getLong("id"));
        product.setTitle(rs.getString("title"));
        product.setArtist(rs.getString("artist"));
        product.setPrice(rs.getBigDecimal("price"));
        product.setRating(rs.getBigDecimal("rating"));
        product.setCoverUrl(rs.getString("cover_url"));
        return product;
    };

    public FavoriteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int countByUser(long userId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM favorites WHERE user_id = ?",
                Integer.class,
                userId
        );
        return count == null ? 0 : count;
    }

    public List<Product> findByUser(long userId) {
        return jdbc.query(
                """
                SELECT p.id, p.title, p.artist, p.price, p.rating, p.cover_url
                FROM favorites f
                JOIN products p ON p.id = f.product_id
                WHERE f.user_id = ?
                ORDER BY f.created_at DESC
                """,
                mapper,
                userId
        );
    }

    public void add(long userId, long productId) {
        jdbc.update(
                """
                INSERT INTO favorites (user_id, product_id)
                VALUES (?, ?)
                ON CONFLICT (user_id, product_id) DO NOTHING
                """,
                userId,
                productId
        );
    }

    public void remove(long userId, long productId) {
        jdbc.update("DELETE FROM favorites WHERE user_id = ? AND product_id = ?", userId, productId);
    }
}
