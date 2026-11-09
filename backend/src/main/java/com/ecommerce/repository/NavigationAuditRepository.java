package com.ecommerce.repository;

import com.ecommerce.domain.NavigationAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NavigationAuditRepository extends JpaRepository<NavigationAuditEntity, Long> {
}

