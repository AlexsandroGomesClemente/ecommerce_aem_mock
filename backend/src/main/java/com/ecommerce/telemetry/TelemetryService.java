package com.ecommerce.telemetry;

import com.ecommerce.dto.ApiDtos.StructuredLogEntry;
import com.ecommerce.dto.ApiDtos.TelemetrySnapshot;
import com.ecommerce.security.TokenDenyListService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TelemetryService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);
    private static final int MAX_LOG_BUFFER = 80;
    private static final int MAX_LATENCY_SAMPLES = 500;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger graphqlRequests = new AtomicInteger(0);
    private final AtomicInteger restRequests = new AtomicInteger(0);
    private final AtomicInteger ordersCreated = new AtomicInteger(0);
    private final AtomicLong totalLatencyMs = new AtomicLong(0);

    private final ConcurrentLinkedDeque<StructuredLogEntry> recentLogs = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Long> recentLatencies = new ConcurrentLinkedDeque<>();

    private final Counter graphqlCounter;
    private final Counter restCounter;
    private final Counter orderCounter;
    private final TokenDenyListService tokenDenyListService;

    public TelemetryService(MeterRegistry meterRegistry, TokenDenyListService tokenDenyListService) {
        this.tokenDenyListService = tokenDenyListService;
        this.graphqlCounter = Counter.builder("ecommerce.requests.graphql.total")
                .description("Total de operacoes GraphQL processadas pelo BFF")
                .register(meterRegistry);
        this.restCounter = Counter.builder("ecommerce.requests.rest.total")
                .description("Total de requisicoes REST processadas pela API")
                .register(meterRegistry);
        this.orderCounter = Counter.builder("ecommerce.orders.created.total")
                .description("Total de pedidos transacionais ACID gravados no PostgreSQL")
                .register(meterRegistry);
    }

    public void recordRequest(String channel, String component, String message, long latencyMs) {
        totalRequests.incrementAndGet();
        totalLatencyMs.addAndGet(Math.max(0, latencyMs));

        if ("GRAPHQL".equalsIgnoreCase(channel)) {
            graphqlRequests.incrementAndGet();
            graphqlCounter.increment();
        } else {
            restRequests.incrementAndGet();
            restCounter.increment();
        }

        recentLatencies.addLast(Math.max(0L, latencyMs));
        while (recentLatencies.size() > MAX_LATENCY_SAMPLES) {
            recentLatencies.pollFirst();
        }

        appendLog("INFO", component, message, (int) latencyMs);
    }

    public void recordOrderCreated(String orderCode, String userEmail, double totalAmount, long latencyMs) {
        ordersCreated.incrementAndGet();
        orderCounter.increment();
        appendLog(
                "AUDIT",
                "OrderService[ACID]",
                String.format("Pedido %s confirmado para %s (R$ %.2f)", orderCode, userEmail, totalAmount),
                (int) latencyMs
        );
    }

    public void appendLog(String level, String component, String message, int latencyMs) {
        String corrId = MDC.get("correlationId");
        if (corrId == null || corrId.isBlank()) {
            corrId = "SYS-" + System.nanoTime() % 100000;
        }
        StructuredLogEntry entry = new StructuredLogEntry(
                LocalDateTime.now().format(TIME_FMT),
                corrId,
                level,
                component,
                message,
                latencyMs
        );
        recentLogs.addFirst(entry);
        while (recentLogs.size() > MAX_LOG_BUFFER) {
            recentLogs.pollLast();
        }
        // Evita gargalo de I/O síncrono no stdout durante picos de milhares de requisições por segundo
        if (!"INFO".equals(level) || totalRequests.get() % 100 == 1) {
            log.info("[{}] {} ({} ms)", component, message, latencyMs);
        }
    }

    public TelemetrySnapshot getSnapshot() {
        int total = totalRequests.get();
        double avg = total > 0 ? Math.round(((double) totalLatencyMs.get() / total) * 100.0) / 100.0 : 0.0;
        double p95 = computeP95();
        List<StructuredLogEntry> logs = new ArrayList<>(recentLogs);
        if (logs.size() > 30) {
            logs = logs.subList(0, 30);
        }
        return new TelemetrySnapshot(
                total,
                graphqlRequests.get(),
                restRequests.get(),
                ordersCreated.get(),
                avg,
                p95,
                tokenDenyListService.getActiveRevokedCount(),
                logs
        );
    }

    private double computeP95() {
        List<Long> copy = new ArrayList<>(recentLatencies);
        if (copy.isEmpty()) {
            return 0.0;
        }
        Collections.sort(copy);
        int index = (int) Math.ceil(0.95 * copy.size()) - 1;
        index = Math.max(0, Math.min(index, copy.size() - 1));
        return copy.get(index);
    }
}
