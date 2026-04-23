package ru.vinyl.service;

import org.springframework.stereotype.Service;
import ru.vinyl.domain.Product;
import ru.vinyl.domain.User;
import ru.vinyl.repository.ProductRepository;
import ru.vinyl.repository.SellerStatsRepository;
import ru.vinyl.repository.WalletRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class SellerService {

    private final ProductRepository productRepository;
    private final SellerStatsRepository sellerStatsRepository;
    private final WalletRepository walletRepository;

    public SellerService(
            ProductRepository productRepository,
            SellerStatsRepository sellerStatsRepository,
            WalletRepository walletRepository
    ) {
        this.productRepository = productRepository;
        this.sellerStatsRepository = sellerStatsRepository;
        this.walletRepository = walletRepository;
    }

    public SellerDashboard getDashboard(long sellerId) {
        SellerStatsRepository.SellerStats stats = sellerStatsRepository.getStats(sellerId);
        BigDecimal wallet = walletRepository.getSellerBalance(sellerId);
        List<Product> products = productRepository.findBySeller(sellerId);
        return new SellerDashboard(stats, wallet, products);
    }

    public Optional<String> createProduct(long sellerId, Product product) {
        if (product.getTitle() == null || product.getTitle().isBlank()
                || product.getArtist() == null || product.getArtist().isBlank()) {
            return Optional.of("Укажите название и исполнителя.");
        }
        product.setSellerId(sellerId);
        productRepository.insert(product);
        return Optional.empty();
    }

    public void updateProduct(long sellerId, Product product) {
        productRepository.updateBySeller(product, sellerId);
    }

    public void deleteProduct(long sellerId, long productId) {
        productRepository.deleteBySeller(productId, sellerId);
    }

    public record SellerDashboard(
            SellerStatsRepository.SellerStats stats,
            BigDecimal walletBalance,
            List<Product> products
    ) {
    }
}
