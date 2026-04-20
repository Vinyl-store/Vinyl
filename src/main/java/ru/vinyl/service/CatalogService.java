package ru.vinyl.service;

import org.springframework.stereotype.Service;
import ru.vinyl.domain.Product;
import ru.vinyl.repository.ProductRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CatalogService {

    private static final int PER_PAGE = 8;

    private final ProductRepository productRepository;

    public CatalogService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public HomePageData getHomePage(String searchQuery, int page) {
        List<Product> featured = productRepository.findFeatured(12);
        List<Product> saleItems = productRepository.findSaleItems(8);
        Map<String, List<Product>> genres = new LinkedHashMap<>();
        for (Product item : featured) {
            String genre = item.getGenre() == null || item.getGenre().isBlank() ? "Разное" : item.getGenre();
            genres.computeIfAbsent(genre, key -> new ArrayList<>()).add(item);
        }

        String query = searchQuery == null ? "" : searchQuery.trim();
        int total = productRepository.countPublished(query);
        int totalPages = Math.max((total + PER_PAGE - 1) / PER_PAGE, 1);
        int safePage = Math.min(Math.max(page, 1), totalPages);
        int offset = (safePage - 1) * PER_PAGE;
        List<Product> products = productRepository.findCatalogPage(query, PER_PAGE, offset);

        return new HomePageData(featured, saleItems, genres, products, totalPages, safePage, query);
    }

    public Optional<Product> getProduct(long id) {
        return productRepository.findByIdWithSeller(id);
    }

    public record HomePageData(
            List<Product> featured,
            List<Product> saleItems,
            Map<String, List<Product>> genres,
            List<Product> products,
            int totalPages,
            int page,
            String searchQuery
    ) {
    }
}
