package com.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class ApiDtos {

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record AuthStatusResponse(
            boolean authenticated,
            String message,
            String cookiePolicy,
            UserProfileView user
    ) {}

    public record UserProfileView(
            Long id,
            String name,
            String email,
            String role,
            String planTier,
            String authenticatedVia
    ) {}

    public record AemHeroBanner(
            String jcrPath,
            String eyebrow,
            String title,
            String subtitle,
            String ctaLabel,
            String themeAccent,
            String authoredBy,
            String lastPublished
    ) {}

    public record AemProductEditorial(
            String jcrPath,
            String headline,
            String richDescription,
            String badge,
            List<String> highlights,
            String imageGradient,
            String imageIcon,
            String seoKeywords
    ) {}

    public record ProductView(
            Long id,
            String sku,
            String name,
            String category,
            double price,
            int stock,
            double rating,
            String aemContentPath,
            AemProductEditorial editorialContent
    ) {}

    public record StorefrontCatalogResponse(
            AemHeroBanner heroBanner,
            List<ProductView> products,
            int totalProducts,
            String dataSourceInfo
    ) {}

    public record OrderItemInput(
            @NotBlank String sku,
            @Min(1) int quantity
    ) {}

    public record CreateOrderInput(
            @NotBlank String paymentMethod,
            @NotEmpty List<OrderItemInput> items
    ) {}

    public record OrderItemView(
            String productSku,
            String productName,
            int quantity,
            double unitPrice
    ) {}

    public record OrderView(
            Long id,
            String orderCode,
            String userEmail,
            double totalAmount,
            String status,
            String paymentMethod,
            String createdAt,
            List<OrderItemView> items
    ) {}

    public record StructuredLogEntry(
            String timestamp,
            String correlationId,
            String level,
            String component,
            String message,
            int latencyMs
    ) {}

    public record TelemetrySnapshot(
            int totalRequests,
            int graphqlRequests,
            int restRequests,
            int ordersCreated,
            double avgLatencyMs,
            double p95LatencyMs,
            int activeDenyListTokens,
            List<StructuredLogEntry> recentLogs
    ) {}
}

