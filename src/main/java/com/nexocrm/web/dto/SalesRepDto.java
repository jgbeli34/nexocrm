package com.nexocrm.web.dto;

import com.nexocrm.domain.SalesRep;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Collectors;

public record SalesRepDto(Long id, String name, String email, String initials, BigDecimal monthlyQuota) {

    public static SalesRepDto from(SalesRep rep) {
        return new SalesRepDto(rep.getId(), rep.getName(), rep.getEmail(), initials(rep.getName()), rep.getMonthlyQuota());
    }

    public static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        return Arrays.stream(name.trim().split("\\s+"))
                .limit(2)
                .map(p -> p.substring(0, 1).toUpperCase())
                .collect(Collectors.joining());
    }
}
