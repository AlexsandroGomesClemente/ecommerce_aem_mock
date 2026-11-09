package com.ecommerce.service;

import com.ecommerce.domain.ProductEntity;
import com.ecommerce.dto.ApiDtos.AemProductEditorial;
import com.ecommerce.dto.ApiDtos.ProductView;
import com.ecommerce.dto.ApiDtos.StorefrontCatalogResponse;
import com.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CatalogService {

    private final ProductRepository productRepository;
    private final AemContentService aemContentService;
    private final Map<String, StorefrontCatalogResponse> catalogReadCache = new ConcurrentHashMap<>();

    public CatalogService(ProductRepository productRepository, AemContentService aemContentService) {
        this.productRepository = productRepository;
        this.aemContentService = aemContentService;
    }

    private String normalize(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    /**
     * Cache inteligente na camada de serviço do BFF GraphQL/REST,
     * invalidado automaticamente em qualquer mutação transacional de estoque (OrderService).
     */
    public StorefrontCatalogResponse getStorefrontCatalog(String category, String search, boolean eagerEditorial) {
        String normCat = normalize(category);
        String normSearch = normalize(search);
        String cacheKey = (normCat.isEmpty() || "todos".equals(normCat) ? "ALL" : normCat)
                + "|" + normSearch
                + "|" + eagerEditorial;

        return catalogReadCache.computeIfAbsent(cacheKey, key -> loadCatalogFromDatabase(category, search, eagerEditorial));
    }

    public void evictCatalogCache() {
        catalogReadCache.clear();
    }

    @Transactional(readOnly = true)
    public StorefrontCatalogResponse loadCatalogFromDatabase(String category, String search, boolean eagerEditorial) {
        List<ProductEntity> entities = productRepository.findAll().stream()
                .sorted(Comparator.comparing(ProductEntity::getId))
                .toList();

        if (category != null && !category.isBlank() && !"TODOS".equalsIgnoreCase(category)) {
            String normCat = normalize(category);
            entities = entities.stream()
                    .filter(p -> normalize(p.getCategory()).equals(normCat))
                    .toList();
        }

        if (search != null && !search.isBlank()) {
            String normTerm = normalize(search);
            entities = entities.stream()
                    .filter(p -> normalize(p.getName()).contains(normTerm)
                            || normalize(p.getSku()).contains(normTerm)
                            || normalize(p.getCategory()).contains(normTerm))
                    .toList();
        }

        List<ProductView> products = entities.stream()
                .map(entity -> toProductView(entity, eagerEditorial))
                .toList();

        return new StorefrontCatalogResponse(
                aemContentService.getStorefrontHeroBanner(),
                products,
                products.size(),
                "PostgreSQL 16 (ACID Transacional) + AEM JCR Content Fragments (Batch DataLoader)"
        );
    }

    @Transactional(readOnly = true)
    public Optional<ProductView> getProductBySku(String sku, boolean eagerEditorial) {
        return productRepository.findBySku(sku)
                .map(entity -> toProductView(entity, eagerEditorial));
    }

    private ProductView toProductView(ProductEntity entity, boolean eagerEditorial) {
        AemProductEditorial editorial = eagerEditorial
                ? aemContentService.getEditorialByPath(entity.getAemContentPath())
                : null;

        return new ProductView(
                entity.getId(),
                entity.getSku(),
                entity.getName(),
                entity.getCategory(),
                entity.getPrice().doubleValue(),
                entity.getStock(),
                entity.getRating(),
                entity.getAemContentPath(),
                editorial
        );
    }
}
