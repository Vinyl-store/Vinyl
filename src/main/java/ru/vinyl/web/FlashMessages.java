package ru.vinyl.web;

import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.List;

public final class FlashMessages {

    private static final String KEY = "flashMessages";

    private FlashMessages() {
    }

    public static void add(HttpSession session, String category, String message) {
        if (session == null) {
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            List<FlashMessage> messages = (List<FlashMessage>) session.getAttribute(KEY);
            if (messages == null) {
                messages = new ArrayList<>();
            }
            messages.add(new FlashMessage(category, message));
            session.setAttribute(KEY, messages);
        } catch (IllegalStateException ignored) {
            // сессия уже уничтожена
        }
    }

    @SuppressWarnings("unchecked")
    public static List<FlashMessage> consume(HttpSession session) {
        if (session == null) {
            return List.of();
        }
        try {
            Object value = session.getAttribute(KEY);
            session.removeAttribute(KEY);
            if (value instanceof List<?> list) {
                return (List<FlashMessage>) list;
            }
        } catch (IllegalStateException ignored) {
            // сессия уже уничтожена
        }
        return List.of();
    }
}
