package ru.vinyl.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public class WalletRepository {

    private final JdbcTemplate jdbc;

    public WalletRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void ensureBuyerProfile(long userId) {
        jdbc.update(
                """
                INSERT INTO buyer_profiles (user_id, wallet_balance)
                VALUES (?, 0)
                ON CONFLICT (user_id) DO NOTHING
                """,
                userId
        );
    }

    public void ensureSellerProfile(long userId) {
        jdbc.update(
                """
                INSERT INTO seller_profiles (user_id, wallet_balance)
                VALUES (?, 0)
                ON CONFLICT (user_id) DO NOTHING
                """,
                userId
        );
    }

    public void ensureAllBuyerProfiles() {
        jdbc.update(
                """
                INSERT INTO buyer_profiles (user_id, wallet_balance)
                SELECT id, 0 FROM users WHERE role = 'buyer'
                ON CONFLICT (user_id) DO NOTHING
                """
        );
    }

    public BigDecimal getBuyerBalance(long userId) {
        ensureBuyerProfile(userId);
        return jdbc.queryForObject(
                "SELECT wallet_balance FROM buyer_profiles WHERE user_id = ?",
                BigDecimal.class,
                userId
        );
    }

    public BigDecimal topUpBuyer(long userId, BigDecimal amount) {
        ensureBuyerProfile(userId);
        return jdbc.queryForObject(
                """
                UPDATE buyer_profiles
                SET wallet_balance = wallet_balance + ?
                WHERE user_id = ?
                RETURNING wallet_balance
                """,
                BigDecimal.class,
                amount,
                userId
        );
    }

    public Optional<BigDecimal> deductBuyer(long userId, BigDecimal amount) {
        ensureBuyerProfile(userId);
        BigDecimal balance = jdbc.queryForObject(
                "SELECT wallet_balance FROM buyer_profiles WHERE user_id = ? FOR UPDATE",
                BigDecimal.class,
                userId
        );
        if (balance.compareTo(amount) < 0) {
            return Optional.empty();
        }
        jdbc.update(
                "UPDATE buyer_profiles SET wallet_balance = wallet_balance - ? WHERE user_id = ?",
                amount,
                userId
        );
        return Optional.of(balance.subtract(amount));
    }

    public BigDecimal getSellerBalance(long userId) {
        ensureSellerProfile(userId);
        BigDecimal balance = jdbc.queryForObject(
                "SELECT wallet_balance FROM seller_profiles WHERE user_id = ?",
                BigDecimal.class,
                userId
        );
        return balance != null ? balance : BigDecimal.ZERO;
    }

    public void refreshSellerBalances() {
        jdbc.update("UPDATE seller_profiles SET wallet_balance = 0");
        jdbc.update(
                """
                UPDATE seller_profiles sp
                SET wallet_balance = seller_totals.total_revenue
                FROM (
                    SELECT oi.seller_id, COALESCE(SUM(oi.quantity * oi.price_snapshot), 0) AS total_revenue
                    FROM order_items oi
                    JOIN orders o ON o.id = oi.order_id
                    WHERE o.status = 'completed'
                    GROUP BY oi.seller_id
                ) AS seller_totals
                WHERE sp.user_id = seller_totals.seller_id
                """
        );
    }
}
