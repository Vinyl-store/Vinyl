package ru.vinyl.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.vinyl.domain.User;
import ru.vinyl.domain.UserRole;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<User> mapper = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setRole(UserRole.fromCode(rs.getString("role")));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setPhone(rs.getString("phone"));
        user.setEmailConfirmed(rs.getBoolean("is_email_confirmed"));
        user.setBlocked(rs.getBoolean("is_blocked"));
        user.setCreatedAt(JdbcSupport.getOffsetDateTime(rs, "created_at"));
        user.setUpdatedAt(JdbcSupport.getOffsetDateTime(rs, "updated_at"));
        return user;
    };

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<User> findById(long id) {
        List<User> users = jdbc.query(
                """
                SELECT id, role, first_name, last_name, email, password_hash, phone,
                       is_email_confirmed, is_blocked, created_at, updated_at
                FROM users WHERE id = ?
                """,
                mapper,
                id
        );
        return users.stream().findFirst();
    }

    public Optional<User> findByEmail(String email) {
        List<User> users = jdbc.query(
                """
                SELECT id, role, first_name, last_name, email, password_hash, phone,
                       is_email_confirmed, is_blocked, created_at, updated_at
                FROM users WHERE email = ?
                """,
                mapper,
                email.toLowerCase().trim()
        );
        return users.stream().findFirst();
    }

    public Optional<User> findByEmailAndRole(String email, UserRole role) {
        List<User> users = jdbc.query(
                """
                SELECT id, role, first_name, last_name, email, password_hash, phone,
                       is_email_confirmed, is_blocked, created_at, updated_at
                FROM users WHERE email = ? AND role = ?
                """,
                mapper,
                email.toLowerCase().trim(),
                role.getCode()
        );
        return users.stream().findFirst();
    }

    public boolean existsByEmail(String email) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?",
                Integer.class,
                email.toLowerCase().trim()
        );
        return count != null && count > 0;
    }

    public long insert(User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO users (role, first_name, last_name, email, password_hash, phone)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    new String[]{"id"}
            );
            ps.setString(1, user.getRole().getCode());
            ps.setString(2, user.getFirstName());
            ps.setString(3, user.getLastName());
            ps.setString(4, user.getEmail().toLowerCase().trim());
            ps.setString(5, user.getPasswordHash());
            ps.setString(6, user.getPhone());
            return ps;
        }, keyHolder);
        return JdbcSupport.extractGeneratedId(keyHolder);
    }

    public void confirmEmail(long userId) {
        jdbc.update("UPDATE users SET is_email_confirmed = TRUE, updated_at = NOW() WHERE id = ?", userId);
    }

    public int confirmAllUnconfirmedEmails() {
        return jdbc.update(
                "UPDATE users SET is_email_confirmed = TRUE, updated_at = NOW() WHERE is_email_confirmed = FALSE"
        );
    }

    public void updatePassword(long userId, String passwordHash) {
        jdbc.update(
                "UPDATE users SET password_hash = ?, updated_at = NOW() WHERE id = ?",
                passwordHash,
                userId
        );
    }

    public void toggleBlock(long userId, UserRole role) {
        jdbc.update(
                """
                UPDATE users
                SET is_blocked = NOT is_blocked, updated_at = NOW()
                WHERE id = ? AND role = ?
                """,
                userId,
                role.getCode()
        );
    }

    public void updateBuyer(long userId, String firstName, String lastName, String phone) {
        jdbc.update(
                """
                UPDATE users
                SET first_name = ?, last_name = ?, phone = ?, updated_at = NOW()
                WHERE id = ? AND role = 'buyer'
                """,
                firstName,
                lastName,
                phone,
                userId
        );
    }

    public List<User> findBuyers() {
        return jdbc.query(
                """
                SELECT u.id, u.role, u.first_name, u.last_name, u.email, u.password_hash, u.phone,
                       u.is_email_confirmed, u.is_blocked, u.created_at, u.updated_at
                FROM users u
                WHERE u.role = 'buyer'
                ORDER BY u.created_at DESC
                """,
                mapper
        );
    }

    public List<User> findSellers() {
        return jdbc.query(
                """
                SELECT u.id, u.role, u.first_name, u.last_name, u.email, u.password_hash, u.phone,
                       u.is_email_confirmed, u.is_blocked, u.created_at, u.updated_at
                FROM users u
                WHERE u.role = 'seller'
                ORDER BY u.created_at DESC
                """,
                mapper
        );
    }

    public Optional<User> findSellerById(long sellerId) {
        List<User> users = jdbc.query(
                """
                SELECT id, role, first_name, last_name, email, password_hash, phone,
                       is_email_confirmed, is_blocked, created_at, updated_at
                FROM users WHERE id = ? AND role = 'seller'
                """,
                mapper,
                sellerId
        );
        return users.stream().findFirst();
    }

    public UserCounts countByRole() {
        return jdbc.queryForObject(
                """
                SELECT
                    COUNT(*) FILTER (WHERE role = 'buyer') AS buyer_count,
                    COUNT(*) FILTER (WHERE role = 'seller') AS seller_count,
                    COUNT(*) FILTER (WHERE role = 'admin') AS admin_count
                FROM users
                """,
                (rs, rowNum) -> new UserCounts(
                        rs.getInt("buyer_count"),
                        rs.getInt("seller_count"),
                        rs.getInt("admin_count")
                )
        );
    }

    public record UserCounts(int buyerCount, int sellerCount, int adminCount) {
    }
}
