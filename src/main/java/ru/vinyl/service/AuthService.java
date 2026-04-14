package ru.vinyl.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.vinyl.config.VinylProperties;
import ru.vinyl.domain.User;
import ru.vinyl.domain.UserRole;
import ru.vinyl.repository.LoginAttemptRepository;
import ru.vinyl.repository.UserRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final PasswordEncoder passwordEncoder;
    private final VinylProperties properties;

    public AuthService(
            UserRepository userRepository,
            LoginAttemptRepository loginAttemptRepository,
            PasswordEncoder passwordEncoder,
            VinylProperties properties
    ) {
        this.userRepository = userRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    public String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    public boolean verifyPassword(String hash, String password) {
        return passwordEncoder.matches(password, hash);
    }

    public Optional<User> findById(long id) {
        return userRepository.findById(id);
    }

    public Optional<User> authenticate(String email, UserRole role, String password) {
        return userRepository.findByEmailAndRole(email, role)
                .filter(user -> verifyPassword(user.getPasswordHash(), password));
    }

    public void recordLoginAttempt(String email, UserRole role, boolean success, String ip) {
        loginAttemptRepository.record(email, role.getCode(), success, ip);
    }

    public Optional<LockoutInfo> getLockout(String email, UserRole role) {
        OffsetDateTime since = OffsetDateTime.now(ZoneOffset.UTC)
                .minusMinutes(properties.getLoginBlockMinutes());
        return loginAttemptRepository.failedAttemptsSince(email, role.getCode(), since)
                .filter(state -> state.failedCount() >= properties.getLoginAttemptLimit())
                .map(state -> {
                    OffsetDateTime unlock = state.lastFailedAt().plusMinutes(properties.getLoginBlockMinutes());
                    long remaining = Math.max(
                            (unlock.toEpochSecond() - OffsetDateTime.now(ZoneOffset.UTC).toEpochSecond()) / 60,
                            1
                    );
                    return new LockoutInfo(true, (int) remaining);
                });
    }

    public record LockoutInfo(boolean locked, int remainingMinutes) {
    }
}
