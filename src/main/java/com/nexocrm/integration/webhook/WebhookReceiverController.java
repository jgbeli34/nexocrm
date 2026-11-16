package com.nexocrm.integration.webhook;

import com.nexocrm.repository.WebhookSubscriptionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Receptor de ejemplo ("sandbox") para probar los webhooks sin depender de un servicio externo.
 * Muestra cómo un consumidor debe verificar la firma HMAC antes de confiar en el mensaje.
 */
@RestController
@RequestMapping("/api/webhooks/receiver")
@Tag(name = "Webhooks (sandbox)", description = "Receptor sandbox para probar webhooks")
public class WebhookReceiverController {

    public record ReceivedWebhook(String event, String deliveryId, boolean signatureValid, Instant receivedAt,
                                  String payload) {
    }

    private static final int MAX = 25;
    private final Deque<ReceivedWebhook> inbox = new ConcurrentLinkedDeque<>();
    private final WebhookSubscriptionRepository subscriptions;

    public WebhookReceiverController(WebhookSubscriptionRepository subscriptions) {
        this.subscriptions = subscriptions;
    }

    @PostMapping
    @Operation(summary = "Recibe un webhook y verifica su firma")
    public ResponseEntity<Void> receive(@RequestBody String payload,
                                        @RequestHeader(value = WebhookSigner.SIGNATURE_HEADER, required = false) String signature,
                                        @RequestHeader(value = WebhookSigner.EVENT_HEADER, required = false) String event,
                                        @RequestHeader(value = WebhookSigner.DELIVERY_HEADER, required = false) String deliveryId,
                                        @RequestHeader(value = WebhookSigner.SUBSCRIPTION_HEADER, required = false) Long subscriptionId) {
        boolean valid = subscriptionId != null && subscriptions.findById(subscriptionId)
                .map(s -> WebhookSigner.verify(payload, s.getSecret(), signature))
                .orElse(false);
        inbox.addFirst(new ReceivedWebhook(event, deliveryId, valid, Instant.now(), payload));
        while (inbox.size() > MAX) {
            inbox.pollLast();
        }
        return valid ? ResponseEntity.noContent().build() : ResponseEntity.status(401).build();
    }

    @GetMapping
    @Operation(summary = "Últimos webhooks recibidos por el sandbox")
    public List<ReceivedWebhook> latest() {
        return new ArrayList<>(inbox);
    }
}
