package com.nexocrm.web.dto;

import com.nexocrm.domain.ActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ActivityRequest(
        @NotNull(message = "El tipo es obligatorio") ActivityType type,
        @NotBlank(message = "El asunto es obligatorio") @Size(max = 200) String subject,
        @Size(max = 2000) String description,
        @NotNull(message = "La fecha es obligatoria") LocalDateTime dueAt,
        Long customerId,
        Long dealId,
        Long ownerId) {
}
