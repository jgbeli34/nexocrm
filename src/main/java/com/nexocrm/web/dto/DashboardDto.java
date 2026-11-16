package com.nexocrm.web.dto;

import java.math.BigDecimal;
import java.util.List;

/** Todo lo que necesita el panel analítico en una sola respuesta. */
public record DashboardDto(
        Kpis kpis,
        List<MonthlyRevenue> revenueByMonth,
        List<StageSummary> pipelineByStage,
        List<RepPerformance> leaderboard,
        List<Breakdown> wonByIndustry,
        List<SourceConversion> conversionBySource,
        List<ForecastMonth> forecast) {

    public record Kpis(
            BigDecimal revenueWon,
            BigDecimal revenueThisMonth,
            /** Ingresos del mes anterior hasta el mismo día del mes (comparación MTD). */
            BigDecimal revenueLastMonth,
            Double monthOverMonthGrowth,
            BigDecimal openPipeline,
            BigDecimal weightedPipeline,
            double winRate,
            BigDecimal averageDealSize,
            double averageSalesCycleDays,
            long openDeals,
            long activeCustomers,
            long overdueActivities) {
    }

    public record MonthlyRevenue(String month, String label, BigDecimal revenue, long dealsWon) {
    }

    public record StageSummary(String stage, String label, long count, BigDecimal amount, BigDecimal weighted) {
    }

    public record RepPerformance(Long id, String name, String initials, BigDecimal revenueWon,
                                 BigDecimal revenueThisMonth, BigDecimal monthlyQuota, double quotaAttainment,
                                 long dealsWon, double winRate) {
    }

    public record Breakdown(String label, BigDecimal amount, long count) {
    }

    public record SourceConversion(String source, String label, long total, long won, double conversionRate) {
    }

    public record ForecastMonth(String month, String label, BigDecimal weighted, BigDecimal bestCase) {
    }
}
