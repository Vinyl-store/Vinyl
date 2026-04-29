package ru.vinyl.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import ru.vinyl.domain.UserRole;
import ru.vinyl.repository.CartRepository;
import ru.vinyl.repository.FavoriteRepository;
import ru.vinyl.web.session.SessionSupport;
import ru.vinyl.web.session.SessionUser;

@Component
public class NavigationInterceptor implements HandlerInterceptor {

    private final CartRepository cartRepository;
    private final FavoriteRepository favoriteRepository;

    public NavigationInterceptor(CartRepository cartRepository, FavoriteRepository favoriteRepository) {
        this.cartRepository = cartRepository;
        this.favoriteRepository = favoriteRepository;
    }

    @Override
    public void postHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            ModelAndView modelAndView
    ) {
        if (modelAndView == null) {
            return;
        }
        HttpSession session = request.getSession(false);
        SessionUser navUser = session == null ? null : SessionSupport.currentUser(session).orElse(null);
        modelAndView.addObject("navUser", navUser);
        int cartCount = 0;
        int favoritesCount = 0;
        if (navUser != null && navUser.getRole() == UserRole.BUYER) {
            cartCount = cartRepository.countItems(navUser.getId());
            favoritesCount = favoriteRepository.countByUser(navUser.getId());
        }
        modelAndView.addObject("cartCount", cartCount);
        modelAndView.addObject("favoritesCount", favoritesCount);
        modelAndView.addObject("currentUser", navUser);
    }
}
