package com.nexocrm.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Suscripción de un sistema externo (Slack, Zapier, n8n, ERP...) a un evento del CRM. */
@Entity
@Table(name = "webhook_subscriptions")
public class WebhookSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 500)
    private String targetUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WebhookEvent event;

    /** Secreto compartido para firmar los payloads con HMAC-SHA256. */
    @Column(nullable = false, length = 80)
    private String secret;

    private boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected WebhookSubscription() {
    }

    public WebhookSubscription(String name, String targetUrl, WebhookEvent event, String secret) {
        this.name = name;
        this.targetUrl = targetUrl;
        this.event = event;
        this.secret = secret;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getTargetUrl() { return targetUrl; }
    public WebhookEvent getEvent() { return event; }
    public String getSecret() { return secret; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
