package ru.vinyl.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ValidationService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Map<String, String> validateRequired(Map<String, String> data, Map<String, String> labels) {
        Map<String, String> errors = new HashMap<>();
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            String value = data.getOrDefault(entry.getKey(), "");
            if (value == null || value.trim().isEmpty()) {
                errors.put(entry.getKey(), "Поле «" + entry.getValue() + "» обязательно для заполнения.");
            }
        }
        return errors;
    }

    public String validateEmail(String email) {
        if (!EMAIL.matcher(email == null ? "" : email).matches()) {
            return "Введите корректный адрес e-mail.";
        }
        return null;
    }

    public String validatePassword(String password) {
        if (password == null || password.length() < 8) {
            return "Пароль должен содержать не менее 8 символов.";
        }
        if (!password.matches(".*[A-Za-zА-Яа-я].*")) {
            return "Пароль должен содержать буквы.";
        }
        if (!password.matches(".*\\d.*")) {
            return "Пароль должен содержать цифры.";
        }
        return null;
    }

    public String validatePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "Поле «Телефон» обязательно для заполнения.";
        }
        String cleaned = phone.replaceAll("[^\\d+]", "");
        if (cleaned.length() < 10) {
            return "Введите корректный номер телефона.";
        }
        return null;
    }

    public BigDecimal parsePositiveAmount(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Укажите корректную сумму пополнения.");
        }
        try {
            BigDecimal amount = new BigDecimal(value.trim().replace(',', '.'));
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Сумма пополнения должна быть больше нуля.");
            }
            return amount.setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Укажите корректную сумму пополнения.");
        }
    }

    public int toInt(String value, int defaultValue, int minimum) {
        try {
            int parsed = Integer.parseInt(value);
            return Math.max(parsed, minimum);
        } catch (Exception ex) {
            return Math.max(defaultValue, minimum);
        }
    }
}
