package com.nexocrm.web.dto;

import com.nexocrm.domain.DealStage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StageChangeRequest(
        @NotNull(message = "La etapa es obligatoria") DealStage stage,
        @Size(max = 300) String lostReason) {
}
