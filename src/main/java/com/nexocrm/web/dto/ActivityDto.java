package com.nexocrm.web.dto;

import com.nexocrm.domain.Activity;
import com.nexocrm.domain.ActivityType;

import java.time.LocalDateTime;

public record ActivityDto(
        Long id,
        ActivityType type,
        String typeLabel,
        String subject,
        String description,
        LocalDateTime dueAt,
        boolean completed,
        LocalDateTime completedAt,
        boolean overdue,
        Long customerId,
        String customerName,
        Long dealId,
        String dealTitle,
        Long ownerId,
        String ownerName) {

    public static ActivityDto from(Activity a) {
        boolean overdue = !a.isCompleted() && a.getDueAt().isBefore(LocalDateTime.now());
        return new ActivityDto(
                a.getId(), a.getType(), a.getType().getLabel(), a.getSubject(), a.getDescription(),
                a.getDueAt(), a.isCompleted(), a.getCompletedAt(), overdue,
                a.getCustomer() != null ? a.getCustomer().getId() : null,
                a.getCustomer() != null ? a.getCustomer().getCompanyName() : null,
                a.getDeal() != null ? a.getDeal().getId() : null,
                a.getDeal() != null ? a.getDeal().getTitle() : null,
                a.getOwner() != null ? a.getOwner().getId() : null,
                a.getOwner() != null ? a.getOwner().getName() : null);
    }
}
