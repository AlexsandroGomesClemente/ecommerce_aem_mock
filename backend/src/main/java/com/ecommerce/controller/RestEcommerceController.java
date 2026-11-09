package com.ecommerce.controller;

import com.ecommerce.dto.ApiDtos.*;
import com.ecommerce.security.AuthenticatedUserPrincipal;
import com.ecommerce.service.AemContentService;
import com.ecommerce.service.CatalogService;
import com.ecommerce.service.OrderService;
import com.ecommerce.telemetry.TelemetryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoints REST complementares ao GraphQL, atendendo ao requisito de consumo
 * híbrido ("CONSUMO -> VIA GRAPHQL e API").
 */
@RestController
@RequestMapping("/api/v1")
public class RestEcommerceController {

    private final CatalogService catalogService;
    private final OrderService orderService;
    private final AemContentService aemContentService;
    private final TelemetryService telemetryService;

    public RestEcommerceController(
            CatalogService catalogService,
            OrderService orderService,
            AemContentService aemContentService,
            TelemetryService telemetryService
    ) {
        this.catalogService = catalogService;
        this.orderService = orderService;
        this.aemContentService = aemContentService;
        this.telemetryService = telemetryService;
    }

    @GetMapping("/products")
    public ResponseEntity<StorefrontCatalogResponse> listProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(catalogService.getStorefrontCatalog(category, search, true));
    }

    @GetMapping("/products/{sku}")
    public ResponseEntity<ProductView> getProductBySku(@PathVariable String sku) {
        return catalogService.getProductBySku(sku, true)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/aem/hero-banner")
    public ResponseEntity<AemHeroBanner> getHeroBanner() {
        return ResponseEntity.ok(aemContentService.getStorefrontHeroBanner());
    }

    @GetMapping("/aem/clientlibs-status")
    public ResponseEntity<Map<String, Object>> getAemClientLibsStatus() {
        return ResponseEntity.ok(Map.of(
                "clientLibCategory", "ecommerce.site",
                "jcrRootPath", "/apps/ecommerce/clientlibs/clientlib-site",
                "bundler", "Vite 5 -> AEM ClientLib XML Generator",
                "dependencies", List.of("aem.core.wcm.components.all"),
                "status", "ACTIVE"
        ));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderView>> listMyOrders() {
        AuthenticatedUserPrincipal principal = currentPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(orderService.listOrdersForUser(principal.email()));
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderView> createOrderRest(@Valid @RequestBody CreateOrderInput input) {
        AuthenticatedUserPrincipal principal = currentPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        OrderView created = orderService.createOrder(principal.email(), input);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/telemetry")
    public ResponseEntity<TelemetrySnapshot> getTelemetry() {
        return ResponseEntity.ok(telemetryService.getSnapshot());
    }

    private AuthenticatedUserPrincipal currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
            return principal;
        }
        return null;
    }
}

