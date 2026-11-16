package com.nexocrm.domain;

public enum ActivityType {
    CALL("Llamada"),
    EMAIL("Correo"),
    MEETING("Reunión"),
    TASK("Tarea");

    private final String label;

    ActivityType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
