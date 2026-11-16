package com.nexocrm.integration.webhook;

import com.nexocrm.domain.DealStage;
import com.nexocrm.domain.WebhookEvent;
import com.nexocrm.service.event.CustomerCreatedEvent;
import com.nexocrm.service.event.DealStageChangedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce eventos de dominio en webhooks. Se ejecuta de forma asíncrona y solo
 * después de que la transacción se confirma, para no notificar cambios que luego se revierten
 * ni hacer esperar al usuario por un sistema externo lento.
 */
@Component
public class WebhookEventListener {

    private final WebhookService webhooks;
    private final WebhookPayloadFactory payloads;

    public WebhookEventListener(WebhookService webhooks, WebhookPayloadFactory payloads) {
        this.webhooks = webhooks;
        this.payloads = payloads;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDealStageChanged(DealStageChangedEvent event) {
        payloads.deal(event.dealId()).ifPresent(deal -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("from", event.from().name());
            data.put("to", event.to().name());
            data.put("deal", deal);
            webhooks.publish(WebhookEvent.DEAL_STAGE_CHANGED, data);
            if (event.to() == DealStage.WON) {
                webhooks.publish(WebhookEvent.DEAL_WON, deal);
            } else if (event.to() == DealStage.LOST) {
                webhooks.publish(WebhookEvent.DEAL_LOST, deal);
            }
        });
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCustomerCreated(CustomerCreatedEvent event) {
        payloads.customer(event.customerId()).ifPresent(c -> webhooks.publish(WebhookEvent.CUSTOMER_CREATED, c));
    }
}
