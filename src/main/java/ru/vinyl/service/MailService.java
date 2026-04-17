package ru.vinyl.service;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import ru.vinyl.config.VinylProperties;
import ru.vinyl.domain.User;

import java.math.BigDecimal;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final String mailFrom;
    private final VinylProperties properties;
    private final String smtpUsername;

    public MailService(
            JavaMailSender mailSender,
            @Value("${vinyl.mail-from}") String mailFrom,
            @Value("${spring.mail.username:}") String smtpUsername,
            VinylProperties properties
    ) {
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
        this.smtpUsername = smtpUsername == null ? "" : smtpUsername.trim();
        this.properties = properties;
    }

    @PostConstruct
    void logMailConfig() {
        if (smtpUsername.isEmpty()) {
            log.warn("[MAIL] SMTP login not set (spring.mail.username). Letters will not be sent.");
        } else {
            log.info("[MAIL] SMTP: {} | from: {} | verification links in console only: {}",
                    smtpUsername, mailFrom, properties.isMailConsoleOnly());
        }
    }

    public void sendVerificationEmail(User user, String verificationUrl, String title) {
        String html = """
                <h2>Подтверждение аккаунта</h2>
                <p>Здравствуйте, %s %s.</p>
                <p>Для активации аккаунта перейдите по ссылке:</p>
                <p><a href="%s">%s</a></p>
                <p>Если вы не создавали аккаунт, просто проигнорируйте это письмо.</p>
                """.formatted(user.getFirstName(), user.getLastName(), verificationUrl, verificationUrl);
        send(user.getEmail(), title, html, verificationUrl);
    }

    public void sendOrderEmail(String email, long orderId, BigDecimal total) {
        sendOrderStatusEmail(email, orderId, total, "new", null);
    }

    public void sendOrderStatusEmail(String email, long orderId, BigDecimal total, String status, String cancellationReason) {
        String statusLabel = orderStatusLabel(status);
        StringBuilder html = new StringBuilder("""
                <h2>Обновление заказа</h2>
                <p>Заказ №%d</p>
                <p>Текущий статус: <strong>%s</strong></p>
                <p>Сумма заказа: <strong>%.2f ₽</strong></p>
                """.formatted(orderId, statusLabel, total));
        if ("cancelled".equals(status) && cancellationReason != null && !cancellationReason.isBlank()) {
            html.append("<p>Причина отмены: ").append(escapeHtml(cancellationReason)).append("</p>");
        }
        if ("cancelled".equals(status)) {
            html.append("<p>Сумма заказа возвращена на ваш кошелёк.</p>");
        }
        html.append("<p>Подробности можно посмотреть в личном кабинете.</p>");
        String subject = "Заказ №" + orderId + " — " + statusLabel;
        send(email, subject, html.toString(), null);
    }

    private static String orderStatusLabel(String status) {
        if (status == null) {
            return "неизвестен";
        }
        return switch (status) {
            case "new" -> "Новый";
            case "processing" -> "В обработке";
            case "completed" -> "Выполнен";
            case "cancelled" -> "Отменён";
            default -> status;
        };
    }

    private static String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public void sendAdminResetEmail(User admin, String resetUrl) {
        String html = """
                <h2>Сброс пароля</h2>
                <p>Здравствуйте, %s.</p>
                <p>Чтобы установить новый пароль, перейдите по ссылке:</p>
                <p><a href="%s">%s</a></p>
                <p>Ссылка одноразовая и ограничена по времени.</p>
                """.formatted(admin.getFirstName(), resetUrl, resetUrl);
        send(admin.getEmail(), "Сброс пароля администратора", html, resetUrl);
    }

    private void send(String to, String subject, String html, String actionUrl) {
        boolean verificationMail = actionUrl != null && !actionUrl.isBlank();
        if (properties.isMailConsoleOnly() && verificationMail) {
            logMailFallback(subject, to, actionUrl);
            return;
        }
        if (smtpUsername.isEmpty()) {
            log.warn("[MAIL] SMTP not configured, letter not sent: {} -> {}", subject, to);
            logMailFallback(subject, to, actionUrl);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText("Ваш почтовый клиент не поддерживает HTML-письма.", html);
            mailSender.send(message);
            log.info("[MAIL] Sent: {} -> {}", subject, to);
        } catch (Exception ex) {
            log.warn("[MAIL] Send failed ({}): {}", to, ex.getMessage());
            logMailFallback(subject, to, actionUrl);
        }
    }

    private void logMailFallback(String subject, String to, String actionUrl) {
        if (actionUrl != null && !actionUrl.isBlank()) {
            log.info("[MAIL] (console) {} -> {}", subject, to);
            log.info("================================================================");
            log.info("Email confirmation link:");
            log.info("{}", actionUrl);
            log.info("================================================================");
        }
    }
}
