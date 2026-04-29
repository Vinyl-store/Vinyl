package ru.vinyl.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import ru.vinyl.config.VinylProperties;
import ru.vinyl.domain.UserRole;
import ru.vinyl.web.RequiresRole;
import ru.vinyl.web.session.SessionSupport;
import ru.vinyl.web.session.SessionUser;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final VinylProperties properties;

    public AuthInterceptor(VinylProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequiresRole roleAnnotation = handlerMethod.getMethodAnnotation(RequiresRole.class);
        if (roleAnnotation == null) {
            roleAnnotation = handlerMethod.getBeanType().getAnnotation(RequiresRole.class);
        }
        if (roleAnnotation == null) {
            return true;
        }

        HttpSession session = request.getSession();
        SessionUser user = SessionSupport.currentUser(session).orElse(null);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        if (user.isBlocked()) {
            SessionSupport.logout(session);
            response.sendRedirect(request.getContextPath() + "/login?blocked=1");
            return false;
        }
        if (user.getRole() != roleAnnotation.value()) {
            response.sendRedirect(request.getContextPath() + "/");
            return false;
        }
        if (properties.isRequireEmailConfirmation()
                && roleAnnotation.requireConfirmedEmail()
                && !user.isEmailConfirmed()) {
            response.sendRedirect(request.getContextPath() + "/login?role=admin&unconfirmed=1");
            return false;
        }
        return true;
    }
}
