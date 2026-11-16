package com.nexocrm.web.dto;

import com.nexocrm.domain.Deal;
import com.nexocrm.domain.DealStage;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DealDto(
        Long id,
        String title,
        Long customerId,
        String customerName,
        Long ownerId,
        String ownerName,
        String ownerInitials,
        BigDecimal amount,
        DealStage stage,
        String stageLabel,
        int probability,
        BigDecimal weightedAmount,
        LocalDate expectedCloseDate,
        LocalDateTime createdAt,
        LocalDateTime stageChangedAt,
        long daysInStage,
        LocalDateTime closedAt,
        String lostReason) {

    public static DealDto from(Deal d) {
        LocalDateTime since = d.getStageChangedAt() != null ? d.getStageChangedAt() : d.getCreatedAt();
        LocalDateTime until = d.getClosedAt() != null ? d.getClosedAt() : LocalDateTime.now();
        long days = since == null ? 0 : Math.max(0, Duration.between(since, until).toDays());
        return new DealDto(
                d.getId(), d.getTitle(),
                d.getCustomer().getId(), d.getCustomer().getCompanyName(),
                d.getOwner() != null ? d.getOwner().getId() : null,
                d.getOwner() != null ? d.getOwner().getName() : null,
                d.getOwner() != null ? SalesRepDto.initials(d.getOwner().getName()) : null,
                d.getAmount(), d.getStage(), d.getStage().getLabel(), d.getProbability(),
                d.weightedAmount(), d.getExpectedCloseDate(), d.getCreatedAt(), d.getStageChangedAt(),
                days, d.getClosedAt(), d.getLostReason());
    }
}
