package com.nexocrm.service.event;

import com.nexocrm.domain.DealStage;

/** Se publica cuando una oportunidad cambia de etapa (incluye ganar/perder). */
public record DealStageChangedEvent(Long dealId, DealStage from, DealStage to) {
}
