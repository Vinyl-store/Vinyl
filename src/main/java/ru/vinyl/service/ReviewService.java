package ru.vinyl.service;

import org.springframework.stereotype.Service;
import ru.vinyl.repository.ProductRepository;
import ru.vinyl.repository.ReviewRepository;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
    }

    public boolean canReview(long userId, long productId) {
        return reviewRepository.hasCompletedPurchase(userId, productId);
    }

    public void saveReview(long userId, long productId, int rating, String text) {
        reviewRepository.upsert(productId, userId, rating, text);
        productRepository.updateRating(productId);
    }
}
