package com.nexocrm.web.dto;

import com.nexocrm.domain.DealStage;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DealRequest(
        @NotBlank(message = "El título es obligatorio") @Size(max = 160) String title,
        @NotNull(message = "El cliente es obligatorio") Long customerId,
        Long ownerId,
        @NotNull(message = "El monto es obligatorio") @Positive(message = "El monto debe ser mayor que cero") BigDecimal amount,
        DealStage stage,
        @Min(0) @Max(100) Integer probability,
        LocalDate expectedCloseDate) {
}
