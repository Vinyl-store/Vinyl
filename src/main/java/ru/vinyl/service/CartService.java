package ru.vinyl.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vinyl.domain.CartItem;
import ru.vinyl.domain.Product;
import ru.vinyl.repository.CartRepository;
import ru.vinyl.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    public CartSummary getCart(long userId) {
        List<CartItem> items = cartRepository.findByUser(userId);
        BigDecimal total = items.stream()
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartSummary(items, total);
    }

    public Optional<String> addToCart(long userId, long productId, int quantity) {
        Optional<Product> product = productRepository.findPublishedById(productId);
        if (product.isEmpty()) {
            return Optional.of("Товар недоступен.");
        }
        if (product.get().getStock() < quantity) {
            return Optional.of("На складе недостаточно товара.");
        }
        cartRepository.addOrIncrease(userId, productId, quantity);
        return Optional.empty();
    }

    public Optional<String> updateQuantity(long userId, long productId, int quantity) {
        Optional<Product> product = productRepository.findById(productId);
        if (product.isEmpty()) {
            return Optional.of("Товар не найден.");
        }
        if (quantity > product.get().getStock()) {
            return Optional.of("Нельзя указать количество больше остатка на складе.");
        }
        cartRepository.updateQuantity(userId, productId, quantity);
        return Optional.empty();
    }

    public void remove(long userId, long productId) {
        cartRepository.remove(userId, productId);
    }

    public record CartSummary(List<CartItem> items, BigDecimal total) {
    }
}
