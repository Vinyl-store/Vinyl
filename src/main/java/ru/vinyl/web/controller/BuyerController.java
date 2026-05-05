package ru.vinyl.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.vinyl.domain.User;
import ru.vinyl.domain.UserRole;
import ru.vinyl.repository.FavoriteRepository;
import ru.vinyl.repository.OrderRepository;
import ru.vinyl.repository.WalletRepository;
import ru.vinyl.service.AuthService;
import ru.vinyl.service.CartService;
import ru.vinyl.service.OrderService;
import ru.vinyl.service.ReviewService;
import ru.vinyl.service.ValidationService;
import ru.vinyl.web.FlashMessages;
import ru.vinyl.web.RequiresRole;
import ru.vinyl.web.session.SessionSupport;
import ru.vinyl.web.session.SessionUser;

@Controller
@RequestMapping("/buyer")
@RequiresRole(UserRole.BUYER)
public class BuyerController {

    private final AuthService authService;
    private final OrderService orderService;
    private final CartService cartService;
    private final FavoriteRepository favoriteRepository;
    private final WalletRepository walletRepository;
    private final OrderRepository orderRepository;
    private final ValidationService validationService;
    private final ReviewService reviewService;

    public BuyerController(
            AuthService authService,
            OrderService orderService,
            CartService cartService,
            FavoriteRepository favoriteRepository,
            WalletRepository walletRepository,
            OrderRepository orderRepository,
            ValidationService validationService,
            ReviewService reviewService
    ) {
        this.authService = authService;
        this.orderService = orderService;
        this.cartService = cartService;
        this.favoriteRepository = favoriteRepository;
        this.walletRepository = walletRepository;
        this.orderRepository = orderRepository;
        this.validationService = validationService;
        this.reviewService = reviewService;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        SessionUser sessionUser = SessionSupport.currentUser(session).orElseThrow();
        User user = authService.findById(sessionUser.getId()).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("currentOrders", orderService.getActiveOrders(user.getId()));
        model.addAttribute("orderHistory", orderService.getOrderHistory(user.getId()));
        return "buyer-dashboard";
    }

    @GetMapping("/wallet")
    public String wallet(HttpSession session, Model model) {
        SessionUser sessionUser = SessionSupport.currentUser(session).orElseThrow();
        User user = authService.findById(sessionUser.getId()).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("walletBalance", walletRepository.getBuyerBalance(user.getId()));
        model.addAttribute("stats", orderRepository.buyerStats(user.getId()));
        return "wallet";
    }

    @GetMapping("/cart")
    public String cart(HttpSession session, Model model) {
        long userId = SessionSupport.currentUser(session).orElseThrow().getId();
        CartService.CartSummary cart = cartService.getCart(userId);
        model.addAttribute("items", cart.items());
        model.addAttribute("total", cart.total());
        model.addAttribute("walletBalance", walletRepository.getBuyerBalance(userId));
        return "cart";
    }

    @PostMapping("/cart/add/{productId}")
    public String addToCart(
            @PathVariable long productId,
            @RequestParam(defaultValue = "1") String quantityValue,
            HttpServletRequest request,
            HttpSession session
    ) {
        long userId = SessionSupport.currentUser(session).orElseThrow().getId();
        int quantity = validationService.toInt(quantityValue, 1, 1);
        cartService.addToCart(userId, productId, quantity).ifPresentOrElse(
                error -> FlashMessages.add(session, "error", error),
                () -> FlashMessages.add(session, "success", "Товар добавлен в корзину.")
        );
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/buyer/cart");
    }

    @PostMapping("/cart/update/{productId}")
    public String updateCart(
            @PathVariable long productId,
            @RequestParam String quantity,
            HttpSession session
    ) {
        long userId = SessionSupport.currentUser(session).orElseThrow().getId();
        int qty = validationService.toInt(quantity, 1, 1);
        cartService.updateQuantity(userId, productId, qty).ifPresentOrElse(
                error -> FlashMessages.add(session, "error", error),
                () -> FlashMessages.add(session, "success", "Количество товара обновлено.")
        );
        return "redirect:/buyer/cart";
    }

    @PostMapping("/cart/remove/{productId}")
    public String removeFromCart(@PathVariable long productId, HttpSession session) {
        long userId = SessionSupport.currentUser(session).orElseThrow().getId();
        cartService.remove(userId, productId);
        FlashMessages.add(session, "success", "Товар удален из корзины.");
        return "redirect:/buyer/cart";
    }

    @GetMapping("/favorites")
    public String favorites(HttpSession session, Model model) {
        long userId = SessionSupport.currentUser(session).orElseThrow().getId();
        model.addAttribute("items", favoriteRepository.findByUser(userId));
        return "favorites";
    }

    @PostMapping("/favorites/add/{productId}")
    public String addFavorite(@PathVariable long productId, HttpServletRequest request, HttpSession session) {
        long userId = SessionSupport.currentUser(session).orElseThrow().getId();
        favoriteRepository.add(userId, productId);
        FlashMessages.add(session, "success", "Товар добавлен в избранное.");
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/buyer/favorites");
    }

    @PostMapping("/favorites/remove/{productId}")
    public String removeFavorite(@PathVariable long productId, HttpSession session) {
        long userId = SessionSupport.currentUser(session).orElseThrow().getId();
        favoriteRepository.remove(userId, productId);
        FlashMessages.add(session, "success", "Товар удален из избранного.");
        return "redirect:/buyer/favorites";
    }

    @PostMapping("/review/{productId}")
    public String createReview(
            @PathVariable long productId,
            @RequestParam(defaultValue = "5") int rating,
            @RequestParam(value = "reviewText", defaultValue = "") String reviewText,
            HttpSession session
    ) {
        SessionUser user = SessionSupport.currentUser(session).orElseThrow();
        if (!reviewService.canReview(user.getId(), productId)) {
            FlashMessages.add(session, "error", "Оставить отзыв можно только после завершенной покупки.");
            return "redirect:/product/" + productId;
        }
        reviewService.saveReview(user.getId(), productId, Math.min(Math.max(rating, 1), 5), reviewText.trim());
        FlashMessages.add(session, "success", "Отзыв сохранен.");
        return "redirect:/product/" + productId;
    }

    @PostMapping("/checkout")
    public String checkout(HttpSession session) {
        SessionUser sessionUser = SessionSupport.currentUser(session).orElseThrow();
        User user = authService.findById(sessionUser.getId()).orElseThrow();
        OrderService.CheckoutResult result = orderService.checkout(user);
        if (!result.ok()) {
            FlashMessages.add(session, "error", result.message());
            return "redirect:/buyer/cart";
        }
        FlashMessages.add(
                session,
                "success",
                "Заказ №" + result.orderId() + " успешно оформлен. Подтверждение отправлено на e-mail."
        );
        return "redirect:/buyer/dashboard";
    }
}
