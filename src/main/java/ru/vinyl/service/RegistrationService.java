package ru.vinyl.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vinyl.config.VinylProperties;
import ru.vinyl.domain.User;
import ru.vinyl.domain.UserRole;
import ru.vinyl.repository.UserRepository;
import ru.vinyl.repository.WalletRepository;

import java.util.Map;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final AuthService authService;
    private final TokenService tokenService;
    private final MailService mailService;
    private final ValidationService validationService;
    private final VinylProperties properties;

    public RegistrationService(
            UserRepository userRepository,
            WalletRepository walletRepository,
            AuthService authService,
            TokenService tokenService,
            MailService mailService,
            ValidationService validationService,
            VinylProperties properties
    ) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.authService = authService;
        this.tokenService = tokenService;
        this.mailService = mailService;
        this.validationService = validationService;
        this.properties = properties;
    }

    @Transactional
    public RegistrationResult registerBuyer(Map<String, String> form, String password) {
        Map<String, String> errors = validationService.validateRequired(
                Map.of(
                        "firstName", form.getOrDefault("firstName", ""),
                        "lastName", form.getOrDefault("lastName", ""),
                        "email", form.getOrDefault("email", ""),
                        "phone", form.getOrDefault("phone", ""),
                        "password", password == null ? "" : password
                ),
                Map.of(
                        "firstName", "Имя",
                        "lastName", "Фамилия",
                        "email", "E-mail",
                        "phone", "Телефон",
                        "password", "Пароль"
                )
        );
        String emailError = validationService.validateEmail(form.getOrDefault("email", ""));
        if (emailError != null) {
            errors.put("email", emailError);
        }
        String phoneError = validationService.validatePhone(form.getOrDefault("phone", ""));
        if (phoneError != null) {
            errors.put("phone", phoneError);
        }
        String passwordError = validationService.validatePassword(password);
        if (passwordError != null) {
            errors.put("password", passwordError);
        }
        String email = form.getOrDefault("email", "").trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            errors.put("email", "Пользователь с таким e-mail уже существует.");
        }
        if (!errors.isEmpty()) {
            return RegistrationResult.failure(errors);
        }

        User user = new User();
        user.setRole(UserRole.BUYER);
        user.setFirstName(form.get("firstName").trim());
        user.setLastName(form.get("lastName").trim());
        user.setEmail(email);
        user.setPhone(form.get("phone").trim());
        user.setPasswordHash(authService.hashPassword(password));
        long userId = userRepository.insert(user);
        walletRepository.ensureBuyerProfile(userId);
        user.setId(userId);

        finishRegistration(user, "Подтверждение почты");
        return RegistrationResult.success(user);
    }

    @Transactional
    public RegistrationResult registerSeller(Map<String, String> form, String password, String confirmPassword) {
        Map<String, String> errors = validationService.validateRequired(
                Map.of(
                        "fullName", form.getOrDefault("fullName", ""),
                        "email", form.getOrDefault("email", ""),
                        "password", password == null ? "" : password,
                        "confirmPassword", confirmPassword == null ? "" : confirmPassword,
                        "phone", form.getOrDefault("phone", "")
                ),
                Map.of(
                        "fullName", "ФИО",
                        "email", "E-mail",
                        "password", "Пароль",
                        "confirmPassword", "Подтверждение пароля",
                        "phone", "Телефон"
                )
        );
        String emailError = validationService.validateEmail(form.getOrDefault("email", ""));
        if (emailError != null) {
            errors.put("email", emailError);
        }
        String phoneError = validationService.validatePhone(form.getOrDefault("phone", ""));
        if (phoneError != null) {
            errors.put("phone", phoneError);
        }
        String passwordError = validationService.validatePassword(password);
        if (passwordError != null) {
            errors.put("password", passwordError);
        }
        if (password != null && !password.equals(confirmPassword)) {
            errors.put("confirmPassword", "Пароли не совпадают.");
        }
        String email = form.getOrDefault("email", "").trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            errors.put("email", "Аккаунт с таким e-mail уже существует.");
        }
        if (!errors.isEmpty()) {
            return RegistrationResult.failure(errors);
        }

        String[] nameParts = splitFullName(form.get("fullName").trim());
        User user = new User();
        user.setRole(UserRole.SELLER);
        user.setFirstName(nameParts[0]);
        user.setLastName(nameParts[1]);
        user.setEmail(email);
        user.setPhone(form.get("phone").trim());
        user.setPasswordHash(authService.hashPassword(password));
        long userId = userRepository.insert(user);
        walletRepository.ensureSellerProfile(userId);
        user.setId(userId);

        finishRegistration(user, "Подтверждение регистрации продавца");
        return RegistrationResult.success(user);
    }

    private void finishRegistration(User user, String title) {
        if (properties.isRequireEmailConfirmation()) {
            String token = tokenService.issueEmailVerificationToken(user.getId());
            String url = properties.getBaseUrl() + "/confirm-email/" + token;
            mailService.sendVerificationEmail(user, url, title);
            return;
        }
        userRepository.confirmEmail(user.getId());
        user.setEmailConfirmed(true);
    }

    private String[] splitFullName(String fullName) {
        String[] parts = fullName.split("\\s+");
        if (parts.length == 0) {
            return new String[]{"", ""};
        }
        if (parts.length == 1) {
            return new String[]{parts[0], "-"};
        }
        return new String[]{parts[0], String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length))};
    }

    public record RegistrationResult(boolean ok, User user, Map<String, String> errors) {
        public static RegistrationResult success(User user) {
            return new RegistrationResult(true, user, Map.of());
        }

        public static RegistrationResult failure(Map<String, String> errors) {
            return new RegistrationResult(false, null, errors);
        }
    }
}
