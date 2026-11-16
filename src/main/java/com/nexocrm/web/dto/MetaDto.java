package com.nexocrm.web.dto;

import java.util.List;

/** Catálogos (enums con etiquetas en español) para que el frontend no los duplique. */
public record MetaDto(
        List<Option> stages,
        List<Option> customerStatuses,
        List<Option> leadSources,
        List<Option> activityTypes,
        List<Option> webhookEvents,
        List<SalesRepDto> salesReps) {

    public record Option(String value, String label, Integer probability) {
    }
}
