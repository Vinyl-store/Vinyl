package ru.vinyl.web.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.vinyl.domain.Product;
import ru.vinyl.domain.UserRole;
import ru.vinyl.repository.ReviewRepository;
import ru.vinyl.service.CatalogService;
import ru.vinyl.web.FlashMessages;
import ru.vinyl.web.session.SessionSupport;
import ru.vinyl.web.session.SessionUser;

@Controller
public class ProductController {

    private final CatalogService catalogService;
    private final ReviewRepository reviewRepository;

    public ProductController(CatalogService catalogService, ReviewRepository reviewRepository) {
        this.catalogService = catalogService;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable long id, HttpSession session, Model model) {
        Product product = catalogService.getProduct(id).orElse(null);
        SessionUser user = SessionSupport.currentUser(session).orElse(null);
        boolean isAdmin = user != null && user.getRole() == UserRole.ADMIN;
        if (product == null || (!product.isPublished() && !isAdmin)) {
            FlashMessages.add(session, "error", "Товар не найден.");
            return "redirect:/";
        }
        model.addAttribute("product", product);
        model.addAttribute("reviews", reviewRepository.findByProduct(id));
        return "product-detail";
    }

}
