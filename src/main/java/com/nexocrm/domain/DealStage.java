package com.nexocrm.domain;

/**
 * Etapas del embudo de ventas. Cada etapa tiene una probabilidad de cierre
 * por defecto que se usa para calcular el pipeline ponderado (forecast).
 */
public enum DealStage {
    LEAD("Prospecto", 10),
    QUALIFIED("Calificado", 25),
    PROPOSAL("Propuesta", 50),
    NEGOTIATION("Negociación", 75),
    WON("Ganado", 100),
    LOST("Perdido", 0);

    private final String label;
    private final int defaultProbability;

    DealStage(String label, int defaultProbability) {
        this.label = label;
        this.defaultProbability = defaultProbability;
    }

    public String getLabel() {
        return label;
    }

    public int getDefaultProbability() {
        return defaultProbability;
    }

    public boolean isClosed() {
        return this == WON || this == LOST;
    }

    public boolean isOpen() {
        return !isClosed();
    }
}
