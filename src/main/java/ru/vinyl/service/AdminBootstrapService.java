package ru.vinyl.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vinyl.config.VinylProperties;
import ru.vinyl.domain.User;
import ru.vinyl.domain.UserRole;
import ru.vinyl.repository.UserRepository;
import ru.vinyl.repository.WalletRepository;

@Service
public class AdminBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapService.class);

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final AuthService authService;
    private final TokenService tokenService;
    private final MailService mailService;
    private final VinylProperties properties;

    public AdminBootstrapService(
            UserRepository userRepository,
            WalletRepository walletRepository,
            AuthService authService,
            TokenService tokenService,
            MailService mailService,
            VinylProperties properties
    ) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.authService = authService;
        this.tokenService = tokenService;
        this.mailService = mailService;
        this.properties = properties;
    }

    public void ensureBuyerProfiles() {
        walletRepository.ensureAllBuyerProfiles();
    }

    @Transactional
    public void ensureAdminAccount() {
        VinylProperties.Admin adminProps = properties.getAdmin();
        var existing = userRepository.findByEmail(adminProps.getEmail());
        if (existing.isPresent()) {
            User admin = existing.get();
            if (!admin.isEmailConfirmed()) {
                sendAdminVerification(admin);
                log.info(
                        "Администратор {} ожидает подтверждения e-mail — ссылка в консоли выше.",
                        admin.getEmail()
                );
            }
            return;
        }

        User admin = new User();
        admin.setRole(UserRole.ADMIN);
        admin.setFirstName(adminProps.getFirstName());
        admin.setLastName(adminProps.getLastName());
        admin.setEmail(adminProps.getEmail().toLowerCase().trim());
        admin.setPasswordHash(authService.hashPassword(adminProps.getPassword()));
        long adminId = userRepository.insert(admin);
        admin.setId(adminId);

        sendAdminVerification(admin);
        log.info(
                "Создан администратор {}. Пароль: {}. Подтвердите e-mail по ссылке в консоли, затем войдите с ролью «Администратор».",
                adminProps.getEmail(),
                adminProps.getPassword()
        );
    }

    private void sendAdminVerification(User admin) {
        String token = tokenService.issueEmailVerificationToken(admin.getId());
        String url = properties.getBaseUrl() + "/confirm-email/" + token;
        mailService.sendVerificationEmail(admin, url, "Подтверждение почты администратора");
    }
}
