package com.nexocrm.integration.webhook;

import com.nexocrm.domain.WebhookDelivery;
import com.nexocrm.domain.WebhookEvent;
import com.nexocrm.domain.WebhookSubscription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class WebhookDtos {

    private WebhookDtos() {
    }

    public record SubscriptionRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String name,
            @NotBlank(message = "La URL es obligatoria")
            @Pattern(regexp = "^https?://.+", message = "La URL debe empezar por http:// o https://")
            @Size(max = 500) String targetUrl,
            @NotNull(message = "El evento es obligatorio") WebhookEvent event) {
    }

    /** {@code secret} solo se devuelve completo al crear la suscripción. */
    public record SubscriptionDto(Long id, String name, String targetUrl, WebhookEvent event, String eventLabel,
                                  boolean active, String secret, LocalDateTime createdAt) {

        static SubscriptionDto from(WebhookSubscription s, boolean revealSecret) {
            String secret = revealSecret ? s.getSecret()
                    : s.getSecret().substring(0, 10) + "••••••••";
            return new SubscriptionDto(s.getId(), s.getName(), s.getTargetUrl(), s.getEvent(),
                    s.getEvent().getLabel(), s.isActive(), secret, s.getCreatedAt());
        }
    }

    public record DeliveryDto(Long id, Long subscriptionId, String subscriptionName, WebhookEvent event,
                              String eventLabel, Integer statusCode, boolean success, int attempts,
                              String errorMessage, long durationMs, LocalDateTime deliveredAt, String payload) {

        static DeliveryDto from(WebhookDelivery d) {
            return new DeliveryDto(d.getId(), d.getSubscription().getId(), d.getSubscription().getName(),
                    d.getEvent(), d.getEvent().getLabel(), d.getStatusCode(), d.isSuccess(), d.getAttempts(),
                    d.getErrorMessage(), d.getDurationMs(), d.getDeliveredAt(), d.getPayload());
        }
    }
}
