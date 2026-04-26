package ru.vinyl.web.session;

import jakarta.servlet.http.HttpSession;
import ru.vinyl.domain.User;
import ru.vinyl.domain.UserRole;

import java.util.Optional;

public final class SessionSupport {

    private SessionSupport() {
    }

    public static void login(HttpSession session, User user) {
        session.setAttribute(SessionKeys.USER, new SessionUser(user));
    }

    public static void logout(HttpSession session) {
        if (session == null) {
            return;
        }
        try {
            session.removeAttribute(SessionKeys.USER);
        } catch (IllegalStateException ignored) {
            // сессия уже недействительна
        }
    }

    public static Optional<SessionUser> currentUser(HttpSession session) {
        Object value = session.getAttribute(SessionKeys.USER);
        if (value instanceof SessionUser sessionUser) {
            return Optional.of(sessionUser);
        }
        return Optional.empty();
    }

    public static boolean hasRole(HttpSession session, UserRole role) {
        return currentUser(session).map(user -> user.getRole() == role).orElse(false);
    }
}
