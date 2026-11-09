package com.ecommerce.service;

import com.ecommerce.domain.ProductEntity;
import com.ecommerce.dto.ApiDtos.AemProductEditorial;
import com.ecommerce.dto.ApiDtos.ProductView;
import com.ecommerce.dto.ApiDtos.StorefrontCatalogResponse;
import com.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * ADR-003: Cache inteligente na camada de serviço do BFF GraphQL/REST,
     * invalidado automaticamente em qualquer mutação transacional de estoque (OrderService).
     */
    @Transactional(readOnly = true)
    public StorefrontCatalogResponse getStorefrontCatalog(String category, String search, boolean eagerEditorial) {
        String cacheKey = (category == null ? "ALL" : category.trim().toUpperCase(Locale.ROOT))
                + "|" + (search == null ? "" : search.trim().toLowerCase(Locale.ROOT))
                + "|" + eagerEditorial;

        return catalogReadCache.computeIfAbsent(cacheKey, key -> loadCatalogFromDatabase(category, search, eagerEditorial));
    }

    public void evictCatalogCache() {
        catalogReadCache.clear();
    }

    private StorefrontCatalogResponse loadCatalogFromDatabase(String category, String search, boolean eagerEditorial) {
        List<ProductEntity> entities;
        if (category != null && !category.isBlank() && !"TODOS".equalsIgnoreCase(category)) {
            entities = productRepository.findByCategoryIgnoreCaseOrderByIdAsc(category.trim());
        } else {
            entities = productRepository.findAll().stream()
                    .sorted(Comparator.comparing(ProductEntity::getId))
                    .toList();
        }

        if (search != null && !search.isBlank()) {
            String term = search.trim().toLowerCase(Locale.ROOT);
            entities = entities.stream()
                    .filter(p -> p.getName().toLowerCase(Locale.ROOT).contains(term)
                            || p.getSku().toLowerCase(Locale.ROOT).contains(term)
                            || p.getCategory().toLowerCase(Locale.ROOT).contains(term))
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
