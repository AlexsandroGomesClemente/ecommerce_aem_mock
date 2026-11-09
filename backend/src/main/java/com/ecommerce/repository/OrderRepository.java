package com.ecommerce.repository;

import com.ecommerce.domain.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByUserEmailOrderByCreatedAtDesc(String userEmail);
    List<OrderEntity> findTop25ByOrderByCreatedAtDesc();
}

