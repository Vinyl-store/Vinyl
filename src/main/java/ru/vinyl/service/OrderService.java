package ru.vinyl.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vinyl.domain.CartItem;
import ru.vinyl.domain.Order;
import ru.vinyl.domain.Product;
import ru.vinyl.domain.User;
import ru.vinyl.repository.CartRepository;
import ru.vinyl.repository.OrderRepository;
import ru.vinyl.repository.ProductRepository;
import ru.vinyl.repository.WalletRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final CartService cartService;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final WalletRepository walletRepository;
    private final MailService mailService;

    public OrderService(
            CartService cartService,
            CartRepository cartRepository,
            OrderRepository orderRepository,
            ProductRepository productRepository,
            WalletRepository walletRepository,
            MailService mailService
    ) {
        this.cartService = cartService;
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.walletRepository = walletRepository;
        this.mailService = mailService;
    }

    public List<Order> getActiveOrders(long buyerId) {
        return orderRepository.findActiveByBuyer(buyerId);
    }

    public List<Order> getOrderHistory(long buyerId) {
        return orderRepository.findHistoryByBuyer(buyerId);
    }

    @Transactional
    public CheckoutResult checkout(User buyer) {
        CartService.CartSummary cart = cartService.getCart(buyer.getId());
        if (cart.items().isEmpty()) {
            return CheckoutResult.error("Корзина пуста.");
        }
        for (CartItem item : cart.items()) {
            if (item.getQuantity() > item.getStock()) {
                return CheckoutResult.error("Товара «" + item.getTitle() + "» недостаточно на складе.");
            }
        }

        Optional<BigDecimal> newBalance = walletRepository.deductBuyer(buyer.getId(), cart.total());
        if (newBalance.isEmpty()) {
            BigDecimal balance = walletRepository.getBuyerBalance(buyer.getId());
            return CheckoutResult.error(
                    "Недостаточно средств на кошельке. Доступно: "
                            + balance + " ₽, нужно: " + cart.total() + " ₽."
            );
        }

        long orderId = orderRepository.create(buyer.getId(), cart.total());
        for (CartItem item : cart.items()) {
            Product product = productRepository.findById(item.getProductId()).orElseThrow();
            orderRepository.addItem(
                    orderId,
                    item.getProductId(),
                    product.getSellerId(),
                    item.getTitle(),
                    item.getArtist(),
                    item.getPrice(),
                    item.getQuantity()
            );
            productRepository.decreaseStock(item.getProductId(), item.getQuantity());
        }
        cartRepository.clear(buyer.getId());
        mailService.sendOrderEmail(buyer.getEmail(), orderId, cart.total());
        return CheckoutResult.success(orderId);
    }

    public record CheckoutResult(boolean ok, String message, Long orderId) {
        public static CheckoutResult success(long orderId) {
            return new CheckoutResult(true, null, orderId);
        }

        public static CheckoutResult error(String message) {
            return new CheckoutResult(false, message, null);
        }
    }
}
