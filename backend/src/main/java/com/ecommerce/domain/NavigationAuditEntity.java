package com.ecommerce.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "navigation_audit")
public class NavigationAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correlation_id", nullable = false, length = 80)
    private String correlationId;

    @Column(name = "user_email", nullable = false, length = 160)
    private String userEmail;

    @Column(name = "action_type", nullable = false, length = 80)
    private String actionType;

    @Column(name = "resource_path", nullable = false, length = 255)
    private String resourcePath;

    @Column(name = "latency_ms", nullable = false)
    private Long latencyMs;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public NavigationAuditEntity() {
    }

    public NavigationAuditEntity(String correlationId, String userEmail, String actionType, String resourcePath, Long latencyMs) {
        this.correlationId = correlationId;
        this.userEmail = userEmail;
        this.actionType = actionType;
        this.resourcePath = resourcePath;
        this.latencyMs = latencyMs;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getCorrelationId() { return correlationId; }
    public String getUserEmail() { return userEmail; }
    public String getActionType() { return actionType; }
    public String getResourcePath() { return resourcePath; }
    public Long getLatencyMs() { return latencyMs; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

