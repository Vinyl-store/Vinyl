package ru.vinyl.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vinyl.domain.Order;
import ru.vinyl.domain.Product;
import ru.vinyl.domain.User;
import ru.vinyl.domain.UserRole;
import ru.vinyl.repository.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final WalletRepository walletRepository;
    private final SellerStatsRepository sellerStatsRepository;
    private final TokenService tokenService;
    private final MailService mailService;
    private final ru.vinyl.config.VinylProperties properties;

    public AdminService(
            UserRepository userRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            WalletRepository walletRepository,
            SellerStatsRepository sellerStatsRepository,
            TokenService tokenService,
            MailService mailService,
            ru.vinyl.config.VinylProperties properties
    ) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.walletRepository = walletRepository;
        this.sellerStatsRepository = sellerStatsRepository;
        this.tokenService = tokenService;
        this.mailService = mailService;
        this.properties = properties;
    }

    public DashboardStats getDashboardStats() {
        OrderRepository.OrderStats orderStats = orderRepository.countStats();
        UserRepository.UserCounts userCounts = userRepository.countByRole();
        ProductRepository.ProductStats productStats = productRepository.countStats();
        return new DashboardStats(orderStats, userCounts, productStats);
    }

    public List<BuyerView> listBuyers() {
        return userRepository.findBuyers().stream()
                .map(user -> new BuyerView(user, walletRepository.getBuyerBalance(user.getId())))
                .toList();
    }

    public List<SellerView> listSellers() {
        return userRepository.findSellers().stream()
                .map(user -> new SellerView(
                        user,
                        walletRepository.getSellerBalance(user.getId()),
                        sellerStatsRepository.countProducts(user.getId())
                ))
                .toList();
    }

    public Optional<SellerDetailView> getSellerDetail(long sellerId) {
        return userRepository.findSellerById(sellerId)
                .map(seller -> new SellerDetailView(
                        seller,
                        walletRepository.getSellerBalance(sellerId),
                        sellerStatsRepository.getStats(sellerId),
                        productRepository.findBySeller(sellerId)
                ));
    }

    public void toggleUserBlock(long userId, UserRole role) {
        userRepository.toggleBlock(userId, role);
    }

    public void updateBuyer(long userId, String firstName, String lastName, String phone) {
        userRepository.updateBuyer(userId, firstName, lastName, phone);
    }

    public BigDecimal topUpBuyer(long userId, BigDecimal amount) {
        return walletRepository.topUpBuyer(userId, amount);
    }

    public void resendVerification(User user) {
        String token = tokenService.issueEmailVerificationToken(user.getId());
        String url = properties.getBaseUrl() + "/confirm-email/" + token;
        mailService.sendVerificationEmail(user, url, "Подтверждение почты");
    }

    public List<Product> listProducts() {
        return productRepository.findAllWithSeller();
    }

    public List<Order> listOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public void updateOrderStatus(long orderId, String status, String reason) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return;
        }
        String previousStatus = order.getStatus();

        orderRepository.updateStatus(orderId, status, reason);

        if ("cancelled".equals(status) && !"cancelled".equals(previousStatus)) {
            walletRepository.topUpBuyer(order.getBuyerId(), order.getTotalAmount());
            orderRepository.updatePaymentStatus(orderId, "refunded");
            for (OrderRepository.OrderItemRow item : orderRepository.findItemsByOrderId(orderId)) {
                if (item.productId() != null) {
                    productRepository.increaseStock(item.productId(), item.quantity());
                }
            }
            if ("completed".equals(previousStatus)) {
                walletRepository.refreshSellerBalances();
            }
        } else if ("completed".equals(status)) {
            walletRepository.refreshSellerBalances();
        }

        orderRepository.findById(orderId).ifPresent(updated -> mailService.sendOrderStatusEmail(
                updated.getBuyerEmail(),
                updated.getId(),
                updated.getTotalAmount(),
                status,
                reason
        ));
    }

    public void resendOrderEmail(long orderId) {
        orderRepository.findById(orderId).ifPresent(order -> mailService.sendOrderStatusEmail(
                order.getBuyerEmail(),
                order.getId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCancellationReason()
        ));
    }

    public record DashboardStats(
            OrderRepository.OrderStats orderStats,
            UserRepository.UserCounts userCounts,
            ProductRepository.ProductStats productStats
    ) {
    }

    public record BuyerView(User user, BigDecimal walletBalance) {
    }

    public record SellerView(User user, BigDecimal walletBalance, int productCount) {
    }

    public record SellerDetailView(
            User seller,
            BigDecimal walletBalance,
            SellerStatsRepository.SellerStats stats,
            List<Product> products
    ) {
    }
}
