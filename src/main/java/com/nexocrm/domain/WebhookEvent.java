package com.nexocrm.domain;

/** Eventos de negocio que pueden notificarse a sistemas externos vía webhook. */
public enum WebhookEvent {
    DEAL_WON("Negocio ganado"),
    DEAL_LOST("Negocio perdido"),
    DEAL_STAGE_CHANGED("Cambio de etapa"),
    CUSTOMER_CREATED("Cliente creado");

    private final String label;

    WebhookEvent(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
