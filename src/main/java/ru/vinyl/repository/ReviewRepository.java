package ru.vinyl.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.vinyl.domain.Review;

import java.util.List;

@Repository
public class ReviewRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<Review> mapper = (rs, rowNum) -> {
        Review review = new Review();
        review.setRating(rs.getInt("rating"));
        review.setReviewText(rs.getString("review_text"));
        review.setCreatedAt(JdbcSupport.getOffsetDateTime(rs, "created_at"));
        review.setFirstName(rs.getString("first_name"));
        review.setLastName(rs.getString("last_name"));
        return review;
    };

    public ReviewRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Review> findByProduct(long productId) {
        return jdbc.query(
                """
                SELECT r.rating, r.review_text, r.created_at, u.first_name, u.last_name
                FROM reviews r
                JOIN users u ON u.id = r.user_id
                WHERE r.product_id = ?
                ORDER BY r.created_at DESC
                """,
                mapper,
                productId
        );
    }

    public boolean hasCompletedPurchase(long userId, long productId) {
        Integer count = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM order_items oi
                JOIN orders o ON o.id = oi.order_id
                WHERE o.buyer_id = ? AND oi.product_id = ? AND o.status = 'completed'
                """,
                Integer.class,
                userId,
                productId
        );
        return count != null && count > 0;
    }

    public void upsert(long productId, long userId, int rating, String reviewText) {
        jdbc.update(
                """
                INSERT INTO reviews (product_id, user_id, rating, review_text)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (product_id, user_id)
                DO UPDATE SET rating = EXCLUDED.rating, review_text = EXCLUDED.review_text, created_at = NOW()
                """,
                productId,
                userId,
                rating,
                reviewText
        );
    }
}
