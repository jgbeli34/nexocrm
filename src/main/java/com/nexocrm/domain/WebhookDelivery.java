package com.nexocrm.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Registro de auditoría de cada intento de entrega de un webhook. */
@Entity
@Table(name = "webhook_deliveries")
public class WebhookDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private WebhookSubscription subscription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WebhookEvent event;

    // varchar largo en vez de @Lob: portable entre H2 y PostgreSQL (evita columnas oid)
    @Column(nullable = false, length = 20000)
    private String payload;

    private Integer statusCode;

    private boolean success;

    private int attempts;

    @Column(length = 500)
    private String errorMessage;

    private long durationMs;

    @Column(nullable = false)
    private LocalDateTime deliveredAt;

    protected WebhookDelivery() {
    }

    public WebhookDelivery(WebhookSubscription subscription, WebhookEvent event, String payload,
                           Integer statusCode, boolean success, int attempts, String errorMessage,
                           long durationMs, LocalDateTime deliveredAt) {
        this.subscription = subscription;
        this.event = event;
        this.payload = payload;
        this.statusCode = statusCode;
        this.success = success;
        this.attempts = attempts;
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
        this.deliveredAt = deliveredAt;
    }

    public Long getId() { return id; }
    public WebhookSubscription getSubscription() { return subscription; }
    public WebhookEvent getEvent() { return event; }
    public String getPayload() { return payload; }
    public Integer getStatusCode() { return statusCode; }
    public boolean isSuccess() { return success; }
    public int getAttempts() { return attempts; }
    public String getErrorMessage() { return errorMessage; }
    public long getDurationMs() { return durationMs; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
}
