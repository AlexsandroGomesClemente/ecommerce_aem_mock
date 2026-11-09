package com.ecommerce.service;

import com.ecommerce.domain.NavigationAuditEntity;
import com.ecommerce.domain.OrderEntity;
import com.ecommerce.domain.OrderItemEntity;
import com.ecommerce.domain.ProductEntity;
import com.ecommerce.dto.ApiDtos.CreateOrderInput;
import com.ecommerce.dto.ApiDtos.OrderItemInput;
import com.ecommerce.dto.ApiDtos.OrderItemView;
import com.ecommerce.dto.ApiDtos.OrderView;
import com.ecommerce.repository.NavigationAuditRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.telemetry.TelemetryService;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Camada de serviço transacional (ACID) para criação de pedidos,
 * atualização atômica de estoque com lock otimista (@Version) e registro de auditoria no PostgreSQL.
 */
@Service
public class OrderService {

    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final NavigationAuditRepository navigationAuditRepository;
    private final TelemetryService telemetryService;
    private final CatalogService catalogService;

    public OrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            NavigationAuditRepository navigationAuditRepository,
            TelemetryService telemetryService,
            CatalogService catalogService
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.navigationAuditRepository = navigationAuditRepository;
        this.telemetryService = telemetryService;
        this.catalogService = catalogService;
    }

    @Transactional
    public OrderView createOrder(String userEmail, CreateOrderInput input) {
        long startNanos = System.nanoTime();

        if (input == null || input.items() == null || input.items().isEmpty()) {
            throw new IllegalArgumentException("O pedido precisa conter pelo menos 1 item.");
        }

        OrderEntity order = new OrderEntity();
        order.setOrderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setUserEmail(userEmail != null && !userEmail.isBlank() ? userEmail : "alex@ecommerce.com");
        order.setPaymentMethod(input.paymentMethod() != null ? input.paymentMethod() : "PIX_INSTANTANEO");
        order.setStatus("PAID_ACID_CONFIRMED");

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemInput itemInput : input.items()) {
            if (itemInput.quantity() <= 0) {
                throw new IllegalArgumentException("Quantidade inválida para o SKU: " + itemInput.sku());
            }

            ProductEntity product = productRepository.findBySku(itemInput.sku())
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado no PostgreSQL: " + itemInput.sku()));

            if (product.getStock() < itemInput.quantity()) {
                throw new IllegalStateException("Estoque insuficiente para o produto " + product.getName());
            }

            product.setStock(product.getStock() - itemInput.quantity());
            productRepository.save(product);

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(itemInput.quantity()));
            total = total.add(lineTotal);

            OrderItemEntity orderItem = new OrderItemEntity(
                    product.getSku(),
                    product.getName(),
                    itemInput.quantity(),
                    product.getPrice()
            );
            order.addItem(orderItem);
        }

        order.setTotalAmount(total);
        OrderEntity savedOrder = orderRepository.save(order);
        catalogService.evictCatalogCache();

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
        String corrId = MDC.get("correlationId");
        navigationAuditRepository.save(new NavigationAuditEntity(
                corrId != null ? corrId : "TX-ACID",
                order.getUserEmail(),
                "CREATE_ORDER_ACID",
                "/orders/" + savedOrder.getOrderCode(),
                elapsedMs
        ));

        telemetryService.recordOrderCreated(
                savedOrder.getOrderCode(),
                savedOrder.getUserEmail(),
                savedOrder.getTotalAmount().doubleValue(),
                elapsedMs
        );

        return toOrderView(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderView> listOrdersForUser(String userEmail) {
        List<OrderEntity> orders = (userEmail != null && !userEmail.isBlank())
                ? orderRepository.findByUserEmailOrderByCreatedAtDesc(userEmail)
                : orderRepository.findTop25ByOrderByCreatedAtDesc();
        return orders.stream().map(this::toOrderView).toList();
    }

    private OrderView toOrderView(OrderEntity entity) {
        List<OrderItemView> itemViews = entity.getItems().stream()
                .map(i -> new OrderItemView(
                        i.getProductSku(),
                        i.getProductName(),
                        i.getQuantity(),
                        i.getUnitPrice().doubleValue()
                ))
                .toList();

        return new OrderView(
                entity.getId(),
                entity.getOrderCode(),
                entity.getUserEmail(),
                entity.getTotalAmount().doubleValue(),
                entity.getStatus(),
                entity.getPaymentMethod(),
                entity.getCreatedAt().format(ISO_FMT),
                itemViews
        );
    }
}
