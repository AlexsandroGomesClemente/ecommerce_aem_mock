package com.ecommerce.repository;

import com.ecommerce.domain.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    Optional<ProductEntity> findBySku(String sku);
    List<ProductEntity> findByCategoryIgnoreCaseOrderByIdAsc(String category);
}

