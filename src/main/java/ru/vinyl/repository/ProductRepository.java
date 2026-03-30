package ru.vinyl.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.vinyl.domain.Product;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

@Repository
public class ProductRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<Product> mapper = (rs, rowNum) -> mapProduct(rs);

    public ProductRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private Product mapProduct(java.sql.ResultSet rs) throws java.sql.SQLException {
        Product product = new Product();
        product.setId(rs.getLong("id"));
        product.setSellerId(rs.getLong("seller_id"));
        product.setTitle(rs.getString("title"));
        product.setArtist(rs.getString("artist"));
        product.setGenre(rs.getString("genre"));
        product.setPrice(rs.getBigDecimal("price"));
        product.setRating(rs.getBigDecimal("rating"));
        product.setDescription(rs.getString("description"));
        int releaseYear = rs.getInt("release_year");
        product.setReleaseYear(rs.wasNull() ? null : releaseYear);
        product.setStock(rs.getInt("stock"));
        product.setCoverUrl(rs.getString("cover_url"));
        product.setPublished(rs.getBoolean("is_published"));
        product.setFeatured(rs.getBoolean("is_featured"));
        int featuredPos = rs.getInt("featured_position");
        product.setFeaturedPosition(rs.wasNull() ? null : featuredPos);
        product.setSale(rs.getBoolean("is_sale"));
        int salePos = rs.getInt("sale_position");
        product.setSalePosition(rs.wasNull() ? null : salePos);
        product.setCreatedAt(JdbcSupport.getOffsetDateTime(rs, "created_at"));
        product.setUpdatedAt(JdbcSupport.getOffsetDateTime(rs, "updated_at"));
        try {
            product.setSellerFirstName(rs.getString("seller_first_name"));
            product.setSellerLastName(rs.getString("seller_last_name"));
        } catch (java.sql.SQLException ignored) {
        }
        return product;
    }

    public Optional<Product> findById(long id) {
        return jdbc.query(
                "SELECT * FROM products WHERE id = ?",
                mapper,
                id
        ).stream().findFirst();
    }

    public Optional<Product> findPublishedById(long id) {
        return jdbc.query(
                "SELECT * FROM products WHERE id = ? AND is_published = TRUE",
                mapper,
                id
        ).stream().findFirst();
    }

    public Optional<Product> findByIdWithSeller(long id) {
        return jdbc.query(
                """
                SELECT p.*, u.first_name AS seller_first_name, u.last_name AS seller_last_name
                FROM products p
                JOIN users u ON u.id = p.seller_id
                WHERE p.id = ?
                """,
                mapper,
                id
        ).stream().findFirst();
    }

    public List<Product> findFeatured(int limit) {
        return jdbc.query(
                """
                SELECT * FROM products
                WHERE is_published = TRUE
                ORDER BY featured_position NULLS LAST, created_at DESC
                LIMIT ?
                """,
                mapper,
                limit
        );
    }

    public List<Product> findSaleItems(int limit) {
        return jdbc.query(
                """
                SELECT * FROM products
                WHERE is_published = TRUE AND is_sale = TRUE
                ORDER BY sale_position NULLS LAST, created_at DESC
                LIMIT ?
                """,
                mapper,
                limit
        );
    }

    public int countPublished(String searchQuery) {
        String query = searchQuery == null ? "" : searchQuery.trim();
        Integer count = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM products
                WHERE is_published = TRUE AND (? = '' OR title ILIKE ?)
                """,
                Integer.class,
                query,
                "%" + query + "%"
        );
        return count == null ? 0 : count;
    }

    public List<Product> findCatalogPage(String searchQuery, int limit, int offset) {
        String query = searchQuery == null ? "" : searchQuery.trim();
        return jdbc.query(
                """
                SELECT id, seller_id, title, artist, genre, price, rating, description, release_year,
                       stock, cover_url, is_published, is_featured, featured_position, is_sale,
                       sale_position, created_at, updated_at
                FROM products
                WHERE is_published = TRUE AND (? = '' OR title ILIKE ?)
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """,
                mapper,
                query,
                "%" + query + "%",
                limit,
                offset
        );
    }

    public List<Product> findBySeller(long sellerId) {
        return jdbc.query(
                """
                SELECT * FROM products WHERE seller_id = ?
                ORDER BY created_at DESC
                """,
                mapper,
                sellerId
        );
    }

    public List<Product> findAllWithSeller() {
        return jdbc.query(
                """
                SELECT p.*, u.first_name AS seller_first_name, u.last_name AS seller_last_name
                FROM products p
                JOIN users u ON u.id = p.seller_id
                ORDER BY p.created_at DESC
                """,
                mapper
        );
    }

    public long insert(Product product) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO products (
                        seller_id, title, artist, genre, price, description, release_year,
                        stock, cover_url, is_published, is_featured, featured_position, is_sale, sale_position
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    new String[]{"id"}
            );
            int i = 1;
            ps.setLong(i++, product.getSellerId());
            ps.setString(i++, product.getTitle());
            ps.setString(i++, product.getArtist());
            ps.setString(i++, product.getGenre());
            ps.setBigDecimal(i++, product.getPrice());
            ps.setString(i++, product.getDescription());
            if (product.getReleaseYear() == null) {
                ps.setNull(i++, Types.INTEGER);
            } else {
                ps.setInt(i++, product.getReleaseYear());
            }
            ps.setInt(i++, product.getStock());
            ps.setString(i++, product.getCoverUrl());
            ps.setBoolean(i++, product.isPublished());
            ps.setBoolean(i++, product.isFeatured());
            if (product.getFeaturedPosition() == null) {
                ps.setNull(i++, Types.INTEGER);
            } else {
                ps.setInt(i++, product.getFeaturedPosition());
            }
            ps.setBoolean(i++, product.isSale());
            if (product.getSalePosition() == null) {
                ps.setNull(i, Types.INTEGER);
            } else {
                ps.setInt(i, product.getSalePosition());
            }
            return ps;
        }, keyHolder);
        return JdbcSupport.extractGeneratedId(keyHolder);
    }

    public void update(Product product) {
        jdbc.update(
                """
                UPDATE products
                SET title = ?, artist = ?, genre = ?, price = ?, description = ?,
                    release_year = ?, stock = ?, cover_url = ?, is_published = ?,
                    is_featured = ?, featured_position = ?, is_sale = ?, sale_position = ?,
                    updated_at = NOW()
                WHERE id = ?
                """,
                product.getTitle(),
                product.getArtist(),
                product.getGenre(),
                product.getPrice(),
                product.getDescription(),
                product.getReleaseYear(),
                product.getStock(),
                product.getCoverUrl(),
                product.isPublished(),
                product.isFeatured(),
                product.getFeaturedPosition(),
                product.isSale(),
                product.getSalePosition(),
                product.getId()
        );
    }

    public void updateBySeller(Product product, long sellerId) {
        jdbc.update(
                """
                UPDATE products
                SET title = ?, artist = ?, genre = ?, price = ?, description = ?,
                    release_year = ?, stock = ?, cover_url = ?, is_published = ?,
                    is_featured = ?, is_sale = ?, updated_at = NOW()
                WHERE id = ? AND seller_id = ?
                """,
                product.getTitle(),
                product.getArtist(),
                product.getGenre(),
                product.getPrice(),
                product.getDescription(),
                product.getReleaseYear(),
                product.getStock(),
                product.getCoverUrl(),
                product.isPublished(),
                product.isFeatured(),
                product.isSale(),
                product.getId(),
                sellerId
        );
    }

    public void decreaseStock(long productId, int quantity) {
        jdbc.update(
                "UPDATE products SET stock = stock - ?, updated_at = NOW() WHERE id = ?",
                quantity,
                productId
        );
    }

    public void increaseStock(long productId, int quantity) {
        jdbc.update(
                "UPDATE products SET stock = stock + ?, updated_at = NOW() WHERE id = ?",
                quantity,
                productId
        );
    }

    public void updateRating(long productId) {
        jdbc.update(
                """
                UPDATE products
                SET rating = (
                    SELECT COALESCE(ROUND(AVG(rating)::numeric, 1), 0)
                    FROM reviews WHERE product_id = ?
                )
                WHERE id = ?
                """,
                productId,
                productId
        );
    }

    public void delete(long productId) {
        jdbc.update("DELETE FROM products WHERE id = ?", productId);
    }

    public void deleteBySeller(long productId, long sellerId) {
        jdbc.update("DELETE FROM products WHERE id = ? AND seller_id = ?", productId, sellerId);
    }

    public ProductStats countStats() {
        return jdbc.queryForObject(
                """
                SELECT COUNT(*) AS product_count,
                       COUNT(*) FILTER (WHERE stock = 0) AS out_of_stock_count
                FROM products
                """,
                (rs, rowNum) -> new ProductStats(rs.getInt("product_count"), rs.getInt("out_of_stock_count"))
        );
    }

    public record ProductStats(int productCount, int outOfStockCount) {
    }
}
