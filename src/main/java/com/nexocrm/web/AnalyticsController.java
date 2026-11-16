package com.nexocrm.web;

import com.nexocrm.domain.*;
import com.nexocrm.service.AnalyticsService;
import com.nexocrm.service.SalesRepService;
import com.nexocrm.web.dto.DashboardDto;
import com.nexocrm.web.dto.MetaDto;
import com.nexocrm.web.dto.MetaDto.Option;
import com.nexocrm.web.dto.SalesRepDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Analítica y catálogos")
public class AnalyticsController {

    private final AnalyticsService analytics;
    private final SalesRepService salesReps;

    public AnalyticsController(AnalyticsService analytics, SalesRepService salesReps) {
        this.analytics = analytics;
        this.salesReps = salesReps;
    }

    @GetMapping("/analytics/dashboard")
    @Operation(summary = "KPIs, ventas por mes, embudo, ranking de vendedores y pronóstico")
    public DashboardDto dashboard() {
        return analytics.dashboard();
    }

    @GetMapping("/sales-reps")
    public List<SalesRepDto> salesReps() {
        return salesReps.findAll();
    }

    @GetMapping("/meta")
    @Operation(summary = "Catálogos con etiquetas en español para construir formularios")
    public MetaDto meta() {
        return new MetaDto(
                Arrays.stream(DealStage.values()).map(s -> new Option(s.name(), s.getLabel(), s.getDefaultProbability())).toList(),
                Arrays.stream(CustomerStatus.values()).map(s -> new Option(s.name(), s.getLabel(), null)).toList(),
                Arrays.stream(LeadSource.values()).map(s -> new Option(s.name(), s.getLabel(), null)).toList(),
                Arrays.stream(ActivityType.values()).map(s -> new Option(s.name(), s.getLabel(), null)).toList(),
                Arrays.stream(WebhookEvent.values()).map(s -> new Option(s.name(), s.getLabel(), null)).toList(),
                salesReps.findAll());
    }
}
