package com.ecommerce.controller;

import com.ecommerce.dto.ApiDtos.*;
import com.ecommerce.security.AuthenticatedUserPrincipal;
import com.ecommerce.service.AemContentService;
import com.ecommerce.service.CatalogService;
import com.ecommerce.service.OrderService;
import com.ecommerce.telemetry.TelemetryService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Camada BFF em GraphQL que unifica dados transacionais do PostgreSQL
 * com conteúdos editoriais do Adobe Experience Manager (AEM).
 *
 * Implementa @BatchMapping (DataLoader) para eliminar o problema de "N+1 queries"
 * ao resolver o campo editorialContent de múltiplos produtos simultaneamente.
 */
@Controller
public class GraphQlEcommerceController {

    private final CatalogService catalogService;
    private final AemContentService aemContentService;
    private final OrderService orderService;
    private final TelemetryService telemetryService;

    public GraphQlEcommerceController(
            CatalogService catalogService,
            AemContentService aemContentService,
            OrderService orderService,
            TelemetryService telemetryService
    ) {
        this.catalogService = catalogService;
        this.aemContentService = aemContentService;
        this.orderService = orderService;
        this.telemetryService = telemetryService;
    }

    @QueryMapping
    public StorefrontCatalogResponse storefrontCatalog(
            @Argument String category,
            @Argument String search
    ) {
        // Passa eagerEditorial=false para que o @BatchMapping (DataLoader) resolva em lote apenas se a query pedir editorialContent!
        return catalogService.getStorefrontCatalog(category, search, false);
    }

    @QueryMapping
    public ProductView productBySku(@Argument String sku) {
        return catalogService.getProductBySku(sku, false).orElse(null);
    }

    /**
     * Padrão DataLoader via @BatchMapping.
     * Em vez de executar N chamadas separadas para buscar o Content Fragment do AEM de cada produto,
     * o Spring for GraphQL agrupa todos os ProductView da resposta e invoca este método UMA ÚNICA VEZ.
     */
    @BatchMapping(typeName = "ProductView", field = "editorialContent")
    public Map<ProductView, AemProductEditorial> editorialContent(List<ProductView> products) {
        Set<String> paths = products.stream()
                .map(ProductView::aemContentPath)
                .collect(Collectors.toSet());

        Map<String, AemProductEditorial> batchByPath = aemContentService.fetchEditorialByPathsBatch(paths);

        Map<ProductView, AemProductEditorial> result = new HashMap<>(products.size());
        for (ProductView product : products) {
            result.put(product, batchByPath.get(product.aemContentPath()));
        }
        return result;
    }

    @QueryMapping
    public List<OrderView> myOrders() {
        AuthenticatedUserPrincipal principal = currentPrincipal();
        String email = principal != null ? principal.email() : null;
        return orderService.listOrdersForUser(email);
    }

    @QueryMapping
    public UserProfileView me() {
        AuthenticatedUserPrincipal principal = currentPrincipal();
        if (principal == null) {
            return null;
        }
        return new UserProfileView(
                principal.userId(),
                principal.name(),
                principal.email(),
                principal.role(),
                principal.planTier(),
                principal.authSource()
        );
    }

    @QueryMapping
    public TelemetrySnapshot telemetrySnapshot() {
        return telemetryService.getSnapshot();
    }

    @MutationMapping
    public OrderView createOrder(@Argument CreateOrderInput input) {
        AuthenticatedUserPrincipal principal = currentPrincipal();
        if (principal == null) {
            throw new AccessDeniedException("Autenticação OAuth2/JWT (Cookie HttpOnly) obrigatória para finalizar pedido.");
        }
        return orderService.createOrder(principal.email(), input);
    }

    private AuthenticatedUserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
            return principal;
        }
        return null;
    }
}

