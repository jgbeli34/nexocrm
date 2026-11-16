package com.nexocrm.domain;

public enum CustomerStatus {
    LEAD("Lead"),
    ACTIVE("Activo"),
    INACTIVE("Inactivo");

    private final String label;

    CustomerStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
