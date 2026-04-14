package ru.vinyl.service;

import org.springframework.stereotype.Service;
import ru.vinyl.config.VinylProperties;
import ru.vinyl.repository.TokenRepository;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;

@Service
public class TokenService {

    private final TokenRepository tokenRepository;
    private final VinylProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenService(TokenRepository tokenRepository, VinylProperties properties) {
        this.tokenRepository = tokenRepository;
        this.properties = properties;
    }

    public String issueEmailVerificationToken(long userId) {
        String token = generateToken();
        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC)
                .plusHours(properties.getEmailTokenTtlHours());
        return tokenRepository.createEmailToken(userId, token, expiresAt);
    }

    public String issuePasswordResetToken(long userId) {
        String token = generateToken();
        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC)
                .plusMinutes(properties.getResetTokenTtlMinutes());
        return tokenRepository.createResetToken(userId, token, expiresAt);
    }

    public Optional<TokenRepository.EmailVerification> findEmailToken(String token) {
        return tokenRepository.findEmailToken(token);
    }

    public Optional<TokenRepository.PasswordReset> findResetToken(String token, String role) {
        return tokenRepository.findResetToken(token, role);
    }

    public void markEmailTokenUsed(long id) {
        tokenRepository.markEmailTokenUsed(id);
    }

    public void markResetTokenUsed(long id) {
        tokenRepository.markResetTokenUsed(id);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
