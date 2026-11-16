package com.nexocrm.web.dto;

import com.nexocrm.domain.CustomerStatus;
import com.nexocrm.domain.LeadSource;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank(message = "La empresa es obligatoria") @Size(max = 160) String companyName,
        @NotBlank(message = "El contacto es obligatorio") @Size(max = 120) String contactName,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no es válido") String email,
        @Size(max = 40) String phone,
        @Size(max = 80) String industry,
        @Size(max = 80) String city,
        @Size(max = 80) String country,
        @NotNull(message = "El estado es obligatorio") CustomerStatus status,
        LeadSource source,
        Long ownerId,
        @Size(max = 2000) String notes) {
}
