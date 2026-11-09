package com.ecommerce.telemetry;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_HEADER = "X-Correlation-ID";
    private final TelemetryService telemetryService;

    public CorrelationIdFilter(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = "req-" + UUID.randomUUID().toString().substring(0, 8);
        }

        MDC.put("correlationId", correlationId);
        response.setHeader(CORRELATION_HEADER, correlationId);

        long startNanos = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
            String uri = request.getRequestURI();
            if (!uri.startsWith("/actuator") && !uri.contains("/telemetry")) {
                String channel = uri.startsWith("/graphql") ? "GRAPHQL" : "REST";
                telemetryService.recordRequest(
                        channel,
                        channel + ":" + request.getMethod(),
                        uri + " -> HTTP " + response.getStatus(),
                        elapsedMs
                );
            }
            MDC.remove("correlationId");
        }
    }
}

