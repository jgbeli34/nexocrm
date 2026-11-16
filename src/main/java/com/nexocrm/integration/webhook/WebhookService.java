package com.nexocrm.integration.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexocrm.config.NexoCrmProperties;
import com.nexocrm.domain.WebhookDelivery;
import com.nexocrm.domain.WebhookEvent;
import com.nexocrm.domain.WebhookSubscription;
import com.nexocrm.integration.webhook.WebhookDtos.DeliveryDto;
import com.nexocrm.integration.webhook.WebhookDtos.SubscriptionDto;
import com.nexocrm.integration.webhook.WebhookDtos.SubscriptionRequest;
import com.nexocrm.repository.WebhookDeliveryRepository;
import com.nexocrm.repository.WebhookSubscriptionRepository;
import com.nexocrm.service.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Webhooks salientes: cuando ocurre un evento de negocio (p. ej. se gana un negocio)
 * se envía un POST firmado a cada URL suscrita. Así el CRM se integra con Slack,
 * Zapier, n8n, un ERP o cualquier sistema que reciba HTTP.
 */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final WebhookSubscriptionRepository subscriptions;
    private final WebhookDeliveryRepository deliveries;
    private final ObjectMapper mapper;
    private final RestClient client;
    private final int maxAttempts;

    public WebhookService(WebhookSubscriptionRepository subscriptions, WebhookDeliveryRepository deliveries,
                          ObjectMapper mapper, RestClient.Builder builder, NexoCrmProperties properties) {
        this.subscriptions = subscriptions;
        this.deliveries = deliveries;
        this.mapper = mapper;
        var factory = new SimpleClientHttpRequestFactory();
        int timeout = (int) properties.webhooks().timeout().toMillis();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        this.client = builder.requestFactory(factory).build();
        this.maxAttempts = Math.max(1, properties.webhooks().maxAttempts());
    }

    // ---- Gestión de suscripciones ----

    @Transactional(readOnly = true)
    public List<SubscriptionDto> list() {
        return subscriptions.findAll().stream().map(s -> SubscriptionDto.from(s, false)).toList();
    }

    @Transactional
    public SubscriptionDto create(SubscriptionRequest request) {
        var subscription = new WebhookSubscription(request.name().trim(), request.targetUrl().trim(),
                request.event(), WebhookSigner.newSecret());
        return SubscriptionDto.from(subscriptions.save(subscription), true);
    }

    @Transactional
    public SubscriptionDto toggle(Long id) {
        WebhookSubscription s = get(id);
        s.setActive(!s.isActive());
        return SubscriptionDto.from(s, false);
    }

    @Transactional
    public void delete(Long id) {
        WebhookSubscription s = get(id);
        deliveries.deleteBySubscriptionId(id);
        subscriptions.delete(s);
    }

    @Transactional(readOnly = true)
    public List<DeliveryDto> recentDeliveries() {
        return deliveries.findTop30ByOrderByDeliveredAtDesc().stream().map(DeliveryDto::from).toList();
    }

    /** Envía un evento de ejemplo para comprobar la conexión con el sistema externo. */
    public DeliveryDto sendTest(Long id) {
        WebhookSubscription s = subscriptions.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook", id));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", "Evento de prueba enviado desde NexoCRM");
        data.put("subscription", s.getName());
        return DeliveryDto.from(deliver(s, s.getEvent(), data, true));
    }

    // ---- Entrega ----

    /** Envía el evento a todas las suscripciones activas. */
    public void publish(WebhookEvent event, Object data) {
        for (WebhookSubscription s : subscriptions.findByEventAndActiveTrue(event)) {
            deliver(s, event, data, false);
        }
    }

    private WebhookDelivery deliver(WebhookSubscription s, WebhookEvent event, Object data, boolean test) {
        String deliveryId = UUID.randomUUID().toString();
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("id", deliveryId);
        envelope.put("event", event.name());
        envelope.put("test", test);
        envelope.put("occurredAt", Instant.now().toString());
        envelope.put("data", data);

        String payload;
        try {
            payload = mapper.writeValueAsString(envelope);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el webhook", e);
        }

        long start = System.nanoTime();
        Integer status = null;
        String error = null;
        int attempt = 0;
        boolean success = false;
        while (attempt < maxAttempts && !success) {
            attempt++;
            try {
                var response = client.post()
                        .uri(s.getTargetUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(WebhookSigner.SIGNATURE_HEADER, WebhookSigner.sign(payload, s.getSecret()))
                        .header(WebhookSigner.EVENT_HEADER, event.name())
                        .header(WebhookSigner.DELIVERY_HEADER, deliveryId)
                        .header(WebhookSigner.SUBSCRIPTION_HEADER, String.valueOf(s.getId()))
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();
                status = response.getStatusCode().value();
                success = true;
                error = null;
            } catch (RestClientResponseException e) {
                status = e.getStatusCode().value();
                error = "HTTP " + status;
                if (status < 500) {
                    break; // errores 4xx no se reintentan
                }
            } catch (Exception e) {
                error = e.getClass().getSimpleName() + ": " + e.getMessage();
            }
            if (!success && attempt < maxAttempts) {
                sleep(250L * attempt);
            }
        }
        long duration = (System.nanoTime() - start) / 1_000_000;
        if (!success) {
            log.warn("Webhook {} ({}) falló tras {} intento(s): {}", s.getName(), event, attempt, error);
        }
        String truncatedError = error != null && error.length() > 480 ? error.substring(0, 480) : error;
        return deliveries.save(new WebhookDelivery(s, event, payload, status, success, attempt,
                truncatedError, duration, LocalDateTime.now()));
    }

    private WebhookSubscription get(Long id) {
        return subscriptions.findById(id).orElseThrow(() -> new ResourceNotFoundException("Webhook", id));
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
