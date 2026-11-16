package com.nexocrm.web.dto;

import com.nexocrm.domain.Customer;
import com.nexocrm.domain.CustomerStatus;
import com.nexocrm.domain.LeadSource;

import java.time.LocalDateTime;

public record CustomerDto(
        Long id,
        String companyName,
        String contactName,
        String email,
        String phone,
        String industry,
        String city,
        String country,
        CustomerStatus status,
        String statusLabel,
        LeadSource source,
        String sourceLabel,
        Long ownerId,
        String ownerName,
        String notes,
        LocalDateTime createdAt) {

    public static CustomerDto from(Customer c) {
        return new CustomerDto(
                c.getId(), c.getCompanyName(), c.getContactName(), c.getEmail(), c.getPhone(),
                c.getIndustry(), c.getCity(), c.getCountry(),
                c.getStatus(), c.getStatus().getLabel(),
                c.getSource(), c.getSource().getLabel(),
                c.getOwner() != null ? c.getOwner().getId() : null,
                c.getOwner() != null ? c.getOwner().getName() : null,
                c.getNotes(), c.getCreatedAt());
    }
}
