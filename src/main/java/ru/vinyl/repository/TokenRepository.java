package ru.vinyl.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

@Repository
public class TokenRepository {

    private final JdbcTemplate jdbc;

    public TokenRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String createEmailToken(long userId, String token, OffsetDateTime expiresAt) {
        jdbc.update(
                "UPDATE email_verifications SET used_at = NOW() WHERE user_id = ? AND used_at IS NULL",
                userId
        );
        jdbc.update(
                "INSERT INTO email_verifications (user_id, token, expires_at) VALUES (?, ?, ?)",
                userId,
                token,
                expiresAt
        );
        return token;
    }

    public Optional<EmailVerification> findEmailToken(String token) {
        return jdbc.query(
                """
                SELECT ev.id, ev.user_id, ev.expires_at, ev.used_at, u.email
                FROM email_verifications ev
                JOIN users u ON u.id = ev.user_id
                WHERE ev.token = ?
                """,
                (rs, rowNum) -> new EmailVerification(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        JdbcSupport.getOffsetDateTime(rs, "expires_at"),
                        rs.getTimestamp("used_at") != null,
                        rs.getString("email")
                ),
                token
        ).stream().findFirst();
    }

    public void markEmailTokenUsed(long id) {
        jdbc.update("UPDATE email_verifications SET used_at = NOW() WHERE id = ?", id);
    }

    public String createResetToken(long userId, String token, OffsetDateTime expiresAt) {
        jdbc.update(
                "UPDATE password_reset_tokens SET used_at = NOW() WHERE user_id = ? AND used_at IS NULL",
                userId
        );
        jdbc.update(
                "INSERT INTO password_reset_tokens (user_id, token, expires_at) VALUES (?, ?, ?)",
                userId,
                token,
                expiresAt
        );
        return token;
    }

    public Optional<PasswordReset> findResetToken(String token, String role) {
        return jdbc.query(
                """
                SELECT prt.id, prt.user_id, prt.expires_at, prt.used_at, u.email
                FROM password_reset_tokens prt
                JOIN users u ON u.id = prt.user_id
                WHERE prt.token = ? AND u.role = ?
                """,
                (rs, rowNum) -> new PasswordReset(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        JdbcSupport.getOffsetDateTime(rs, "expires_at"),
                        rs.getTimestamp("used_at") != null,
                        rs.getString("email")
                ),
                token,
                role
        ).stream().findFirst();
    }

    public void markResetTokenUsed(long id) {
        jdbc.update("UPDATE password_reset_tokens SET used_at = NOW() WHERE id = ?", id);
    }

    public record EmailVerification(long id, long userId, OffsetDateTime expiresAt, boolean used, String email) {
    }

    public record PasswordReset(long id, long userId, OffsetDateTime expiresAt, boolean used, String email) {
    }
}
