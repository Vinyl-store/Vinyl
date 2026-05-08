package ru.vinyl.web.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.vinyl.config.VinylProperties;
import ru.vinyl.domain.User;
import ru.vinyl.domain.UserRole;
import ru.vinyl.repository.TokenRepository;
import ru.vinyl.repository.UserRepository;
import ru.vinyl.service.AuthService;
import ru.vinyl.service.MailService;
import ru.vinyl.service.TokenService;
import ru.vinyl.service.ValidationService;
import ru.vinyl.web.FlashMessages;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
public class EmailController {

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final MailService mailService;
    private final ValidationService validationService;
    private final VinylProperties properties;

    public EmailController(
            TokenService tokenService,
            UserRepository userRepository,
            AuthService authService,
            MailService mailService,
            ValidationService validationService,
            VinylProperties properties
    ) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.authService = authService;
        this.mailService = mailService;
        this.validationService = validationService;
        this.properties = properties;
    }

    @GetMapping("/confirm-email/{token}")
    public String confirmEmail(@PathVariable String token, HttpSession session) {
        Optional<TokenRepository.EmailVerification> verification = tokenService.findEmailToken(token);
        if (verification.isEmpty()
                || verification.get().used()
                || verification.get().expiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            FlashMessages.add(session, "error", "Ссылка подтверждения недействительна или уже использована.");
            return "redirect:/";
        }
        userRepository.confirmEmail(verification.get().userId());
        tokenService.markEmailTokenUsed(verification.get().id());
        return "activation-success";
    }

    @GetMapping("/admin/reset-password")
    public String resetRequestForm() {
        return "admin-reset-request";
    }

    @PostMapping("/admin/reset-password")
    public String resetRequest(@RequestParam String email, HttpSession session) {
        userRepository.findByEmailAndRole(email, UserRole.ADMIN).ifPresent(admin -> {
            String token = tokenService.issuePasswordResetToken(admin.getId());
            String url = properties.getBaseUrl() + "/admin/reset-password/" + token;
            mailService.sendAdminResetEmail(admin, url);
        });
        FlashMessages.add(
                session,
                "success",
                "Если администратор с таким e-mail существует, письмо уже отправлено."
        );
        return "redirect:/login?role=admin";
    }

    @GetMapping("/admin/reset-password/{token}")
    public String resetForm(@PathVariable String token, HttpSession session, org.springframework.ui.Model model) {
        if (!isResetValid(token)) {
            FlashMessages.add(session, "error", "Ссылка сброса пароля недействительна или истекла.");
            return "redirect:/login?role=admin";
        }
        model.addAttribute("token", token);
        model.addAttribute("errors", Map.<String, String>of());
        return "admin-reset-form";
    }

    @PostMapping("/admin/reset-password/{token}")
    public String resetPassword(
            @PathVariable String token,
            @RequestParam String password,
            @RequestParam("confirmPassword") String confirmPassword,
            HttpSession session,
            org.springframework.ui.Model model
    ) {
        Optional<TokenRepository.PasswordReset> reset = tokenService.findResetToken(token, UserRole.ADMIN.getCode());
        if (reset.isEmpty()
                || reset.get().used()
                || reset.get().expiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            FlashMessages.add(session, "error", "Ссылка сброса пароля недействительна или истекла.");
            return "redirect:/login?role=admin";
        }

        Map<String, String> errors = new HashMap<>();
        String passwordError = validationService.validatePassword(password);
        if (passwordError != null) {
            errors.put("password", passwordError);
        }
        if (!password.equals(confirmPassword)) {
            errors.put("confirmPassword", "Пароли не совпадают.");
        }
        if (!errors.isEmpty()) {
            model.addAttribute("token", token);
            model.addAttribute("errors", errors);
            return "admin-reset-form";
        }

        userRepository.updatePassword(reset.get().userId(), authService.hashPassword(password));
        tokenService.markResetTokenUsed(reset.get().id());
        FlashMessages.add(session, "success", "Пароль администратора обновлен.");
        return "redirect:/login?role=admin";
    }

    private boolean isResetValid(String token) {
        return tokenService.findResetToken(token, UserRole.ADMIN.getCode())
                .filter(reset -> !reset.used())
                .filter(reset -> reset.expiresAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC)))
                .isPresent();
    }
}
