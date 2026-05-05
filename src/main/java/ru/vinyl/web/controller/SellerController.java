package ru.vinyl.web.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.vinyl.domain.Product;
import ru.vinyl.domain.User;
import ru.vinyl.domain.UserRole;
import ru.vinyl.service.AuthService;
import ru.vinyl.service.SellerService;
import ru.vinyl.web.FlashMessages;
import ru.vinyl.web.RequiresRole;
import ru.vinyl.web.session.SessionSupport;
import ru.vinyl.web.session.SessionUser;

import java.math.BigDecimal;

@Controller
@RequestMapping("/seller")
@RequiresRole(UserRole.SELLER)
public class SellerController {

    private final SellerService sellerService;
    private final AuthService authService;

    public SellerController(SellerService sellerService, AuthService authService) {
        this.sellerService = sellerService;
        this.authService = authService;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        SessionUser sessionUser = SessionSupport.currentUser(session).orElseThrow();
        User seller = authService.findById(sessionUser.getId()).orElseThrow();
        SellerService.SellerDashboard dashboard = sellerService.getDashboard(seller.getId());
        model.addAttribute("seller", seller);
        model.addAttribute("stats", dashboard.stats());
        model.addAttribute("products", dashboard.products());
        model.addAttribute("walletBalance", dashboard.walletBalance());
        return "seller-dashboard";
    }

    @PostMapping("/product/create")
    public String createProduct(ProductForm form, HttpSession session) {
        long sellerId = SessionSupport.currentUser(session).orElseThrow().getId();
        Product product = form.toProduct();
        sellerService.createProduct(sellerId, product).ifPresentOrElse(
                error -> FlashMessages.add(session, "error", error),
                () -> FlashMessages.add(session, "success", "Товар добавлен.")
        );
        return "redirect:/seller/dashboard";
    }

    @PostMapping("/product/{id}/update")
    public String updateProduct(@PathVariable long id, ProductForm form, HttpSession session) {
        long sellerId = SessionSupport.currentUser(session).orElseThrow().getId();
        Product product = form.toProduct();
        product.setId(id);
        sellerService.updateProduct(sellerId, product);
        FlashMessages.add(session, "success", "Карточка товара обновлена.");
        return "redirect:/seller/dashboard";
    }

    @PostMapping("/product/{id}/delete")
    public String deleteProduct(@PathVariable long id, HttpSession session) {
        long sellerId = SessionSupport.currentUser(session).orElseThrow().getId();
        sellerService.deleteProduct(sellerId, id);
        FlashMessages.add(session, "success", "Товар удален.");
        return "redirect:/seller/dashboard";
    }

    public static class ProductForm {
        private String title;
        private String artist;
        private String genre;
        private String price;
        private String description;
        private String releaseYear;
        private String stock;
        private String coverUrl;
        private String isPublished;
        private String isSale;
        private String isFeatured;

        public Product toProduct() {
            Product product = new Product();
            product.setTitle(title == null ? "" : title.trim());
            product.setArtist(artist == null ? "" : artist.trim());
            product.setGenre(genre == null ? "" : genre.trim());
            product.setPrice(new BigDecimal(price == null || price.isBlank() ? "0" : price.trim()));
            product.setDescription(description == null ? "" : description.trim());
            if (releaseYear != null && !releaseYear.isBlank()) {
                product.setReleaseYear(Integer.parseInt(releaseYear.trim()));
            }
            product.setStock(Integer.parseInt(stock == null || stock.isBlank() ? "0" : stock.trim()));
            product.setCoverUrl(coverUrl == null ? "" : coverUrl.trim());
            product.setPublished("on".equals(isPublished));
            product.setSale("on".equals(isSale));
            product.setFeatured("on".equals(isFeatured));
            return product;
        }

        public void setTitle(String title) { this.title = title; }
        public void setArtist(String artist) { this.artist = artist; }
        public void setGenre(String genre) { this.genre = genre; }
        public void setPrice(String price) { this.price = price; }
        public void setDescription(String description) { this.description = description; }
        public void setReleaseYear(String releaseYear) { this.releaseYear = releaseYear; }
        public void setStock(String stock) { this.stock = stock; }
        public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
        public void setIsPublished(String isPublished) { this.isPublished = isPublished; }
        public void setIsSale(String isSale) { this.isSale = isSale; }
        public void setIsFeatured(String isFeatured) { this.isFeatured = isFeatured; }
    }
}
