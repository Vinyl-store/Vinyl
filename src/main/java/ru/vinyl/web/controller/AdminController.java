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
import ru.vinyl.repository.ProductRepository;
import ru.vinyl.repository.UserRepository;
import ru.vinyl.service.AdminService;
import ru.vinyl.service.ValidationService;
import ru.vinyl.web.FlashMessages;
import ru.vinyl.web.RequiresRole;
import ru.vinyl.web.controller.SellerController.ProductForm;
import ru.vinyl.web.session.SessionSupport;
import ru.vinyl.web.session.SessionUser;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin")
@RequiresRole(value = UserRole.ADMIN, requireConfirmedEmail = true)
public class AdminController {

    private final AdminService adminService;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ValidationService validationService;

    public AdminController(
            AdminService adminService,
            ProductRepository productRepository,
            UserRepository userRepository,
            ValidationService validationService
    ) {
        this.adminService = adminService;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.validationService = validationService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        AdminService.DashboardStats stats = adminService.getDashboardStats();
        model.addAttribute("orderStats", stats.orderStats());
        model.addAttribute("userStats", stats.userCounts());
        model.addAttribute("productStats", stats.productStats());
        return "admin-dashboard";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("buyers", adminService.listBuyers());
        return "admin-users";
    }

    @PostMapping("/users/{id}/toggle-block")
    public String toggleUserBlock(@PathVariable long id, HttpSession session) {
        adminService.toggleUserBlock(id, UserRole.BUYER);
        FlashMessages.add(session, "success", "Статус пользователя обновлен.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/update")
    public String updateUser(
            @PathVariable long id,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam(required = false) String phone,
            HttpSession session
    ) {
        adminService.updateBuyer(id, firstName.trim(), lastName.trim(), phone == null ? "" : phone.trim());
        FlashMessages.add(session, "success", "Данные пользователя обновлены.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/top-up")
    public String topUp(@PathVariable long id, @RequestParam String amount, HttpSession session) {
        try {
            BigDecimal value = validationService.parsePositiveAmount(amount);
            BigDecimal balance = adminService.topUpBuyer(id, value);
            FlashMessages.add(
                    session,
                    "success",
                    "Кошелек пополнен на " + value + " ₽. Текущий баланс: " + balance + " ₽."
            );
        } catch (IllegalArgumentException ex) {
            FlashMessages.add(session, "error", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/resend-verification")
    public String resendVerification(@PathVariable long id, HttpSession session) {
        userRepository.findById(id).ifPresent(adminService::resendVerification);
        FlashMessages.add(session, "success", "Письмо подтверждения отправлено повторно.");
        return "redirect:/admin/users";
    }

    @GetMapping("/sellers")
    public String sellers(Model model) {
        model.addAttribute("sellerRows", adminService.listSellers());
        return "admin-sellers";
    }

    @GetMapping("/sellers/{id}")
    public String sellerDetail(@PathVariable long id, Model model, HttpSession session) {
        var detail = adminService.getSellerDetail(id);
        if (detail.isEmpty()) {
            FlashMessages.add(session, "error", "Продавец не найден.");
            return "redirect:/admin/sellers";
        }
        model.addAttribute("seller", detail.get().seller());
        model.addAttribute("stats", detail.get().stats());
        model.addAttribute("products", detail.get().products());
        model.addAttribute("walletBalance", detail.get().walletBalance());
        return "admin-seller-detail";
    }

    @PostMapping("/sellers/{id}/toggle-block")
    public String toggleSellerBlock(@PathVariable long id, HttpSession session) {
        adminService.toggleUserBlock(id, UserRole.SELLER);
        FlashMessages.add(session, "success", "Статус продавца обновлен.");
        return "redirect:/admin/sellers";
    }

    @GetMapping("/products")
    public String products(Model model) {
        model.addAttribute("productsRows", adminService.listProducts());
        model.addAttribute("sellers", userRepository.findSellers());
        return "admin-products";
    }

    @PostMapping("/products/create")
    public String createProduct(
            @RequestParam long sellerId,
            ProductForm form,
            HttpSession session
    ) {
        Product product = form.toProduct();
        product.setSellerId(sellerId);
        productRepository.insert(product);
        FlashMessages.add(session, "success", "Товар создан администратором.");
        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/update")
    public String updateProduct(
            @PathVariable long id,
            ProductForm form,
            HttpSession session
    ) {
        Product product = form.toProduct();
        product.setId(id);
        productRepository.update(product);
        FlashMessages.add(session, "success", "Товар обновлен.");
        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable long id, HttpSession session) {
        productRepository.delete(id);
        FlashMessages.add(session, "success", "Товар удален.");
        return "redirect:/admin/products";
    }

    @GetMapping("/orders")
    public String orders(Model model) {
        model.addAttribute("ordersRows", adminService.listOrders());
        return "admin-orders";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(
            @PathVariable long id,
            @RequestParam String status,
            @RequestParam(required = false) String cancellationReason,
            HttpSession session
    ) {
        adminService.updateOrderStatus(id, status, cancellationReason);
        FlashMessages.add(session, "success", "Статус заказа обновлен.");
        return "redirect:/admin/orders";
    }

    @PostMapping("/orders/{id}/resend")
    public String resendOrder(@PathVariable long id, HttpSession session) {
        adminService.resendOrderEmail(id);
        FlashMessages.add(session, "success", "Уведомление по заказу отправлено повторно.");
        return "redirect:/admin/orders";
    }
}
