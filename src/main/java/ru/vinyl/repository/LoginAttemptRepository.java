package ru.vinyl.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public class LoginAttemptRepository {

    private final JdbcTemplate jdbc;

    public LoginAttemptRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void record(String email, String role, boolean success, String ipAddress) {
        jdbc.update(
                "INSERT INTO login_attempts (email, role, is_success, ip_address) VALUES (?, ?, ?, ?)",
                email.toLowerCase().trim(),
                role,
                success,
                ipAddress
        );
    }

    public Optional<LockoutState> failedAttemptsSince(String email, String role, OffsetDateTime since) {
        return jdbc.query(
                """
                SELECT COUNT(*) AS failed_count, MAX(attempted_at) AS last_failed_at
                FROM login_attempts
                WHERE email = ? AND role = ? AND is_success = FALSE AND attempted_at >= ?
                """,
                (rs, rowNum) -> new LockoutState(rs.getInt("failed_count"), JdbcSupport.getOffsetDateTime(rs, "last_failed_at")),
                email.toLowerCase().trim(),
                role,
                since
        ).stream().findFirst();
    }

    public record LockoutState(int failedCount, OffsetDateTime lastFailedAt) {
    }
}
