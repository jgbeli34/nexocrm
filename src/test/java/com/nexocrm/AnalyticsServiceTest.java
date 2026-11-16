package com.nexocrm;

import com.nexocrm.domain.*;
import com.nexocrm.service.AnalyticsService;
import com.nexocrm.web.dto.DashboardDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Prueba unitaria pura de los cálculos del panel (sin Spring ni base de datos). */
class AnalyticsServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);
    private static final LocalDateTime NOW = TODAY.atTime(10, 0);

    private final SalesRep ana = new SalesRep("Ana Pérez", "ana@test.dev", new BigDecimal("10000"));
    private final SalesRep beto = new SalesRep("Beto Ruiz", "beto@test.dev", new BigDecimal("20000"));
    private final Customer retail = customer("Tienda Uno", "Retail", LeadSource.REFERRAL);
    private final Customer salud = customer("Clínica Dos", "Salud", LeadSource.LINKEDIN);

    @Test
    void calculaKpisPrincipales() {
        List<Deal> deals = List.of(
                closed(retail, ana, "5000", DealStage.WON, NOW.minusDays(2), 10),
                closed(salud, ana, "3000", DealStage.WON, NOW.minusDays(5), 30),
                closed(salud, beto, "2000", DealStage.WON, NOW.minusMonths(1), 20),
                closed(retail, beto, "9000", DealStage.LOST, NOW.minusDays(1), 15),
                open(retail, beto, "10000", DealStage.PROPOSAL, TODAY.plusDays(5)),
                open(salud, ana, "4000", DealStage.NEGOTIATION, TODAY.plusMonths(1)));

        DashboardDto d = AnalyticsService.buildDashboard(deals, List.of(ana, beto), 2, 1, TODAY);
        var k = d.kpis();

        assertThat(k.revenueWon()).isEqualByComparingTo("10000");
        assertThat(k.revenueThisMonth()).isEqualByComparingTo("8000");
        assertThat(k.revenueLastMonth()).isEqualByComparingTo("2000");
        assertThat(k.monthOverMonthGrowth()).isEqualTo(300.0);
        assertThat(k.winRate()).isEqualTo(75.0);                      // 3 ganados de 4 cerrados
        assertThat(k.openPipeline()).isEqualByComparingTo("14000");
        assertThat(k.weightedPipeline()).isEqualByComparingTo("8000"); // 10000×50% + 4000×75%
        assertThat(k.averageDealSize()).isEqualByComparingTo("3333.33");
        assertThat(k.averageSalesCycleDays()).isEqualTo(20.0);
        assertThat(k.openDeals()).isEqualTo(2);
    }

    @Test
    void agrupaPorMesEtapaVendedorYFuente() {
        List<Deal> deals = List.of(
                closed(retail, ana, "5000", DealStage.WON, NOW.minusDays(2), 10),
                closed(salud, beto, "2000", DealStage.WON, NOW.minusMonths(1), 20),
                closed(salud, beto, "1000", DealStage.LOST, NOW.minusMonths(1), 20),
                open(retail, beto, "10000", DealStage.PROPOSAL, TODAY.minusDays(3)),
                open(salud, ana, "4000", DealStage.NEGOTIATION, TODAY.plusMonths(1)));

        DashboardDto d = AnalyticsService.buildDashboard(deals, List.of(ana, beto), 2, 0, TODAY);

        assertThat(d.revenueByMonth()).hasSize(12);
        assertThat(d.revenueByMonth().get(11).revenue()).isEqualByComparingTo("5000");
        assertThat(d.revenueByMonth().get(10).revenue()).isEqualByComparingTo("2000");

        assertThat(d.pipelineByStage()).extracting(DashboardDto.StageSummary::stage)
                .containsExactly("LEAD", "QUALIFIED", "PROPOSAL", "NEGOTIATION");

        var first = d.leaderboard().get(0);
        assertThat(first.name()).isEqualTo("Ana Pérez");
        assertThat(first.quotaAttainment()).isEqualTo(50.0);           // 5000 de 10000

        var linkedin = d.conversionBySource().stream().filter(s -> s.source().equals("LINKEDIN")).findFirst().orElseThrow();
        assertThat(linkedin.conversionRate()).isEqualTo(50.0);

        // Fecha vencida cuenta en el mes actual; la de diciembre en el siguiente
        assertThat(d.forecast().get(0).weighted()).isEqualByComparingTo("5000");
        assertThat(d.forecast().get(1).weighted()).isEqualByComparingTo("3000");
    }

    @Test
    void sinDatosNoFalla() {
        DashboardDto d = AnalyticsService.buildDashboard(List.of(), List.of(ana), 0, 0, TODAY);
        assertThat(d.kpis().winRate()).isZero();
        assertThat(d.kpis().monthOverMonthGrowth()).isNull();
        assertThat(d.wonByIndustry()).isEmpty();
    }

    private static Customer customer(String name, String industry, LeadSource source) {
        Customer c = new Customer(name, "Contacto", name.replace(" ", "").toLowerCase() + "@test.dev");
        c.setIndustry(industry);
        c.setSource(source);
        return c;
    }

    private static Deal closed(Customer c, SalesRep rep, String amount, DealStage stage, LocalDateTime closedAt, int cycleDays) {
        Deal d = new Deal("Deal", c, rep, new BigDecimal(amount));
        d.setCreatedAt(closedAt.minusDays(cycleDays));
        d.moveTo(stage, stage == DealStage.LOST ? "Precio" : null, closedAt);
        return d;
    }

    private static Deal open(Customer c, SalesRep rep, String amount, DealStage stage, LocalDate expected) {
        Deal d = new Deal("Deal", c, rep, new BigDecimal(amount));
        d.setCreatedAt(NOW.minusDays(10));
        d.moveTo(stage, null, NOW.minusDays(3));
        d.setExpectedCloseDate(expected);
        return d;
    }
}
