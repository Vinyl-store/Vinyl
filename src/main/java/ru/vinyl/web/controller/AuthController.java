package ru.vinyl.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.vinyl.config.VinylProperties;
import ru.vinyl.domain.User;
import ru.vinyl.domain.UserRole;
import ru.vinyl.repository.UserRepository;
import ru.vinyl.service.AuthService;
import ru.vinyl.service.RegistrationService;
import ru.vinyl.web.FlashMessages;
import ru.vinyl.web.session.SessionSupport;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
public class AuthController {

    private final RegistrationService registrationService;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final VinylProperties properties;

    public AuthController(
            RegistrationService registrationService,
            AuthService authService,
            UserRepository userRepository,
            VinylProperties properties
    ) {
        this.registrationService = registrationService;
        this.authService = authService;
        this.userRepository = userRepository;
        this.properties = properties;
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("formData", Map.of("firstName", "", "lastName", "", "email", "", "phone", ""));
        model.addAttribute("errors", Map.<String, String>of());
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String password,
            Model model
    ) {
        Map<String, String> form = Map.of(
                "firstName", firstName,
                "lastName", lastName,
                "email", email,
                "phone", phone
        );
        RegistrationService.RegistrationResult result = registrationService.registerBuyer(
                new HashMap<>(form),
                password
        );
        if (!result.ok()) {
            model.addAttribute("formData", form);
            model.addAttribute("errors", result.errors());
            return "register";
        }
        if (properties.isRequireEmailConfirmation()) {
            return "redirect:/verification-notice?email=" + result.user().getEmail();
        }
        return "redirect:/login?registered=1";
    }

    @GetMapping("/seller/register")
    public String sellerRegisterForm(Model model) {
        model.addAttribute("formData", Map.of("fullName", "", "email", "", "phone", ""));
        model.addAttribute("errors", Map.<String, String>of());
        return "seller-register";
    }

    @PostMapping("/seller/register")
    public String sellerRegister(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model
    ) {
        Map<String, String> form = Map.of("fullName", fullName, "email", email, "phone", phone);
        RegistrationService.RegistrationResult result = registrationService.registerSeller(
                new HashMap<>(form),
                password,
                confirmPassword
        );
        if (!result.ok()) {
            model.addAttribute("formData", form);
            model.addAttribute("errors", result.errors());
            return "seller-register";
        }
        if (properties.isRequireEmailConfirmation()) {
            return "redirect:/verification-notice?email=" + result.user().getEmail();
        }
        return "redirect:/login?registered=1";
    }

    @GetMapping("/login")
    public String loginForm(
            @RequestParam(value = "role", defaultValue = "buyer") String role,
            @RequestParam(value = "blocked", required = false) String blocked,
            @RequestParam(value = "unconfirmed", required = false) String unconfirmed,
            @RequestParam(value = "registered", required = false) String registered,
            Model model
    ) {
        model.addAttribute("requestedRole", role);
        Map<String, String> errors = new HashMap<>();
        if (registered != null) {
            errors.put("general", "Регистрация завершена. Войдите с выбранной при регистрации ролью.");
        } else if (blocked != null) {
            errors.put("general", "Аккаунт заблокирован администратором.");
        } else if (unconfirmed != null) {
            errors.put(
                    "general",
                    "Подтвердите e-mail по ссылке из письма (или в консоли сервера при локальном запуске)."
            );
        }
        model.addAttribute("errors", errors);
        model.addAttribute("requireEmailConfirmation", properties.isRequireEmailConfirmation());
        return "login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(value = "role", defaultValue = "buyer") String roleCode,
            HttpServletRequest request,
            HttpSession session,
            Model model
    ) {
        UserRole role = UserRole.fromCode(roleCode);
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }

        Map<String, String> errors = new HashMap<>();
        Optional<AuthService.LockoutInfo> lockout = authService.getLockout(email, role);
        if (lockout.isPresent() && lockout.get().locked()) {
            errors.put(
                    "general",
                    "Слишком много неуспешных попыток входа. Повторите попытку через "
                            + lockout.get().remainingMinutes() + " мин."
            );
        }

        Optional<User> user = Optional.empty();
        if (errors.isEmpty()) {
            user = authService.authenticate(email, role, password);
            if (user.isEmpty()) {
                authService.recordLoginAttempt(email, role, false, ip);
                String message = "Неверные учетные данные. Проверьте e-mail и пароль.";
                Optional<User> byEmail = userRepository.findByEmail(email);
                if (byEmail.isPresent() && byEmail.get().getRole() != role) {
                    message = "Этот e-mail зарегистрирован как "
                            + roleDisplayName(byEmail.get().getRole())
                            + ". Выберите эту роль в списке выше.";
                } else {
                    message += " Если аккаунта ещё нет, пройдите регистрацию.";
                }
                errors.put("general", message);
            } else if (user.get().isBlocked()) {
                authService.recordLoginAttempt(email, role, false, ip);
                errors.put("general", "Аккаунт заблокирован администратором.");
                user = Optional.empty();
            } else if (properties.isRequireEmailConfirmation() && !user.get().isEmailConfirmed()) {
                errors.put(
                        "general",
                        "Подтвердите e-mail по ссылке из письма. Ссылка также пишется в консоль при запуске без SMTP."
                );
                user = Optional.empty();
            }
        }

        if (!errors.isEmpty()) {
            model.addAttribute("requestedRole", roleCode);
            model.addAttribute("errors", errors);
            model.addAttribute("requireEmailConfirmation", properties.isRequireEmailConfirmation());
            return "login";
        }

        authService.recordLoginAttempt(email, role, true, ip);
        SessionSupport.login(session, user.get());
        return switch (role) {
            case SELLER -> "redirect:/seller/dashboard";
            case ADMIN -> "redirect:/admin/dashboard";
            default -> "redirect:/buyer/dashboard";
        };
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        FlashMessages.add(session, "success", "Вы вышли из учетной записи.");
        SessionSupport.logout(session);
        return "redirect:/";
    }

    @GetMapping("/verification-notice")
    public String verificationNotice(@RequestParam String email, Model model) {
        model.addAttribute("email", email);
        return "verification-notice";
    }

    @GetMapping("/admin/login")
    public String adminLogin() {
        return "redirect:/login?role=admin";
    }

    private static String roleDisplayName(UserRole role) {
        return switch (role) {
            case BUYER -> "покупатель";
            case SELLER -> "продавец";
            case ADMIN -> "администратор";
        };
    }
}
