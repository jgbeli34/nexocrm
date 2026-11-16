package com.nexocrm.domain;

public enum LeadSource {
    WEBSITE("Sitio web"),
    REFERRAL("Referido"),
    LINKEDIN("LinkedIn"),
    EVENT("Evento"),
    COLD_OUTREACH("Prospección en frío"),
    IMPORT("Importación CSV");

    private final String label;

    LeadSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
