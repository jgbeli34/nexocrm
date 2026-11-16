package com.nexocrm.service;

import com.nexocrm.domain.*;
import com.nexocrm.repository.ActivityRepository;
import com.nexocrm.repository.CustomerRepository;
import com.nexocrm.repository.DealRepository;
import com.nexocrm.repository.SalesRepRepository;
import com.nexocrm.web.dto.DashboardDto;
import com.nexocrm.web.dto.DashboardDto.*;
import com.nexocrm.web.dto.SalesRepDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Calcula los indicadores comerciales del panel. La lógica de cálculo vive en
 * {@link #buildDashboard} como función pura para poder probarla sin base de datos.
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final Locale ES = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yy", ES);
    private static final List<DealStage> OPEN_STAGES =
            Arrays.stream(DealStage.values()).filter(DealStage::isOpen).toList();

    private final DealRepository deals;
    private final SalesRepRepository salesReps;
    private final CustomerRepository customers;
    private final ActivityRepository activities;

    public AnalyticsService(DealRepository deals, SalesRepRepository salesReps,
                            CustomerRepository customers, ActivityRepository activities) {
        this.deals = deals;
        this.salesReps = salesReps;
        this.customers = customers;
        this.activities = activities;
    }

    public DashboardDto dashboard() {
        LocalDateTime now = LocalDateTime.now();
        long overdue = activities.search(false, null).stream().filter(a -> a.getDueAt().isBefore(now)).count();
        return buildDashboard(
                deals.findAllWithRelations(),
                salesReps.findAll(),
                customers.countByStatus(CustomerStatus.ACTIVE),
                overdue,
                LocalDate.now());
    }

    /** Función pura: mismos datos de entrada ⇒ mismo panel. */
    public static DashboardDto buildDashboard(List<Deal> all, List<SalesRep> reps, long activeCustomers,
                                              long overdueActivities, LocalDate today) {
        YearMonth thisMonth = YearMonth.from(today);
        List<Deal> won = all.stream().filter(d -> d.getStage() == DealStage.WON && d.getClosedAt() != null).toList();
        List<Deal> lost = all.stream().filter(d -> d.getStage() == DealStage.LOST).toList();
        List<Deal> open = all.stream().filter(d -> d.getStage().isOpen()).toList();

        BigDecimal revenueThisMonth = sum(won, inMonth(thisMonth));
        // Comparación justa a mitad de mes: mismo corte de días en el mes anterior (MTD vs. MTD)
        int cutoffDay = today.getDayOfMonth();
        BigDecimal revenueLastMonth = sum(won, inMonth(thisMonth.minusMonths(1))
                .and(d -> d.getClosedAt().getDayOfMonth() <= cutoffDay));
        Double growth = revenueLastMonth.signum() == 0 ? null
                : revenueThisMonth.subtract(revenueLastMonth)
                .multiply(BigDecimal.valueOf(100))
                .divide(revenueLastMonth, 1, RoundingMode.HALF_UP).doubleValue();

        double avgCycle = won.stream()
                .filter(d -> d.getCreatedAt() != null)
                .mapToLong(d -> Duration.between(d.getCreatedAt(), d.getClosedAt()).toDays())
                .average().orElse(0);

        Kpis kpis = new Kpis(
                sum(won, d -> true),
                revenueThisMonth,
                revenueLastMonth,
                growth,
                sum(open, d -> true),
                open.stream().map(Deal::weightedAmount).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP),
                rate(won.size(), won.size() + lost.size()),
                won.isEmpty() ? BigDecimal.ZERO
                        : sum(won, d -> true).divide(BigDecimal.valueOf(won.size()), 2, RoundingMode.HALF_UP),
                round1(avgCycle),
                open.size(),
                activeCustomers,
                overdueActivities);

        return new DashboardDto(
                kpis,
                revenueByMonth(won, thisMonth),
                pipelineByStage(open),
                leaderboard(all, reps, thisMonth),
                wonByIndustry(won),
                conversionBySource(all),
                forecast(open, thisMonth));
    }

    private static List<MonthlyRevenue> revenueByMonth(List<Deal> won, YearMonth thisMonth) {
        List<MonthlyRevenue> result = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = thisMonth.minusMonths(i);
            List<Deal> inMonth = won.stream().filter(inMonth(ym)).toList();
            result.add(new MonthlyRevenue(ym.toString(), label(ym), sum(inMonth, d -> true), inMonth.size()));
        }
        return result;
    }

    private static List<StageSummary> pipelineByStage(List<Deal> open) {
        Map<DealStage, List<Deal>> byStage = open.stream().collect(Collectors.groupingBy(Deal::getStage));
        return OPEN_STAGES.stream().map(stage -> {
            List<Deal> list = byStage.getOrDefault(stage, List.of());
            BigDecimal weighted = list.stream().map(Deal::weightedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            return new StageSummary(stage.name(), stage.getLabel(), list.size(), sum(list, d -> true),
                    weighted.setScale(2, RoundingMode.HALF_UP));
        }).toList();
    }

    private static List<RepPerformance> leaderboard(List<Deal> all, List<SalesRep> reps, YearMonth thisMonth) {
        return reps.stream().map(rep -> {
            List<Deal> mine = all.stream()
                    .filter(d -> isOwnedBy(d, rep))
                    .toList();
            List<Deal> won = mine.stream().filter(d -> d.getStage() == DealStage.WON && d.getClosedAt() != null).toList();
            long lost = mine.stream().filter(d -> d.getStage() == DealStage.LOST).count();
            BigDecimal monthRevenue = sum(won, inMonth(thisMonth));
            double attainment = rep.getMonthlyQuota() == null || rep.getMonthlyQuota().signum() == 0 ? 0
                    : monthRevenue.multiply(BigDecimal.valueOf(100))
                    .divide(rep.getMonthlyQuota(), 1, RoundingMode.HALF_UP).doubleValue();
            return new RepPerformance(rep.getId(), rep.getName(), SalesRepDto.initials(rep.getName()),
                    sum(won, d -> true), monthRevenue, rep.getMonthlyQuota(), attainment,
                    won.size(), rate(won.size(), won.size() + lost));
        }).sorted(Comparator.comparing(RepPerformance::revenueThisMonth)
                .thenComparing(RepPerformance::revenueWon).reversed()).toList();
    }

    private static List<Breakdown> wonByIndustry(List<Deal> won) {
        Map<String, List<Deal>> byIndustry = won.stream().collect(Collectors.groupingBy(
                d -> Optional.ofNullable(d.getCustomer().getIndustry()).filter(s -> !s.isBlank()).orElse("Sin industria")));
        return byIndustry.entrySet().stream()
                .map(e -> new Breakdown(e.getKey(), sum(e.getValue(), d -> true), e.getValue().size()))
                .sorted(Comparator.comparing(Breakdown::amount).reversed())
                .toList();
    }

    private static List<SourceConversion> conversionBySource(List<Deal> all) {
        return Arrays.stream(LeadSource.values()).map(source -> {
            List<Deal> closed = all.stream()
                    .filter(d -> d.getCustomer().getSource() == source && d.getStage().isClosed())
                    .toList();
            long won = closed.stream().filter(d -> d.getStage() == DealStage.WON).count();
            return new SourceConversion(source.name(), source.getLabel(), closed.size(), won, rate(won, closed.size()));
        }).filter(s -> s.total() > 0).toList();
    }

    /** Pronóstico de cierre para el mes actual y los dos siguientes. Las fechas vencidas cuentan en el mes actual. */
    private static List<ForecastMonth> forecast(List<Deal> open, YearMonth thisMonth) {
        List<ForecastMonth> result = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            YearMonth ym = thisMonth.plusMonths(i);
            boolean current = i == 0;
            List<Deal> inMonth = open.stream().filter(d -> {
                if (d.getExpectedCloseDate() == null) {
                    return false;
                }
                YearMonth expected = YearMonth.from(d.getExpectedCloseDate());
                return current ? !expected.isAfter(ym) : expected.equals(ym);
            }).toList();
            BigDecimal weighted = inMonth.stream().map(Deal::weightedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            result.add(new ForecastMonth(ym.toString(), label(ym), weighted.setScale(2, RoundingMode.HALF_UP),
                    sum(inMonth, d -> true)));
        }
        return result;
    }

    private static boolean isOwnedBy(Deal deal, SalesRep rep) {
        SalesRep owner = deal.getOwner();
        return owner != null && (owner == rep || (rep.getId() != null && rep.getId().equals(owner.getId())));
    }

    private static Predicate<Deal> inMonth(YearMonth ym) {
        return d -> d.getClosedAt() != null && YearMonth.from(d.getClosedAt()).equals(ym);
    }

    private static BigDecimal sum(List<Deal> list, Predicate<Deal> filter) {
        return list.stream().filter(filter).map(Deal::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private static double rate(long part, long total) {
        return total == 0 ? 0 : round1(part * 100.0 / total);
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static String label(YearMonth ym) {
        String s = ym.atDay(1).format(MONTH_LABEL).replace(".", "");
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
